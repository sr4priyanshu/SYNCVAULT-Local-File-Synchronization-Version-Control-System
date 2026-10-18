package com.syncvault.db.dao;

import com.syncvault.db.DatabaseManager;
import com.syncvault.db.models.FileRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
public class FileDao {
    public long addFile(
            long folderId,
            String fileName,
            String filePath,
            String extension,
            String currentHash,
            long currentSize
    ) throws SQLException {
        String sql = """
                INSERT INTO files (
                    folder_id,
                    file_name,
                    file_path,
                    extension,
                    current_hash,
                    current_size
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(
                                sql,
                                PreparedStatement.RETURN_GENERATED_KEYS
                        )
        ) {
            statement.setLong(1, folderId);
            statement.setString(2, fileName);
            statement.setString(3, filePath);
            statement.setString(4, extension);
            statement.setString(5, currentHash);
            statement.setLong(6, currentSize);
            statement.executeUpdate();
            try (ResultSet generatedKeys =
                         statement.getGeneratedKeys()) {

                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                }
            }
            throw new SQLException(
                    "File created, but no file ID was returned."
            );
        }
    }
    public FileRecord findByPath(String filePath)
            throws SQLException {
        String sql = """
                SELECT
                    file_id,
                    folder_id,
                    file_name,
                    file_path,
                    extension,
                    current_hash,
                    current_size,
                    created_at,
                    updated_at
                FROM files
                WHERE file_path = ?
                """;
        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setString(1, filePath);
            try (ResultSet resultSet =
                         statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToFileRecord(resultSet);
                }
                return null;
            }
        }
    }
    public FileRecord findById(long fileId)
            throws SQLException {
        String sql = """
                SELECT
                    file_id,
                    folder_id,
                    file_name,
                    file_path,
                    extension,
                    current_hash,
                    current_size,
                    created_at,
                    updated_at
                FROM files
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
                    return mapResultSetToFileRecord(resultSet);
                }

                return null;
            }
        }
    }
    public List<FileRecord> findByFolderId(long folderId)
            throws SQLException {
        String sql = """
                SELECT
                    file_id,
                    folder_id,
                    file_name,
                    file_path,
                    extension,
                    current_hash,
                    current_size,
                    created_at,
                    updated_at
                FROM files
                WHERE folder_id = ?
                ORDER BY file_name
                """;
        List<FileRecord> files = new ArrayList<>();
        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setLong(1, folderId);
            try (ResultSet resultSet =
                         statement.executeQuery()) {

                while (resultSet.next()) {
                    files.add(
                            mapResultSetToFileRecord(resultSet)
                    );
                }
            }
        }

        return files;
    }
    public boolean updateCurrentState(
            long fileId,
            String currentHash,
            long currentSize
    ) throws SQLException {
        String sql = """
                UPDATE files
                SET
                    current_hash = ?,
                    current_size = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE file_id = ?
                """;
        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setString(1, currentHash);
            statement.setLong(2, currentSize);
            statement.setLong(3, fileId);

            return statement.executeUpdate() > 0;
        }
    }
    public boolean deleteFile(long fileId)
            throws SQLException {
        String sql = """
                DELETE FROM files
                WHERE file_id = ?
                """;
        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setLong(1, fileId);

            return statement.executeUpdate() > 0;
        }
    }
    public List<FileRecord> findAllByUserId(long userId)
        throws SQLException {

    String sql = """
            SELECT
                f.file_id,
                f.folder_id,
                f.file_name,
                f.file_path,
                f.extension,
                f.current_hash,
                f.current_size,
                f.created_at,
                f.updated_at
            FROM files f
            JOIN folders fo
                ON f.folder_id = fo.folder_id
            WHERE fo.user_id = ?
            ORDER BY f.file_name
            """;

    List<FileRecord> files = new ArrayList<>();

    try (
            Connection connection =
                    DatabaseManager.getConnection();

            PreparedStatement statement =
                    connection.prepareStatement(sql)
    ) {

        statement.setLong(1, userId);

        try (ResultSet resultSet =
                     statement.executeQuery()) {

            while (resultSet.next()) {
                files.add(
                        mapResultSetToFileRecord(resultSet)
                );
            }
        }
    }
    return files;
}
    private FileRecord mapResultSetToFileRecord(
            ResultSet resultSet
    ) throws SQLException {
        return new FileRecord(
                resultSet.getLong("file_id"),
                resultSet.getLong("folder_id"),
                resultSet.getString("file_name"),
                resultSet.getString("file_path"),
                resultSet.getString("extension"),
                resultSet.getString("current_hash"),
                resultSet.getLong("current_size"),
                resultSet.getString("created_at"),
                resultSet.getString("updated_at")
        );
    }
}