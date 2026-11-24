//Scans directories to discover files and tracks their registration and update status.
package com.syncvault.services;

import com.syncvault.db.dao.ActivityLogDao;
import com.syncvault.db.dao.FileDao;
import com.syncvault.db.dao.VersionDao;
import com.syncvault.db.models.FileRecord;
import com.syncvault.db.models.VersionRecord;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class FileScanner {

    private final FileDao fileDao;
    private final VersionDao versionDao;
    private final VersionStorageService versionStorageService;
    private final ActivityLogDao activityLogDao;

    public FileScanner() {
        this.fileDao = new FileDao();
        this.versionDao = new VersionDao();
        this.versionStorageService = new VersionStorageService();
        this.activityLogDao = new ActivityLogDao();
    }

    public ScanResult scanFolder(
            long folderId,
            Path folderPath
    ) throws IOException, SQLException {

        if (folderId <= 0) {
            throw new IllegalArgumentException(
                    "Folder ID must be positive."
            );
        }

        if (folderPath == null || !Files.isDirectory(folderPath)) {
            throw new IOException(
                    "The specified folder does not exist or is inaccessible."
            );
        }

        Path normalizedFolder = folderPath
                .toAbsolutePath()
                .normalize();

        List<Path> scannedFiles;

        try (Stream<Path> paths = Files.walk(normalizedFolder)) {
            scannedFiles = paths
                    .filter(Files::isRegularFile)
                    .toList();
        }

        Set<String> discoveredPaths = new HashSet<>();

        int discovered = 0;
        int registered = 0;
        int updated = 0;
        int unchanged = 0;
        int versionsCreated = 0;
        int missing = 0;

        for (Path path : scannedFiles) {

            Path normalizedPath = path
                    .toAbsolutePath()
                    .normalize();

            String filePath = normalizedPath.toString();
            String fileName = normalizedPath.getFileName().toString();

            discoveredPaths.add(filePath);
            discovered++;

            String extension = getExtension(fileName);
            long fileSize = Files.size(normalizedPath);
            String currentHash = calculateHash(normalizedPath);

            FileRecord existing = fileDao.findByPath(filePath);

            // CASE 1: New file registration.
            if (existing == null) {

                long fileId = fileDao.addFile(
                        folderId,
                        fileName,
                        filePath,
                        extension,
                        currentHash,
                        fileSize
                );

                boolean versionCreated =
                        versionStorageService.createVersion(
                                fileId,
                                normalizedPath
                        );

                if (versionCreated) {
                    versionsCreated++;

                    logActivity(
                            fileId,
                            "VERSION_CREATED",
                            "Initial version created for: " + fileName
                    );
                }

                registered++;

                logActivity(
                        fileId,
                        "FILE_ADDED",
                        "New file registered: " + filePath
                );

                continue;
            }

            long fileId = existing.getFileId();

            // CASE 2: Previously missing file has reappeared.
            if (existing.isMissing()) {

                boolean markedPresent = fileDao.markPresent(fileId);

                if (markedPresent) {
                    logActivity(
                            fileId,
                            "FILE_REAPPEARED",
                            "Previously missing file detected again: "
                                    + filePath
                    );
                }
            }

            // CASE 3: File exists in database but has no version.
            // Retry initial snapshot creation.
            VersionRecord latestVersion =
                    versionDao.findLatestVersion(fileId);

            if (latestVersion == null) {

                boolean versionCreated =
                        versionStorageService.createVersion(
                                fileId,
                                normalizedPath
                        );

                if (versionCreated) {
                    versionsCreated++;

                    logActivity(
                            fileId,
                            "VERSION_CREATED",
                            "Initial version created for: " + fileName
                    );
                }

                fileDao.updateCurrentState(
                        fileId,
                        currentHash,
                        fileSize
                );

                registered++;

                continue;
            }

            // CASE 4: Existing file content has changed.
            boolean contentChanged =
                    !currentHash.equals(existing.getCurrentHash());

            if (contentChanged) {

                boolean versionCreated =
                        versionStorageService.createVersion(
                                fileId,
                                normalizedPath
                        );

                fileDao.updateCurrentState(
                        fileId,
                        currentHash,
                        fileSize
                );

                updated++;

                logActivity(
                        fileId,
                        "FILE_MODIFIED",
                        "File content changed: " + filePath
                );

                if (versionCreated) {
                    versionsCreated++;

                    logActivity(
                            fileId,
                            "VERSION_CREATED",
                            "New version created for: " + fileName
                    );
                }

            } else {
                unchanged++;
            }
        }

        // CASE 5: Detect files that are no longer present.
        List<FileRecord> trackedFiles =
                fileDao.findByFolderId(folderId);

        for (FileRecord file : trackedFiles) {

            String trackedPath = Path.of(file.getFilePath())
                    .toAbsolutePath()
                    .normalize()
                    .toString();

            if (!discoveredPaths.contains(trackedPath)
                    && !file.isMissing()) {

                boolean markedMissing =
                        fileDao.markMissing(file.getFileId());

                if (markedMissing) {
                    missing++;

                    logActivity(
                            file.getFileId(),
                            "FILE_MISSING",
                            "Tracked file is no longer present: "
                                    + trackedPath
                    );
                }
            }
        }

        // CASE 6: Record scan summary.
        logActivity(
                null,
                "SCAN_COMPLETED",
                "Folder: " + normalizedFolder
                        + "; discovered=" + discovered
                        + "; registered=" + registered
                        + "; updated=" + updated
                        + "; unchanged=" + unchanged
                        + "; missing=" + missing
                        + "; versionsCreated=" + versionsCreated
        );

        return new ScanResult(
                discovered,
                registered,
                updated,
                unchanged,
                versionsCreated
        );
    }

    private String getExtension(String fileName) {

        int dotIndex = fileName.lastIndexOf('.');

        if (dotIndex <= 0 || dotIndex == fileName.length() - 1) {
            return "";
        }

        return fileName.substring(dotIndex + 1);
    }

    private String calculateHash(Path path) throws IOException {

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            try (var inputStream = Files.newInputStream(path)) {

                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }

            return HexFormat.of().formatHex(digest.digest());

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "SHA-256 is not available.",
                    e
            );
        }
    }

    private void logActivity(
            Long fileId,
            String action,
            String details
    ) {

        try {
            activityLogDao.addLog(
                    fileId,
                    action,
                    details
            );

        } catch (SQLException e) {
            System.err.println(
                    "Activity logging failed for "
                            + action
                            + ": "
                            + e.getMessage()
            );
        }
    }

    public record ScanResult(
            int discovered,
            int registered,
            int updated,
            int unchanged,
            int versionsCreated
    ) {
    }
}
