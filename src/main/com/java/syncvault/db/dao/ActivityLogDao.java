package com.syncvault.db.dao;
import com.syncvault.db.DatabaseManager;
import com.syncvault.db.models.ActivityLog;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
public class ActivityLogDao {

    public long addLog(
            Long fileId,
            String action,
            String details
    ) throws SQLException {

        if (fileId != null && fileId <= 0) {
            throw new IllegalArgumentException(
                    "fileId must be greater than zero when supplied."
            );
        }

        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException(
                    "action must not be blank."
            );
        }

        String sql = """
                INSERT INTO activity_logs (
                    file_id,
                    action,
                    details
                )
                VALUES (?, ?, ?)
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            if (fileId == null) {
                statement.setNull(1, Types.INTEGER);
            } else {
                statement.setLong(1, fileId);
            }

            statement.setString(2, action);
            statement.setString(3, details);

            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                }
            }

            throw new SQLException(
                    "Activity log created, but no log ID was returned."
            );
        }
    }

    public ActivityLog findById(long logId) throws SQLException {

        if (logId <= 0) {
            throw new IllegalArgumentException(
                    "logId must be greater than zero."
            );
        }

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

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, logId);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next()
                        ? mapResultSetToActivityLog(resultSet)
                        : null;
            }
        }
    }

    public List<ActivityLog> findByFileId(long fileId)
            throws SQLException {

        if (fileId <= 0) {
            throw new IllegalArgumentException(
                    "fileId must be greater than zero."
            );
        }

        String sql = """
                SELECT
                    log_id,
                    file_id,
                    action,
                    details,
                    created_at
                FROM activity_logs
                WHERE file_id = ?
                ORDER BY created_at DESC, log_id DESC
                """;

        List<ActivityLog> logs = new ArrayList<>();

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, fileId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    logs.add(mapResultSetToActivityLog(resultSet));
                }
            }
        }

        return logs;
    }

    public List<ActivityLog> findRecentLogs(int limit)
            throws SQLException {

        if (limit < 0) {
            throw new IllegalArgumentException(
                    "limit must not be negative."
            );
        }

        if (limit == 0) {
            return new ArrayList<>();
        }

        String sql = """
                SELECT
                    log_id,
                    file_id,
                    action,
                    details,
                    created_at
                FROM activity_logs
                ORDER BY created_at DESC, log_id DESC
                LIMIT ?
                """;

        List<ActivityLog> logs = new ArrayList<>();

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, limit);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    logs.add(mapResultSetToActivityLog(resultSet));
                }
            }
        }

        return logs;
    }

    private ActivityLog mapResultSetToActivityLog(
            ResultSet resultSet
    ) throws SQLException {

        long fileIdValue = resultSet.getLong("file_id");
        Long fileId = resultSet.wasNull() ? null : fileIdValue;

        return new ActivityLog(
                resultSet.getLong("log_id"),
                fileId,
                resultSet.getString("action"),
                resultSet.getString("details"),
                resultSet.getString("created_at")
        );
    }
}
