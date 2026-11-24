package com.syncvault.services;

import com.syncvault.db.dao.FolderDao;
import com.syncvault.db.dao.UserDao;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public final class FileMonitorService implements AutoCloseable {

    private static final long DEBOUNCE_MILLIS = 900;
    private static final String LOCAL_USERNAME = "local-user";

    private final WatchService watchService;
    private final FolderDao folderDao;
    private final com.syncvault.services.FileScanner fileScanner;

    // Tracked root folder -> database folder ID
    private final Map<Path, Long> roots = new ConcurrentHashMap<>();

    // WatchService bookkeeping
    private final Map<WatchKey, Path> directoriesByKey = new HashMap<>();
    private final Map<Path, WatchKey> keysByDirectory = new HashMap<>();

    // Debounced scans
    private final Map<Path, ScheduledFuture<?>> pendingScans =
            new ConcurrentHashMap<>();

    // Prevent overlapping scans of the same root
    private final Map<Path, ReentrantLock> rootScanLocks =
            new ConcurrentHashMap<>();

    private final ScheduledExecutorService scanExecutor =
            Executors.newScheduledThreadPool(2, runnable -> {
                Thread thread = new Thread(
                        runnable,
                        "syncvault-file-scan"
                );
                thread.setDaemon(true);
                return thread;
            });

    private final AtomicBoolean running = new AtomicBoolean(false);

    private Thread watcherThread;

    public FileMonitorService() throws IOException {
        this.watchService =
                FileSystems.getDefault().newWatchService();

        this.folderDao = new FolderDao();
        this.fileScanner = new FileScanner();
    }
    
public void registerExistingFolders()
        throws SQLException, IOException {

    UserDao userDao = new UserDao();

    Long userId =
            userDao.findUserIdByUsername(LOCAL_USERNAME);

    if (userId == null) {
        System.out.println(
                "SyncVault: No local user exists yet. "
                + "Folders will be registered when added."
        );
        return;
    }

    for (String folderPath :
            folderDao.findAllFolderPaths(userId)) {

        Long folderId =
                folderDao.findFolderIdByPath(userId, folderPath);

        if (folderId == null) {
            continue;
        }

        Path root = Path.of(folderPath)
                .toAbsolutePath()
                .normalize();

        if (Files.isDirectory(root)) {

            // Restore filesystem monitoring.
            registerFolder(folderId, root);

            // Reconcile changes made while SyncVault was closed.
            scanExecutor.execute(() -> scanRoot(root));

        } else {
            System.err.println(
                    "SyncVault: Tracked folder is unavailable: "
                    + root
            );
        }
    }
}

    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }

        watcherThread = new Thread(
                this::watchLoop,
                "syncvault-file-watcher"
        );

        watcherThread.setDaemon(true);
        watcherThread.start();

        System.out.println(
                "SyncVault file monitoring started."
        );
    }

    /**
     * Starts monitoring a folder that has already been saved
     * in the database.
     */
    public synchronized void registerFolder(
            long folderId,
            Path folderPath
    ) throws IOException {

        if (folderId <= 0) {
            throw new IllegalArgumentException(
                    "Folder ID must be positive."
            );
        }

        if (folderPath == null) {
            throw new IllegalArgumentException(
                    "Folder path is required."
            );
        }

        Path root = folderPath
                .toAbsolutePath()
                .normalize();

        if (!Files.isDirectory(root)) {
            throw new IOException(
                    "Tracked folder does not exist: " + root
            );
        }

        Long existingId = roots.get(root);

        if (existingId != null
                && existingId.longValue() == folderId) {
            return;
        }

        roots.put(root, folderId);

        try {
            registerDirectoryTree(root);
        } catch (IOException exception) {
            roots.remove(root, folderId);
            throw exception;
        }

        System.out.println(
                "SyncVault is monitoring: " + root
        );
    }

    /**
     * Recursively registers all directories under a root.
     */
    private void registerDirectoryTree(Path root)
            throws IOException {

        Files.walkFileTree(
                root,
                new SimpleFileVisitor<>() {

                    @Override
                    public FileVisitResult preVisitDirectory(
                            Path directory,
                            BasicFileAttributes attributes
                    ) throws IOException {

                        registerDirectory(
                                directory.toAbsolutePath().normalize()
                        );

                        return FileVisitResult.CONTINUE;
                    }
                }
        );
    }

    /**
     * Registers an individual directory with WatchService.
     */
    private void registerDirectory(Path directory)
            throws IOException {

        if (keysByDirectory.containsKey(directory)) {
            return;
        }

        WatchKey key = directory.register(
                watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_DELETE
        );

        keysByDirectory.put(directory, key);
        directoriesByKey.put(key, directory);
    }

    /**
     * Stops monitoring a tracked root.
     * Does not delete the actual folder or its files.
     */
    public synchronized void unregisterFolder(Path folderPath) {

        if (folderPath == null) {
            return;
        }

        Path root = folderPath
                .toAbsolutePath()
                .normalize();

        roots.remove(root);

        ScheduledFuture<?> pending =
                pendingScans.remove(root);

        if (pending != null) {
            pending.cancel(false);
        }

        /*
         * A directory might belong to more than one tracked root.
         * Keep watching directories still needed by another root.
         */
        Set<Path> stillNeeded = new HashSet<>();

        for (Path remainingRoot : roots.keySet()) {
            if (!Files.isDirectory(remainingRoot)) {
                continue;
            }

            try {
                Files.walkFileTree(
                        remainingRoot,
                        new SimpleFileVisitor<>() {

                            @Override
                            public FileVisitResult preVisitDirectory(
                                    Path directory,
                                    BasicFileAttributes attributes
                            ) {
                                stillNeeded.add(
                                        directory.toAbsolutePath()
                                                .normalize()
                                );

                                return FileVisitResult.CONTINUE;
                            }
                        }
                );
            } catch (IOException exception) {
                System.err.println(
                        "Could not inspect remaining tracked folder "
                        + remainingRoot + ": "
                        + exception.getMessage()
                );
            }
        }

        List<Path> directoriesToRemove =
                new ArrayList<>();

        for (Path directory : keysByDirectory.keySet()) {
            if (directory.startsWith(root)
                    && !stillNeeded.contains(directory)) {
                directoriesToRemove.add(directory);
            }
        }

        for (Path directory : directoriesToRemove) {
            WatchKey key =
                    keysByDirectory.remove(directory);

            if (key != null) {
                directoriesByKey.remove(key);
                key.cancel();
            }
        }

        System.out.println(
                "SyncVault stopped monitoring: " + root
        );
    }

    /**
     * Main WatchService event loop.
     */
    private void watchLoop() {

        while (running.get()) {
            WatchKey key;

            try {
                key = watchService.take();

            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                break;

            } catch (ClosedWatchServiceException exception) {
                break;
            }

            Path directory;

            synchronized (this) {
                directory = directoriesByKey.get(key);
            }

            if (directory == null) {
                key.reset();
                continue;
            }

            boolean relevantChange = false;

            for (WatchEvent<?> event : key.pollEvents()) {

                if (event.kind()
                        == StandardWatchEventKinds.OVERFLOW) {

                    // Events were lost; request a reconciliation scan.
                    relevantChange = true;
                    continue;
                }

                Object context = event.context();

                if (!(context instanceof Path relativePath)) {
                    continue;
                }

                Path changedPath = directory
                        .resolve(relativePath)
                        .toAbsolutePath()
                        .normalize();

                relevantChange = true;

                if (event.kind()
                        == StandardWatchEventKinds.ENTRY_CREATE) {

                    System.out.println(
                            "SyncVault detected creation: "
                            + changedPath
                    );

                    /*
                     * WatchService is not recursive by itself.
                     * Register newly created directories and their
                     * existing descendants.
                     */
                    if (Files.isDirectory(changedPath)) {
                        try {
                            synchronized (this) {
                                registerDirectoryTree(changedPath);
                            }
                        } catch (IOException exception) {
                            System.err.println(
                                    "Could not monitor new directory "
                                    + changedPath + ": "
                                    + exception.getMessage()
                            );
                        }
                    }

                } else if (event.kind()
                        == StandardWatchEventKinds.ENTRY_MODIFY) {

                    System.out.println(
                            "SyncVault detected modification: "
                            + changedPath
                    );

                } else if (event.kind()
                        == StandardWatchEventKinds.ENTRY_DELETE) {

                    System.out.println(
                            "SyncVault detected deletion: "
                            + changedPath
                    );
                }
            }

            if (relevantChange) {
                scheduleRootScan(directory);
            }

            boolean valid = key.reset();

            if (!valid) {
                synchronized (this) {
                    Path removed =
                            directoriesByKey.remove(key);

                    if (removed != null) {
                        keysByDirectory.remove(removed);
                    }
                }
            }
        }
    }

    /**
     * Schedules a scan after a quiet period.
     * Repeated events reset the timer.
     */
    private void scheduleRootScan(Path changedDirectory) {

        Path root = findOwningRoot(changedDirectory);

        if (root == null) {
            return;
        }

        ScheduledFuture<?> next = scanExecutor.schedule(
                () -> scanRoot(root),
                DEBOUNCE_MILLIS,
                TimeUnit.MILLISECONDS
        );

        ScheduledFuture<?> previous =
                pendingScans.put(root, next);

        if (previous != null) {
            previous.cancel(false);
        }
    }

    /**
     * Finds the most specific tracked root containing a directory.
     */
    private Path findOwningRoot(Path path) {

        return roots.keySet()
                .stream()
                .filter(path::startsWith)
                .max((first, second) ->
                        Integer.compare(
                                first.getNameCount(),
                                second.getNameCount()
                        ))
                .orElse(null);
    }

    /**
     * Invokes the existing scanner.
     * Version creation remains the responsibility of FileScanner
     * and VersionStorageService.
     */
    private void scanRoot(Path root) {

        Long folderId = roots.get(root);

        if (folderId == null
                || !Files.isDirectory(root)) {
            return;
        }

        ReentrantLock lock =
                rootScanLocks.computeIfAbsent(
                        root,
                        ignored -> new ReentrantLock()
                );

        if (!lock.tryLock()) {
            // A previous scan is running; schedule a fresh scan.
            scheduleRootScan(root);
            return;
        }

        try {
            FileScanner.ScanResult result =
                    fileScanner.scanFolder(folderId, root);

            System.out.printf(
        "SyncVault scan completed for %s: "
        + "discovered=%d, new=%d, updated=%d, "
        + "unchanged=%d, versions=%d%n",
        root,
        result.discovered(),
        result.registered(),
        result.updated(),
        result.unchanged(),
        result.versionsCreated()
);
        } catch (IOException
                 | SQLException
                 | IllegalArgumentException exception) {

            System.err.println(
                    "Automatic scan failed for "
                    + root + ": "
                    + exception.getMessage()
            );

        } finally {
            lock.unlock();
        }
    }

    /**
     * Stops monitoring and releases resources.
     */
    @Override
    public void close() {

        running.set(false);

        try {
            watchService.close();
        } catch (IOException ignored) {
        }

        if (watcherThread != null) {
            watcherThread.interrupt();
        }

        for (ScheduledFuture<?> future :
                new ArrayList<>(pendingScans.values())) {
            future.cancel(false);
        }

        pendingScans.clear();
        scanExecutor.shutdownNow();

        System.out.println(
                "SyncVault file monitoring stopped."
        );
    }
}