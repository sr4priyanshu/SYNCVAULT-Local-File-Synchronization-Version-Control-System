package com.syncvault.services.delta;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
public final class DeltaStorageService {

    private static final int MAGIC_NUMBER = 0x5356444C;
    private static final int FORMAT_VERSION = 1;

    private static final int MAX_BLOCK_COUNT = 10_000_000;

    private DeltaStorageService() {
    }
    public static Path save(
            DeltaPackage deltaPackage,
            Path destination
    ) throws IOException {

        if (deltaPackage == null || destination == null) {
            throw new IllegalArgumentException(
                    "Delta package and destination are required."
            );
        }

        Path absolutePath = destination.toAbsolutePath();
        Path parent = absolutePath.getParent();

        if (parent == null) {
            throw new IOException(
                    "Invalid delta destination."
            );
        }

        Files.createDirectories(parent);

        if (Files.exists(absolutePath)) {
            throw new IOException(
                    "Delta destination already exists: " + absolutePath
            );
        }

        Path tempFile = Files.createTempFile(
                parent,
                ".syncvault-",
                ".tmp"
        );

        boolean saved = false;

        try {

            try (
                DataOutputStream output = new DataOutputStream(
                    new BufferedOutputStream(
                        new GZIPOutputStream(
                            Files.newOutputStream(tempFile)
                        )
                    )
                )
            ) {

                output.writeInt(MAGIC_NUMBER);
                output.writeInt(FORMAT_VERSION);

                output.writeInt(deltaPackage.getBlockSize());
                output.writeLong(deltaPackage.getTargetFileSize());

                Map<Long, byte[]> blocks =
                        deltaPackage.getChangedBlocks();

                output.writeInt(blocks.size());

                for (Map.Entry<Long, byte[]> entry
                        : blocks.entrySet()) {

                    output.writeLong(entry.getKey());

                    byte[] block = entry.getValue();

                    output.writeInt(block.length);
                    output.write(block);
                }
            }

            try {
                Files.move(
                        tempFile,
                        absolutePath,
                        StandardCopyOption.ATOMIC_MOVE
                );

            } catch (AtomicMoveNotSupportedException e) {

                Files.move(
                        tempFile,
                        absolutePath
                );
            }

            saved = true;

            return absolutePath;

        } finally {

            if (!saved) {
                Files.deleteIfExists(tempFile);
            }
        }
    }
    public static DeltaPackage load(
            Path source
    ) throws IOException {

        if (source == null || !Files.isRegularFile(source)) {
            throw new IOException(
                    "Delta file does not exist: " + source
            );
        }

        try (
            DataInputStream input = new DataInputStream(
                new BufferedInputStream(
                    new GZIPInputStream(
                        Files.newInputStream(source)
                    )
                )
            )
        ) {

            int magic = input.readInt();

            if (magic != MAGIC_NUMBER) {
                throw new IOException(
                        "Invalid SyncVault delta file."
                );
            }

            int formatVersion = input.readInt();

            if (formatVersion != FORMAT_VERSION) {
                throw new IOException(
                        "Unsupported delta format version: "
                                + formatVersion
                );
            }

            int blockSize = input.readInt();
            long targetFileSize = input.readLong();

            if (blockSize <= 0 || targetFileSize < 0) {
                throw new IOException(
                        "Invalid delta metadata."
                );
            }

            int blockCount = input.readInt();

            long maximumBlocks = targetFileSize == 0
                    ? 0
                    : 1 + (targetFileSize - 1) / blockSize;

            if (blockCount < 0
                    || blockCount > MAX_BLOCK_COUNT
                    || blockCount > maximumBlocks) {

                throw new IOException(
                        "Invalid changed block count."
                );
            }

            Map<Long, byte[]> changedBlocks = new HashMap<>();

            for (int i = 0; i < blockCount; i++) {

                long blockIndex = input.readLong();
                int blockLength = input.readInt();

                if (blockIndex < 0
                        || blockLength <= 0
                        || blockLength > blockSize) {

                    throw new IOException(
                            "Invalid block metadata."
                    );
                }

                if (changedBlocks.containsKey(blockIndex)) {
                    throw new IOException(
                            "Duplicate block index: " + blockIndex
                    );
                }

                long offset;

                try {
                    offset = Math.multiplyExact(
                            blockIndex,
                            (long) blockSize
                    );
                } catch (ArithmeticException e) {
                    throw new IOException(
                            "Invalid block offset.", e
                    );
                }

                if (offset >= targetFileSize) {
                    throw new IOException(
                            "Block index exceeds target file size."
                    );
                }

                int expectedLength = (int) Math.min(
                        blockSize,
                        targetFileSize - offset
                );

                if (blockLength != expectedLength) {
                    throw new IOException(
                            "Invalid block length at index: "
                                    + blockIndex
                    );
                }

                byte[] block = new byte[blockLength];

                input.readFully(block);

                changedBlocks.put(blockIndex, block);
            }

            if (input.read() != -1) {
                throw new IOException(
                        "Unexpected trailing data in delta file."
                );
            }

            try {
                return new DeltaPackage(
                        blockSize,
                        targetFileSize,
                        changedBlocks
                );

            } catch (IllegalArgumentException e) {
                throw new IOException(
                        "Invalid delta package contents.", e
                );
            }
        }
    }
}