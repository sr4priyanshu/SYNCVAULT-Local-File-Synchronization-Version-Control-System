package com.syncvault.services.delta;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
public final class DeltaPackage {
    private final int blockSize;
    private final long targetFileSize;
    private final Map<Long, byte[]> changedBlocks;

    public DeltaPackage(
            int blockSize,
            long targetFileSize,
            Map<Long, byte[]> changedBlocks
    ) {

        if (blockSize <= 0) {
            throw new IllegalArgumentException(
                    "Block size must be greater than zero."
            );
        }

        if (targetFileSize < 0) {
            throw new IllegalArgumentException(
                    "Target file size cannot be negative."
            );
        }

        if (changedBlocks == null) {
            throw new IllegalArgumentException(
                    "Changed blocks cannot be null."
            );
        }

        this.blockSize = blockSize;
        this.targetFileSize = targetFileSize;

        this.changedBlocks = new TreeMap<>();

        for (Map.Entry<Long, byte[]> entry
                : changedBlocks.entrySet()) {

            Long index = entry.getKey();
            byte[] data = entry.getValue();

            if (index == null || index < 0) {
                throw new IllegalArgumentException(
                        "Block index must be non-negative."
                );
            }

            if (data == null || data.length == 0
                    || data.length > blockSize) {

                throw new IllegalArgumentException(
                        "Invalid block data at index: " + index
                );
            }

            long blockStart = Math.multiplyExact(
                    index,
                    (long) blockSize
            );

            if (blockStart >= targetFileSize) {
                throw new IllegalArgumentException(
                        "Block index exceeds target file size: "
                                + index
                );
            }

            long expectedLength = Math.min(
                    (long) blockSize,
                    targetFileSize - blockStart
            );

            if (data.length != expectedLength) {
                throw new IllegalArgumentException(
                        "Incorrect block length at index: " + index
                );
            }

            this.changedBlocks.put(
                    index,
                    data.clone()
            );
        }
    }

    public int getBlockSize() {
        return blockSize;
    }

    public long getTargetFileSize() {
        return targetFileSize;
    }

    public int getChangedBlockCount() {
        return changedBlocks.size();
    }
    public Map<Long, byte[]> getChangedBlocks() {

        Map<Long, byte[]> copy = new TreeMap<>();

        for (Map.Entry<Long, byte[]> entry
                : changedBlocks.entrySet()) {

            copy.put(
                    entry.getKey(),
                    entry.getValue().clone()
            );
        }

        return Collections.unmodifiableMap(copy);
    }
    public byte[] getBlock(long blockIndex) {

        byte[] block = changedBlocks.get(blockIndex);

        return block == null ? null : block.clone();
    }
   public long getChangedDataSize() {

        long total = 0;

        for (byte[] block : changedBlocks.values()) {
            total = Math.addExact(total, block.length);
        }

        return total;
    }

    @Override
    public String toString() {

        return "DeltaPackage{" +
                "blockSize=" + blockSize +
                ", targetFileSize=" + targetFileSize +
                ", changedBlockCount=" + changedBlocks.size() +
                ", changedDataSize=" + getChangedDataSize() +
                '}';
    }
}