// Reconstructs and validates a target file version, backs up the current file if present, atomically replaces it, updates database state and activity logs, and cleans up temporary files.
package com.syncvault.services;

import com.syncvault.db.dao.FileDao;
import com.syncvault.db.dao.VersionDao;
import com.syncvault.db.dao.ActivityLogDao;
import com.syncvault.db.models.FileRecord;
import com.syncvault.db.models.VersionRecord;

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

public class VersionRestoreService {

    private final ActivityLogDao activityLogDao = new ActivityLogDao();
    private final FileDao fileDao = new FileDao();
    private final VersionDao versionDao = new VersionDao();

    private final VersionStorageService versionStorageService =
            new VersionStorageService();

    private final DeltaChainReconstructionService
            deltaChainReconstructionService =
            new DeltaChainReconstructionService();

    public RestoreResult restoreVersion(long versionId)
            throws IOException, SQLException {

        if (versionId <= 0) {
            throw new IllegalArgumentException(
                    "Version ID must be positive."
            );
        }

        VersionRecord version = versionDao.findById(versionId);

        if (version == null) {
            throw new IllegalArgumentException(
                    "The selected version was not found."
            );
        }

        FileRecord file = fileDao.findById(version.getFileId());

        if (file == null) {
            throw new IllegalStateException(
                    "The associated file record was not found."
            );
        }

        Path destination = Path.of(file.getFilePath())
                .toAbsolutePath()
                .normalize();

        Path parent = destination.getParent();

        if (parent == null) {
            throw new IOException(
                    "The destination does not have a parent directory."
            );
        }

        Files.createDirectories(parent);

        Path temporary = parent.resolve(
                "." + destination.getFileName()
                        + "." + UUID.randomUUID() + ".restore"
        );

        Path reconstructed = null;

        try {
            // Reconstruct the selected version.
            // FULL versions are copied directly.
            // DELTA versions are reconstructed through their parent chain.
            reconstructed =
                    deltaChainReconstructionService
                            .reconstructToTemporaryFile(versionId);

            if (!Files.isRegularFile(reconstructed)
                    || !Files.isReadable(reconstructed)) {
                throw new IOException(
                        "The reconstructed version is missing or unreadable."
                );
            }

            // Validate reconstructed content against selected version metadata.
            validateSnapshot(reconstructed, version);

            if (reconstructed.toAbsolutePath().normalize()
                    .equals(destination)) {
                throw new IOException(
                        "The reconstructed version cannot be its own "
                                + "restore destination."
                );
            }

            // Prepare the final restore file.
            Files.copy(reconstructed, temporary);
            validateSnapshot(temporary, version);

            // Preserve the current file before replacing it.
            if (Files.exists(destination)) {

                if (!Files.isRegularFile(destination)) {
                    throw new IOException(
                            "The destination exists but is not a regular file."
                    );
                }

                boolean saved = versionStorageService.createVersion(
                        file.getFileId(),
                        destination
                );

                if (!saved) {
                    VersionRecord latest =
                            versionDao.findLatestVersion(file.getFileId());

                    if (latest == null
                            || !latest.getContentHash().equalsIgnoreCase(
                                    calculateSha256(destination))) {
                        throw new IOException(
                                "The current file could not be preserved "
                                        + "before restoration."
                        );
                    }
                }
            }

            // Replace destination atomically whenever supported.
            try {
                Files.move(
                        temporary,
                        destination,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING
                );

            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(
                        temporary,
                        destination,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

            // Update current file metadata.
            boolean updated = fileDao.updateCurrentState(
                    file.getFileId(),
                    version.getContentHash(),
                    version.getFileSize()
            );

            if (!updated) {
                throw new SQLException(
                        "The file was restored, but its database record "
                                + "could not be updated."
                );
            }

            // Record restoration activity.
            activityLogDao.addLog(
                    file.getFileId(),
                    "FILE_RESTORED",
                    "Version " + version.getVersionNumber()
                            + " restored to: " + destination
            );

            return new RestoreResult(
                    file.getFileId(),
                    version.getVersionId(),
                    version.getVersionNumber(),
                    destination,
                    version.getContentHash(),
                    version.getFileSize()
            );

        } finally {
            Files.deleteIfExists(temporary);

            // The reconstruction service returns a temporary file.
            // Its cleanup responsibility belongs to this caller.
            if (reconstructed != null) {
                Files.deleteIfExists(reconstructed);
            }
        }
    }

    private void validateSnapshot(
            Path path,
            VersionRecord version
    ) throws IOException {

        if (Files.size(path) != version.getFileSize()) {
            throw new IOException(
                    "Snapshot validation failed: file size does not match."
            );
        }

        String actualHash = calculateSha256(path);

        if (!actualHash.equalsIgnoreCase(version.getContentHash())) {
            throw new IOException(
                    "Snapshot validation failed: SHA-256 does not match."
            );
        }
    }

    private String calculateSha256(Path path) throws IOException {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            try (InputStream input = Files.newInputStream(path)) {
                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead = input.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }

            return HexFormat.of().formatHex(digest.digest());

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable.",
                    exception
            );
        }
    }

    public record RestoreResult(
            long fileId,
            long versionId,
            int versionNumber,
            Path restoredPath,
            String contentHash,
            long fileSize
    ) {
    }
}
