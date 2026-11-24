package com.syncvault.db.dao;
import com.syncvault.db.DatabaseManager;
import com.syncvault.db.models.VersionRecord;
import com.syncvault.db.models.VersionStorageType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
public class VersionDao {
    public long addVersion(
            long fileId,
            int versionNumber,
            String contentHash,
            long fileSize,
            String storageType,
            String storagePath,
            Long parentVersionId
    ) throws SQLException {

        validateVersionData(
                fileId, versionNumber, contentHash, fileSize,
                storageType, storagePath, parentVersionId
        );

        try (Connection connection = DatabaseManager.getConnection()) {
            return insertVersion(
                    connection, fileId, versionNumber, contentHash,
                    fileSize, storageType, storagePath, parentVersionId
            );
        }
    }
    public long addNextVersion(
            long fileId,
            String contentHash,
            long fileSize,
            String storageType,
            String storagePath,
            Long parentVersionId
    ) throws SQLException {

        validateVersionData(
                fileId, 1, contentHash, fileSize,
                storageType, storagePath, parentVersionId
        );

        String nextNumberSql = """
                SELECT COALESCE(MAX(version_number), 0) + 1
                FROM versions
                WHERE file_id = ?
                """;

        try (Connection connection = DatabaseManager.getConnection()) {
            boolean transactionStarted = false;

            try {
                // Acquire the SQLite write reservation before reading MAX().
                try (Statement statement = connection.createStatement()) {
                    statement.execute("BEGIN IMMEDIATE");
                }

                transactionStarted = true;

                int nextVersionNumber;

                try (PreparedStatement statement =
                             connection.prepareStatement(nextNumberSql)) {

                    statement.setLong(1, fileId);

                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (!resultSet.next()) {
                            throw new SQLException(
                                    "Unable to allocate the next version number."
                            );
                        }

                        nextVersionNumber = resultSet.getInt(1);
                    }
                }

                if (nextVersionNumber < 1) {
                    throw new SQLException(
                            "The next version number is outside the supported range."
                    );
                }

                long versionId = insertVersion(
                        connection,
                        fileId,
                        nextVersionNumber,
                        contentHash,
                        fileSize,
                        storageType,
                        storagePath,
                        parentVersionId
                );

                try (Statement statement = connection.createStatement()) {
                    statement.execute("COMMIT");
                }

                transactionStarted = false;
                return versionId;

            } catch (SQLException | RuntimeException exception) {

                if (transactionStarted) {
                    try (Statement statement = connection.createStatement()) {
                        statement.execute("ROLLBACK");
                    } catch (SQLException rollbackException) {
                        exception.addSuppressed(rollbackException);
                    }
                }

                throw exception;
            }
        }
    }

    public VersionRecord findById(long versionId) throws SQLException {

        String sql = """
                SELECT
                    version_id,
                    file_id,
                    version_number,
                    content_hash,
                    file_size,
                    created_at,
                    storage_type,
                    storage_path,
                    parent_version_id
                FROM versions
                WHERE version_id = ?
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, versionId);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next()
                        ? mapResultSetToVersionRecord(resultSet)
                        : null;
            }
        }
    }

    public List<VersionRecord> findByFileId(long fileId)
            throws SQLException {

        String sql = """
                SELECT
                    version_id,
                    file_id,
                    version_number,
                    content_hash,
                    file_size,
                    created_at,
                    storage_type,
                    storage_path,
                    parent_version_id
                FROM versions
                WHERE file_id = ?
                ORDER BY version_number ASC
                """;

        List<VersionRecord> versions = new ArrayList<>();

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, fileId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    versions.add(mapResultSetToVersionRecord(resultSet));
                }
            }
        }

        return versions;
    }

    public VersionRecord findLatestVersion(long fileId)
            throws SQLException {

        String sql = """
                SELECT
                    version_id,
                    file_id,
                    version_number,
                    content_hash,
                    file_size,
                    created_at,
                    storage_type,
                    storage_path,
                    parent_version_id
                FROM versions
                WHERE file_id = ?
                ORDER BY version_number DESC
                LIMIT 1
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, fileId);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next()
                        ? mapResultSetToVersionRecord(resultSet)
                        : null;
            }
        }
    }
    public int getNextVersionNumber(long fileId) throws SQLException {

        if (fileId <= 0) {
            throw new IllegalArgumentException(
                    "fileId must be greater than zero."
            );
        }

        String sql = """
                SELECT COALESCE(MAX(version_number), 0) + 1
                FROM versions
                WHERE file_id = ?
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, fileId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException(
                            "Unable to read the next version number."
                    );
                }

                int nextVersionNumber = resultSet.getInt(1);

                if (nextVersionNumber < 1) {
                    throw new SQLException(
                            "The next version number is outside the supported range."
                    );
                }

                return nextVersionNumber;
            }
        }
    }

    public VersionRecord findByHash(String contentHash)
            throws SQLException {

        if (contentHash == null || contentHash.isBlank()) {
            throw new IllegalArgumentException(
                    "contentHash must not be blank."
            );
        }

        String sql = """
                SELECT
                    version_id,
                    file_id,
                    version_number,
                    content_hash,
                    file_size,
                    created_at,
                    storage_type,
                    storage_path,
                    parent_version_id
                FROM versions
                WHERE content_hash = ?
                ORDER BY created_at DESC
                LIMIT 1
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, contentHash);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next()
                        ? mapResultSetToVersionRecord(resultSet)
                        : null;
            }
        }
    }

    private long insertVersion(
            Connection connection,
            long fileId,
            int versionNumber,
            String contentHash,
            long fileSize,
            String storageType,
            String storagePath,
            Long parentVersionId
    ) throws SQLException {

        String sql = """
                INSERT INTO versions (
                    file_id,
                    version_number,
                    content_hash,
                    file_size,
                    storage_type,
                    storage_path,
                    parent_version_id
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setLong(1, fileId);
            statement.setInt(2, versionNumber);
            statement.setString(3, contentHash);
            statement.setLong(4, fileSize);
            statement.setString(5, storageType);
            statement.setString(6, storagePath);

            if (parentVersionId == null) {
                statement.setNull(7, Types.INTEGER);
            } else {
                statement.setLong(7, parentVersionId);
            }

            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                }
            }
        }

        throw new SQLException(
                "Version created, but no version ID was returned."
        );
    }

    private void validateVersionData(
            long fileId,
            int versionNumber,
            String contentHash,
            long fileSize,
            String storageType,
            String storagePath,
            Long parentVersionId
    ) {

        if (fileId <= 0) {
            throw new IllegalArgumentException(
                    "fileId must be greater than zero."
            );
        }

        if (versionNumber < 1) {
            throw new IllegalArgumentException(
                    "versionNumber must be at least 1."
            );
        }

        if (contentHash == null || contentHash.isBlank()) {
            throw new IllegalArgumentException(
                    "contentHash must not be blank."
            );
        }

        if (fileSize < 0) {
            throw new IllegalArgumentException(
                    "fileSize must not be negative."
            );
        }

        if (storageType == null || storageType.isBlank()) {
            throw new IllegalArgumentException(
                    "storageType must not be blank."
            );
        }

        if (storagePath == null || storagePath.isBlank()) {
            throw new IllegalArgumentException(
                    "storagePath must not be blank."
            );
        }

        if (parentVersionId != null && parentVersionId <= 0) {
            throw new IllegalArgumentException(
                    "parentVersionId must be greater than zero when supplied."
            );
        }
        VersionStorageType type = VersionStorageType.fromString(storageType);
        if (type == VersionStorageType.DELTA && parentVersionId == null) 
                {
            throw new IllegalArgumentException(
                    "A DELTA version must have a parent version."
            );
        }
    }
    private VersionRecord mapResultSetToVersionRecord(
            ResultSet resultSet
    ) throws SQLException {
        long parentVersionId = resultSet.getLong("parent_version_id");
        Long parentId = resultSet.wasNull() ? null : parentVersionId;
        return new VersionRecord(
                resultSet.getLong("version_id"),
                resultSet.getLong("file_id"),
                resultSet.getInt("version_number"),
                resultSet.getString("content_hash"),
                resultSet.getLong("file_size"),
                resultSet.getString("created_at"),
                resultSet.getString("storage_type"),
                resultSet.getString("storage_path"),
                parentId
        );
    }
}
