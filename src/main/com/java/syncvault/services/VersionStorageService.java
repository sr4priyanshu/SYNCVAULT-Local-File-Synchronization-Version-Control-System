// Creates a new deduplicated file version as a delta (when smaller and off the snapshot interval) or full snapshot, persists metadata, and cleans up temporary files.

package com.syncvault.services;

import com.syncvault.db.dao.VersionDao;
import com.syncvault.db.models.VersionRecord;
import com.syncvault.services.delta.DeltaGenerator;
import com.syncvault.services.delta.DeltaPackage;
import com.syncvault.services.delta.DeltaStorageService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.HexFormat;
import java.util.UUID;

public class VersionStorageService {

    private static final int FULL_SNAPSHOT_INTERVAL = 5;

    private final VersionDao versionDao = new VersionDao();

    private final DeltaChainReconstructionService chainReconstructionService =
            new DeltaChainReconstructionService();

    private final Path storageRoot = Path.of(
            System.getProperty("user.home"),
            ".syncvault",
            "versions"
    );

    public boolean createVersion(
            long fileId,
            Path sourcePath
    ) throws IOException, SQLException {

        if (fileId <= 0) {
            throw new IllegalArgumentException(
                    "File ID must be positive."
            );
        }

        if (sourcePath == null
                || !Files.isRegularFile(sourcePath)
                || !Files.isReadable(sourcePath)) {
            throw new IOException(
                    "The source file does not exist or is not readable."
            );
        }

        Files.createDirectories(storageRoot);

        Path fileStorageDirectory = storageRoot.resolve(
                Long.toString(fileId)
        );

        Files.createDirectories(fileStorageDirectory);

        String originalName = sourcePath.getFileName().toString();

        Path temporaryPath = fileStorageDirectory.resolve(
                UUID.randomUUID() + ".tmp"
        );

        Path snapshotPath = fileStorageDirectory.resolve(
                UUID.randomUUID() + "_" + originalName
        );

        Path deltaTemporaryPath = fileStorageDirectory.resolve(
                UUID.randomUUID() + ".delta.tmp"
        );

        Path deltaPath = fileStorageDirectory.resolve(
                UUID.randomUUID() + "_" + originalName + ".delta"
        );

        Path reconstructedBasePath = null;
        Path storedVersionPath = null;

        boolean versionStored = false;

        try {
            Files.copy(sourcePath, temporaryPath);

            String snapshotHash = calculateSha256(temporaryPath);
            long snapshotSize = Files.size(temporaryPath);

            VersionRecord latestVersion =
                    versionDao.findLatestVersion(fileId);

            // Avoid creating duplicate versions.
            if (latestVersion != null
                    && snapshotHash.equals(
                            latestVersion.getContentHash()
                    )) {
                return false;
            }

            long nextVersionNumber = latestVersion == null
                    ? 1
                    : latestVersion.getVersionNumber() + 1;

            boolean forceFullSnapshot =
                    latestVersion == null
                    || nextVersionNumber % FULL_SNAPSHOT_INTERVAL == 0;

            String storageType = "FULL";
            Path selectedStoragePath = snapshotPath;

            // Generate DELTA only when a periodic FULL snapshot
            // is not due.
            if (!forceFullSnapshot) {

                reconstructedBasePath =
                        chainReconstructionService
                                .reconstructToTemporaryFile(
                                        latestVersion.getVersionId()
                                );

                DeltaPackage deltaPackage = DeltaGenerator.generate(
                        reconstructedBasePath,
                        temporaryPath
                );

                DeltaStorageService.save(
                        deltaPackage,
                        deltaTemporaryPath
                );

                long deltaSize = Files.size(deltaTemporaryPath);

                if (deltaSize < snapshotSize) {
                    storageType = "DELTA";
                    selectedStoragePath = deltaPath;

                    moveAtomically(
                            deltaTemporaryPath,
                            deltaPath
                    );
                }
            }

            // Store a FULL snapshot when:
            // 1. It is the first version.
            // 2. The periodic snapshot interval is reached.
            // 3. The DELTA package is not smaller.
            if ("FULL".equals(storageType)) {
                moveAtomically(
                        temporaryPath,
                        snapshotPath
                );
            }

            storedVersionPath = selectedStoragePath;

            Long parentVersionId = latestVersion == null
                    ? null
                    : latestVersion.getVersionId();

            versionDao.addNextVersion(
                    fileId,
                    snapshotHash,
                    snapshotSize,
                    storageType,
                    storedVersionPath.toAbsolutePath().toString(),
                    parentVersionId
            );

            versionStored = true;
            return true;

        } catch (IOException | SQLException | RuntimeException e) {

            if (!versionStored && storedVersionPath != null) {
                try {
                    Files.deleteIfExists(storedVersionPath);
                } catch (IOException cleanupException) {
                    e.addSuppressed(cleanupException);
                }
            }

            throw e;

        } finally {
            Files.deleteIfExists(temporaryPath);
            Files.deleteIfExists(deltaTemporaryPath);

            if (reconstructedBasePath != null) {
                Files.deleteIfExists(reconstructedBasePath);
            }
        }
    }

    private void moveAtomically(
            Path source,
            Path destination
    ) throws IOException {

        try {
            Files.move(
                    source,
                    destination,
                    StandardCopyOption.ATOMIC_MOVE
            );
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, destination);
        }
    }

    private String calculateSha256(Path path)
            throws IOException {

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            try (InputStream inputStream =
                         Files.newInputStream(path)) {

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
}
