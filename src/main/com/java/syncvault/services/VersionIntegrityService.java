
package com.syncvault.services;

import com.syncvault.db.dao.VersionDao;
import com.syncvault.db.models.VersionRecord;
import com.syncvault.services.delta.DeltaPackage;
import com.syncvault.services.delta.DeltaStorageService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.HexFormat;

public class VersionIntegrityService {

    private final VersionDao versionDao = new VersionDao();

    private final DeltaChainReconstructionService reconstructionService =
            new DeltaChainReconstructionService();

    public record IntegrityResult(
            boolean valid,
            long versionId,
            String message
    ) {}

    public IntegrityResult verifyVersion(long versionId)
            throws SQLException {

        if (versionId <= 0) {
            throw new IllegalArgumentException(
                    "Version ID must be positive."
            );
        }

        VersionRecord version = versionDao.findById(versionId);

        if (version == null) {
            return invalid(versionId, "Version does not exist in the database.");
        }

        Path storagePath = Path.of(version.getStoragePath());

        if (!Files.isRegularFile(storagePath)
                || !Files.isReadable(storagePath)) {

            return invalid(
                    versionId,
                    "Stored version file is missing or unreadable."
            );
        }

        try {

            if (version.isFullSnapshot()) {
                return verifyFullSnapshot(version, storagePath);
            }

            if (version.isDelta()) {
                return verifyDelta(version, storagePath);
            }

            return invalid(
                    versionId,
                    "Unsupported version storage type."
            );

        } catch (IOException e) {

            return invalid(
                    versionId,
                    "Integrity verification failed: " + e.getMessage()
            );
        }
    }

    private IntegrityResult verifyFullSnapshot(
            VersionRecord version,
            Path snapshotPath
    ) throws IOException {

        long actualSize = Files.size(snapshotPath);

        if (actualSize != version.getFileSize()) {
            return invalid(
                    version.getVersionId(),
                    "FULL snapshot size does not match database metadata."
            );
        }

        String actualHash = calculateSha256(snapshotPath);

        if (!actualHash.equals(version.getContentHash())) {
            return invalid(
                    version.getVersionId(),
                    "FULL snapshot SHA-256 hash mismatch."
            );
        }

        return valid(
                version.getVersionId(),
                "FULL snapshot integrity verified successfully."
        );
    }

    private IntegrityResult verifyDelta(
            VersionRecord version,
            Path deltaPath
    ) throws IOException, SQLException {
        DeltaPackage deltaPackage = DeltaStorageService.load(deltaPath);

        if (deltaPackage.getTargetFileSize()
                != version.getFileSize()) {

            return invalid(
                    version.getVersionId(),
                    "DELTA target size does not match database metadata."
            );
        }
        Path reconstructedPath = null;
        try {
            reconstructedPath =
                    reconstructionService.reconstructToTemporaryFile(
                            version.getVersionId()
                    );
            long actualSize = Files.size(reconstructedPath);

            if (actualSize != version.getFileSize()) {
                return invalid(
                        version.getVersionId(),
                        "Reconstructed DELTA size does not match database metadata."
                );
            }

            String actualHash = calculateSha256(reconstructedPath);

            if (!actualHash.equals(version.getContentHash())) {
                return invalid(
                        version.getVersionId(),
                        "Reconstructed DELTA SHA-256 hash mismatch."
                );
            }

            return valid(
                    version.getVersionId(),
                    "DELTA package and reconstructed content verified successfully."
            );

        } finally {

            if (reconstructedPath != null) {
                Files.deleteIfExists(reconstructedPath);
            }
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

    private IntegrityResult valid(
            long versionId,
            String message
    ) {

        return new IntegrityResult(
                true,
                versionId,
                message
        );
    }

    private IntegrityResult invalid(
            long versionId,
            String message
    ) {

        return new IntegrityResult(
                false,
                versionId,
                message
        );
    }
}
