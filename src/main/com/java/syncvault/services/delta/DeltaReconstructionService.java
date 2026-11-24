package com.syncvault.services.delta;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Map;

public final class DeltaReconstructionService {

    private DeltaReconstructionService() {
    }
    public static Path reconstruct(
            Path baseSnapshot,
            Path deltaFile,
            Path destination
    ) throws IOException {

        if (baseSnapshot == null
                || deltaFile == null
                || destination == null) {

            throw new IllegalArgumentException(
                    "All file paths are required."
            );
        }

        if (!Files.isRegularFile(baseSnapshot)) {
            throw new IOException(
                    "Base snapshot does not exist: " + baseSnapshot
            );
        }

        if (!Files.isRegularFile(deltaFile)) {
            throw new IOException(
                    "Delta file does not exist: " + deltaFile
            );
        }

        Path base = baseSnapshot.toAbsolutePath().normalize();
        Path output = destination.toAbsolutePath().normalize();

        if (base.equals(output)) {
            throw new IOException(
                    "Destination cannot overwrite the base snapshot."
            );
        }

        DeltaPackage delta =
                DeltaStorageService.load(deltaFile);

        int blockSize = delta.getBlockSize();
        long targetSize = delta.getTargetFileSize();
        long baseSize = Files.size(base);

        Map<Long, byte[]> changedBlocks =
                delta.getChangedBlocks();

        // Validate that unchanged target blocks exist in the base.
        long totalBlocks = targetSize == 0
                ? 0
                : 1 + (targetSize - 1) / blockSize;

        for (long index = 0; index < totalBlocks; index++) {

            long offset = Math.multiplyExact(
                    index,
                    (long) blockSize
            );

            int expectedLength = (int) Math.min(
                    blockSize,
                    targetSize - offset
            );

            byte[] replacement = changedBlocks.get(index);

            if (replacement != null) {

                if (replacement.length != expectedLength) {
                    throw new IOException(
                            "Invalid replacement block: " + index
                    );
                }

            } else if (offset > baseSize
                    || expectedLength > baseSize - offset) {

                throw new IOException(
                        "Missing block " + index
                                + " in base snapshot and delta."
                );
            }
        }

        Path absoluteOutput = output;
        Path parent = absoluteOutput.getParent();

        if (parent == null) {
            throw new IOException(
                    "Invalid destination path."
            );
        }

        Files.createDirectories(parent);

        Path tempFile = Files.createTempFile(
                parent,
                ".syncvault-restore-",
                ".tmp"
        );

        boolean completed = false;

        try {

            // Step 1: Copy the base snapshot.
            Files.copy(
                    base,
                    tempFile,
                    StandardCopyOption.REPLACE_EXISTING
            );

            // Step 2: Replace changed blocks.
            try (
                FileChannel channel = FileChannel.open(
                        tempFile,
                        StandardOpenOption.WRITE
                )
            ) {

                for (Map.Entry<Long, byte[]> entry
                        : changedBlocks.entrySet()) {

                    long offset = Math.multiplyExact(
                            entry.getKey(),
                            (long) blockSize
                    );

                    byte[] block = entry.getValue();

                    ByteBuffer buffer = ByteBuffer.wrap(block);

                    while (buffer.hasRemaining()) {

                        int written = channel.write(
                                buffer,
                                offset + buffer.position()
                        );

                        if (written <= 0) {
                            throw new IOException(
                                    "Unable to write reconstructed block."
                            );
                        }
                    }
                }

                // Step 3: Set the exact target file size.
                channel.truncate(targetSize);

                channel.force(true);
            }

            // Step 4: Move the completed file into place.
            try {

                Files.move(
                        tempFile,
                        absoluteOutput,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING
                );

            } catch (java.nio.file.AtomicMoveNotSupportedException e) {

                Files.move(
                        tempFile,
                        absoluteOutput,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

            completed = true;

            return absoluteOutput;

        } finally {

            if (!completed) {
                Files.deleteIfExists(tempFile);
            }
        }
    }
}
