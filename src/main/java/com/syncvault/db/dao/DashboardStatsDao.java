package com.syncvault.db.dao;
import com.syncvault.db.DatabaseManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
public class DashboardStatsDao {
    public DashboardStats getStats(long userId)
            throws SQLException {
        String sql = """
                SELECT
                    (
                        SELECT COUNT(*)
                        FROM folders
                        WHERE user_id = ?
                    ) AS tracked_folders,

                    (
                        SELECT COUNT(*)
                        FROM files f
                        JOIN folders fo
                            ON f.folder_id = fo.folder_id
                        WHERE fo.user_id = ?
                    ) AS total_files,

                    (
                        SELECT COUNT(*)
                        FROM versions v
                        JOIN files f
                            ON v.file_id = f.file_id
                        JOIN folders fo
                            ON f.folder_id = fo.folder_id
                        WHERE fo.user_id = ?
                    ) AS total_versions,

                    (
                        SELECT COALESCE(SUM(v.file_size), 0)
                        FROM versions v
                        JOIN files f
                            ON v.file_id = f.file_id
                        JOIN folders fo
                            ON f.folder_id = fo.folder_id
                        WHERE fo.user_id = ?
                    ) AS recorded_version_bytes
                """;

        try (
                Connection connection =
                        DatabaseManager.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setLong(1, userId);
            statement.setLong(2, userId);
            statement.setLong(3, userId);
            statement.setLong(4, userId);

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                if (resultSet.next()) {

                    return new DashboardStats(
                            resultSet.getLong("tracked_folders"),
                            resultSet.getLong("total_files"),
                            resultSet.getLong("total_versions"),
                            resultSet.getLong("recorded_version_bytes")
                    );
                }

                throw new SQLException(
                        "Unable to retrieve dashboard statistics."
                );
            }
        }
    }

    public record DashboardStats(
            long trackedFolders,
            long totalFiles,
            long totalVersions,
            long recordedVersionBytes
    ) {
    }
}