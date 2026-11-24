// Provides SHA-256 hex hashing and constant-time equality comparison for byte blocks.
package com.syncvault.services.delta;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class BlockHasher {

    private BlockHasher() {
    }

    public static String hash(byte[] block) {

        if (block == null) {
            throw new IllegalArgumentException("Block cannot be null.");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(block));

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "SHA-256 algorithm is unavailable.", e
            );
        }
    }

    public static boolean areEqual(byte[] first, byte[] second) {

        if (first == null || second == null) {
            return false;
        }

        return MessageDigest.isEqual(
                hexToBytes(hash(first)),
                hexToBytes(hash(second))
        );
    }

    private static byte[] hexToBytes(String hex) {
        return HexFormat.of().parseHex(hex);
    }
}
