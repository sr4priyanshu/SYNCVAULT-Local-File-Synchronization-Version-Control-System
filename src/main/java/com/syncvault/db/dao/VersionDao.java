package com.syncvault.db.dao;
import com.syncvault.db.DatabaseManager;
import com.syncvault.db.models.VersionRecord;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(
                                sql,
                                PreparedStatement.RETURN_GENERATED_KEYS
                        )
        ) {
            statement.setLong(1, fileId);
            statement.setInt(2, versionNumber);
            statement.setString(3, contentHash);
            statement.setLong(4, fileSize);
            statement.setString(5, storageType);
            statement.setString(6, storagePath);
            if (parentVersionId == null) {
                statement.setNull(7, java.sql.Types.INTEGER);
            } else {
                statement.setLong(7, parentVersionId);
            }
            statement.executeUpdate();
            try (ResultSet generatedKeys =
                         statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                }
            }
            throw new SQLException(
                    "Version created, but no version ID was returned."
            );
        }
    }
    public VersionRecord findById(long versionId)
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
                WHERE version_id = ?
                """;
        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setLong(1, versionId);
            try (ResultSet resultSet =
                         statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToVersionRecord(resultSet);
                }
                return null;
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
        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setLong(1, fileId);
            try (ResultSet resultSet =
                         statement.executeQuery()) {

                while (resultSet.next()) {
                    versions.add(
                            mapResultSetToVersionRecord(resultSet)
                    );
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
        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setLong(1, fileId);
            try (ResultSet resultSet =
                         statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToVersionRecord(resultSet);
                }
                return null;
            }
        }
    }
    public int getNextVersionNumber(long fileId)
            throws SQLException {
        String sql = """
                SELECT COALESCE(MAX(version_number), 0) + 1
                FROM versions
                WHERE file_id = ?
                """;
        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setLong(1, fileId);
            try (ResultSet resultSet =
                         statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
                return 1;
            }
        }
    }
    public VersionRecord findByHash(String contentHash)
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
                WHERE content_hash = ?
                ORDER BY created_at DESC
                LIMIT 1
                """;
        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setString(1, contentHash);
            try (ResultSet resultSet =
                         statement.executeQuery()) {

                if (resultSet.next()) {
                    return mapResultSetToVersionRecord(resultSet);
                }
                return null;
            }
        }
    }
    private VersionRecord mapResultSetToVersionRecord(
            ResultSet resultSet
    ) throws SQLException {
        long parentVersionId =
                resultSet.getLong("parent_version_id");

        Long parentId;
        if (resultSet.wasNull()) {
            parentId = null;
        } else {
            parentId = parentVersionId;
        }
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