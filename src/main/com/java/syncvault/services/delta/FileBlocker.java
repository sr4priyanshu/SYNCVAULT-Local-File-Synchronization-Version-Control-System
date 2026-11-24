// Reads a file in fixed-size byte blocks (defaulting to 4096 bytes) and passes each indexed block to a BlockConsumer.
package com.syncvault.services.delta;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
public class FileBlocker {
    public static final int DEFAULT_BLOCK_SIZE = 4096;
    public static void forEachBlock(
            Path filePath,
            int blockSize,
            BlockConsumer consumer
    ) throws IOException {

        if (filePath == null) {
            throw new IllegalArgumentException(
                    "File path cannot be null."
            );
        }

        if (consumer == null) {
            throw new IllegalArgumentException(
                    "Block consumer cannot be null."
            );
        }

        if (blockSize <= 0) {
            throw new IllegalArgumentException(
                    "Block size must be greater than zero."
            );
        }

        if (!Files.exists(filePath)) {
            throw new IOException(
                    "File does not exist: " + filePath
            );
        }

        if (!Files.isRegularFile(filePath)) {
            throw new IOException(
                    "Path is not a regular file: " + filePath
            );
        }

        long blockIndex = 0;

        try (InputStream inputStream =
                     Files.newInputStream(filePath)) {

            byte[] buffer = new byte[blockSize];

            while (true) {

                int totalRead = 0;

                while (totalRead < blockSize) {

                    int bytesRead = inputStream.read(
                            buffer,
                            totalRead,
                            blockSize - totalRead
                    );

                    if (bytesRead == -1) {
                        break;
                    }

                    totalRead += bytesRead;
                }

                if (totalRead == 0) {
                    break;
                }

                byte[] block = new byte[totalRead];

                System.arraycopy(
                        buffer,
                        0,
                        block,
                        0,
                        totalRead
                );

                consumer.accept(blockIndex, block);

                blockIndex++;

                if (totalRead < blockSize) {
                    break;
                }
            }
        }
    }
    @FunctionalInterface
    public interface BlockConsumer {

        void accept(
                long blockIndex,
                byte[] block
        ) throws IOException;
    }
}
