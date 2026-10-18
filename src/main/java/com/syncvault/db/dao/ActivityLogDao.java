package com.syncvault.db.dao;
import com.syncvault.db.DatabaseManager;
import com.syncvault.db.models.ActivityLog;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
public class ActivityLogDao {
    public long addLog(
            Long fileId,
            String action,
            String details
    ) throws SQLException {
        String sql = """
                INSERT INTO activity_logs (
                    file_id,
                    action,
                    details
                )
                VALUES (?, ?, ?)
                """;
        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(
                                sql,
                                PreparedStatement.RETURN_GENERATED_KEYS
                        )
        ) {
            if (fileId == null) {
                statement.setNull(1, java.sql.Types.INTEGER);
            } else {
                statement.setLong(1, fileId);
            }
            statement.setString(2, action);
            statement.setString(3, details);
            statement.executeUpdate();
            try (ResultSet generatedKeys =
                         statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                }
            }
            throw new SQLException(
                    "Activity log created, but no log ID was returned."
            );
        }
    }
    public ActivityLog findById(long logId)
            throws SQLException {
        String sql = """
                SELECT
                    log_id,
                    file_id,
                    action,
                    details,
                    created_at
                FROM activity_logs
                WHERE log_id = ?
                """;
        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setLong(1, logId);
            try (ResultSet resultSet =
                         statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToActivityLog(resultSet);
                }
                return null;
            }
        }
    }
    public List<ActivityLog> findByFileId(long fileId)
            throws SQLException {
        String sql = """
                SELECT
                    log_id,
                    file_id,
                    action,
                    details,
                    created_at
                FROM activity_logs
                WHERE file_id = ?
                ORDER BY created_at DESC
                """;
        List<ActivityLog> logs = new ArrayList<>();
        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setLong(1, fileId);
            try (ResultSet resultSet =
                         statement.executeQuery()) {
                while (resultSet.next()) {
                    logs.add(
                            mapResultSetToActivityLog(resultSet)
                    );
                }
            }
        }
        return logs;
    }
    public List<ActivityLog> findRecentLogs(int limit)
            throws SQLException {
        String sql = """
                SELECT
                    log_id,
                    file_id,
                    action,
                    details,
                    created_at
                FROM activity_logs
                ORDER BY created_at DESC
                LIMIT ?
                """;
        List<ActivityLog> logs = new ArrayList<>();
        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setInt(1, limit);
            try (ResultSet resultSet =
                         statement.executeQuery()) {
                while (resultSet.next()) {
                    logs.add(
                            mapResultSetToActivityLog(resultSet)
                    );
                }
            }
        }
        return logs;
    }
    private ActivityLog mapResultSetToActivityLog(
            ResultSet resultSet
    ) throws SQLException {
        long fileIdValue =
                resultSet.getLong("file_id");
        Long fileId;
        if (resultSet.wasNull()) {
            fileId = null;
        } else {
            fileId = fileIdValue;
        }
        return new ActivityLog(
                resultSet.getLong("log_id"),
                fileId,
                resultSet.getString("action"),
                resultSet.getString("details"),
                resultSet.getString("created_at")
        );
    }
}