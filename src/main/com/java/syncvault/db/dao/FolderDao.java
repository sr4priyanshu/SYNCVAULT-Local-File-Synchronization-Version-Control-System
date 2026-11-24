
package com.syncvault.db.dao;

import com.syncvault.db.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FolderDao {

    public long addFolder(long userId, String folderPath)
            throws SQLException {

        validatePositiveId(userId, "User ID");
        validateFolderPath(folderPath);

        String sql = """
                INSERT INTO folders (user_id, folder_path)
                VALUES (?, ?)
                """;

        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        sql,
                        PreparedStatement.RETURN_GENERATED_KEYS
                )
        ) {
            statement.setLong(1, userId);
            statement.setString(2, folderPath);

            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                }
            }

            throw new SQLException(
                    "Folder created, but no folder ID was returned."
            );
        }
    }

    public boolean folderExists(long userId, String folderPath)
            throws SQLException {

        validatePositiveId(userId, "User ID");
        validateFolderPath(folderPath);

        String sql = """
                SELECT 1
                FROM folders
                WHERE user_id = ? AND folder_path = ?
                LIMIT 1
                """;

        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setLong(1, userId);
            statement.setString(2, folderPath);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public Long findFolderIdByPath(long userId, String folderPath)
            throws SQLException {

        validatePositiveId(userId, "User ID");
        validateFolderPath(folderPath);

        String sql = """
                SELECT folder_id
                FROM folders
                WHERE user_id = ? AND folder_path = ?
                LIMIT 1
                """;

        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setLong(1, userId);
            statement.setString(2, folderPath);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong("folder_id");
                }

                return null;
            }
        }
    }

    public List<String> findAllFolderPaths(long userId)
            throws SQLException {

        validatePositiveId(userId, "User ID");

        String sql = """
                SELECT folder_path
                FROM folders
                WHERE user_id = ?
                ORDER BY folder_path
                """;

        List<String> folderPaths = new ArrayList<>();

        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setLong(1, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    folderPaths.add(
                            resultSet.getString("folder_path")
                    );
                }
            }
        }

        return folderPaths;
    }

    public boolean removeFolder(long folderId)
            throws SQLException {

        validatePositiveId(folderId, "Folder ID");

        String sql = """
                DELETE FROM folders
                WHERE folder_id = ?
                """;

        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setLong(1, folderId);

            return statement.executeUpdate() > 0;
        }
    }

    private void validatePositiveId(long id, String fieldName) {
        if (id <= 0) {
            throw new IllegalArgumentException(
                    fieldName + " must be positive."
            );
        }
    }

    private void validateFolderPath(String folderPath) {
        if (folderPath == null || folderPath.isBlank()) {
            throw new IllegalArgumentException(
                    "Folder path cannot be blank."
            );
        }
    }
}
