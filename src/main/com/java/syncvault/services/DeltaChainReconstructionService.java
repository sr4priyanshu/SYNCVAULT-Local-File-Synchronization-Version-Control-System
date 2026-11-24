// Walks and validates a version chain back to a full snapshot, applies deltas sequentially into temporary files with SHA-256 verification, and returns the reconstructed temporary file.
package com.syncvault.services;
import com.syncvault.db.dao.VersionDao;
import com.syncvault.db.models.VersionRecord;
import com.syncvault.db.models.VersionStorageType;
import com.syncvault.services.delta.DeltaReconstructionService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.HexFormat;

public class DeltaChainReconstructionService {

    private final VersionDao versionDao = new VersionDao();
    public Path reconstructToTemporaryFile(long versionId)
            throws IOException, SQLException {

        if (versionId <= 0) {
            throw new IllegalArgumentException(
                    "Version ID must be positive."
            );
        }

        VersionRecord target = versionDao.findById(versionId);

        if (target == null) {
            throw new IllegalArgumentException(
                    "Requested version was not found."
            );
        }
        List<VersionRecord> chain = new ArrayList<>();

        Set<Long> visited = new HashSet<>();

        VersionRecord current = target;
        VersionRecord fullSnapshot = null;

        while (current != null) {

            if (!visited.add(current.getVersionId())) {
                throw new IOException(
                        "Circular version dependency detected."
                );
            }

            if (current.getFileId() != target.getFileId()) {
                throw new IOException(
                        "Version chain contains a different file ID."
                );
            }

            if (current.isFullSnapshot()) {
                fullSnapshot = current;
                break;
            }

            if (!current.isDelta()) {
                throw new IOException(
                        "Unsupported storage type: "
                                + current.getStorageType()
                );
            }

            chain.add(current);

            Long parentId = current.getParentVersionId();

            if (parentId == null) {
                throw new IOException(
                        "DELTA version has no parent version."
                );
            }

            VersionRecord parent = versionDao.findById(parentId);

            if (parent == null) {
                throw new IOException(
                        "Parent version " + parentId + " is missing."
                );
            }

            if (parent.getFileId() != target.getFileId()) {
                throw new IOException(
                        "Parent version belongs to another file."
                );
            }

            if (parent.getVersionNumber()
                    >= current.getVersionNumber()) {

                throw new IOException(
                        "Invalid version chain ordering."
                );
            }

            current = parent;
        }

        if (fullSnapshot == null) {
            throw new IOException(
                    "No FULL snapshot was found in the version chain."
            );
        }

        Path baseSnapshot = validateStoredPath(fullSnapshot);

        validateVersionFile(baseSnapshot, fullSnapshot);
        Collections.reverse(chain);

        Path currentFile = baseSnapshot;
        boolean currentFileIsTemporary = false;

        try {

            for (VersionRecord deltaVersion : chain) {

                Path deltaPath = validateStoredPath(deltaVersion);

                Path reconstructed = Files.createTempFile(
                        "syncvault-reconstruction-",
                        ".tmp"
                );

                boolean reconstructionComplete = false;

                try {

                    DeltaReconstructionService.reconstruct(
                            currentFile,
                            deltaPath,
                            reconstructed
                    );

                    validateVersionFile(
                            reconstructed,
                            deltaVersion
                    );

                    reconstructionComplete = true;

                } finally {

                    if (!reconstructionComplete) {
                        Files.deleteIfExists(reconstructed);
                    }
                }
                if (currentFileIsTemporary) {
                    Files.deleteIfExists(currentFile);
                }

                currentFile = reconstructed;
                currentFileIsTemporary = true;
            }
            validateVersionFile(currentFile, target);

            if (!currentFileIsTemporary) {
                Path copy = Files.createTempFile(
                        "syncvault-reconstruction-",
                        ".tmp"
                );

                try {
                    Files.copy(
                            currentFile,
                            copy,
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING
                    );

                    validateVersionFile(copy, target);

                } catch (IOException | RuntimeException e) {
                    Files.deleteIfExists(copy);
                    throw e;
                }

                return copy;
            }

            Path result = currentFile;
            currentFileIsTemporary = false;

            return result;

        } finally {

            if (currentFileIsTemporary) {
                Files.deleteIfExists(currentFile);
            }
        }
    }
    private Path validateStoredPath(
            VersionRecord version
    ) throws IOException {

        String storedPath = version.getStoragePath();

        if (storedPath == null || storedPath.isBlank()) {
            throw new IOException(
                    "Stored version path is missing."
            );
        }

        Path path = Path.of(storedPath)
                .toAbsolutePath()
                .normalize();

        if (!Files.isRegularFile(path)
                || !Files.isReadable(path)) {

            throw new IOException(
                    "Stored version is missing or unreadable: " + path
            );
        }

        return path;
    }
    private void validateVersionFile(
            Path path,
            VersionRecord version
    ) throws IOException {

        if (Files.size(path) != version.getFileSize()) {
            throw new IOException(
                    "Version " + version.getVersionNumber()
                            + " failed validation: file size mismatch."
            );
        }

        String actualHash = calculateSha256(path);

        if (!actualHash.equalsIgnoreCase(
                version.getContentHash()
        )) {

            throw new IOException(
                    "Version " + version.getVersionNumber()
                            + " failed SHA-256 validation."
            );
        }
    }

    private String calculateSha256(Path path)
            throws IOException {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            try (InputStream input = Files.newInputStream(path)) {

                byte[] buffer = new byte[8192];

                int bytesRead;

                while ((bytesRead = input.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }

            return HexFormat.of().formatHex(digest.digest());

        } catch (NoSuchAlgorithmException e) {

            throw new IllegalStateException(
                    "SHA-256 algorithm is unavailable.",
                    e
            );
        }
    }
}
