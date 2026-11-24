package com.syncvault.db.models;
public enum VersionStorageType {
    FULL,
    DELTA;
    public static VersionStorageType fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Storage type cannot be null or blank."
            );
        }
        try {
            return VersionStorageType.valueOf(
                    value.trim().toUpperCase()
            );
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Unsupported storage type: " + value
                            + ". Supported types are FULL and DELTA.",
                    e
            );
        }
    }
}