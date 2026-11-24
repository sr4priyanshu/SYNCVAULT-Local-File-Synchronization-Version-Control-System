// Computes a block-level delta package between an old and new file by hashing blocks, verifying matching hashes with byte comparison, and collecting changed blocks.
package com.syncvault.services.delta;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
public final class DeltaGenerator {
    private DeltaGenerator() {}
    public static DeltaPackage generate(
            Path oldFile,
            Path newFile
    ) throws IOException {

        return generate(
                oldFile,
                newFile,
                FileBlocker.DEFAULT_BLOCK_SIZE
        );
    }
    public static DeltaPackage generate(
            Path oldFile,
            Path newFile,
            int blockSize
    ) throws IOException {

        validateFile(oldFile, "Old file");
        validateFile(newFile, "New file");

        if (blockSize <= 0) {
            throw new IllegalArgumentException(
                    "Block size must be greater than zero."
            );
        }

        long targetFileSize = Files.size(newFile);
        Map<Long, String> oldBlockHashes = new HashMap<>();

        FileBlocker.forEachBlock(
                oldFile,
                blockSize,
                (index, block) -> oldBlockHashes.put(
                        index,
                        BlockHasher.hash(block)
                )
        );
        Map<Long, byte[]> changedBlocks = new HashMap<>();

        FileBlocker.forEachBlock(
                newFile,
                blockSize,
                (index, newBlock) -> {

                    String oldHash = oldBlockHashes.get(index);

                    String newHash = BlockHasher.hash(newBlock);

                    boolean changed = oldHash == null
                            || !oldHash.equals(newHash);

                    if (!changed) {
                        changed = !Arrays.equals(
                                newBlock,
                                readOldBlock(oldFile, index, blockSize)
                        );
                    }

                    if (changed) {
                        changedBlocks.put(
                                index,
                                newBlock
                        );
                    }
                }
        );
        return new DeltaPackage(
                blockSize,
                targetFileSize,
                changedBlocks
        );
    }
    private static byte[] readOldBlock(
            Path oldFile,
            long blockIndex,
            int blockSize
    ) throws IOException {

        long offset = Math.multiplyExact(
                blockIndex,
                (long) blockSize
        );

        if (offset >= Files.size(oldFile)) {
            return new byte[0];
        }

        try (var channel = java.nio.channels.FileChannel.open(
                oldFile,
                java.nio.file.StandardOpenOption.READ
        )) {

            channel.position(offset);

            int expectedLength = (int) Math.min(
                    blockSize,
                    Files.size(oldFile) - offset
            );

            byte[] buffer = new byte[expectedLength];

            java.nio.ByteBuffer byteBuffer =
                    java.nio.ByteBuffer.wrap(buffer);

            while (byteBuffer.hasRemaining()) {

                int bytesRead = channel.read(byteBuffer);

                if (bytesRead == -1) {
                    throw new IOException(
                            "Unexpected end of old file."
                    );
                }
            }

            return buffer;
        }
    }
   private static void validateFile(
            Path file,
            String description
    ) throws IOException {

        if (file == null) {
            throw new IllegalArgumentException(
                    description + " path cannot be null."
            );
        }

        if (!Files.exists(file)) {
            throw new IOException(
                    description + " does not exist: " + file
            );
        }

        if (!Files.isRegularFile(file)) {
            throw new IOException(
                    description + " is not a regular file: " + file
            );
        }
    }
}
