package com.syncvault.db;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
public class DatabaseInitializer {

    private static final String SCHEMA_FILE = "/schema.sql";

    public static void initialize() {

        try (
            Connection connection = DatabaseManager.getConnection();
            InputStream inputStream =
                    DatabaseInitializer.class.getResourceAsStream(SCHEMA_FILE)
        ) {

            if (inputStream == null) {
                throw new IllegalStateException(
                        "schema.sql could not be found."
                );
            }
           // Step 1: Read schema.sql.
String schema = new String(
        inputStream.readAllBytes(),
        StandardCharsets.UTF_8
);
executeSchema(connection, schema);
migrateFilesTable(connection);

System.out.println("SyncVault database initialized successfully.");

            System.out.println("SyncVault database initialized successfully.");

        } catch (SQLException | IOException e) {

            throw new RuntimeException(
                    "Failed to initialize SyncVault database.",
                    e
            );
        }
    }
    private static void migrateFilesTable(
            Connection connection
    ) throws SQLException {

        if (!tableExists(connection, "files")) {
            System.out.println(
                    "Files table does not exist. Migration skipped."
            );
            return;
        }

        if (!columnExists(connection, "files", "is_missing")) {

            String sql = """
                    ALTER TABLE files
                    ADD COLUMN is_missing INTEGER NOT NULL DEFAULT 0
                    CHECK (is_missing IN (0, 1))
                    """;

            try (Statement statement = connection.createStatement()) {
                statement.execute(sql);
            }

            System.out.println(
                    "Migration completed: added is_missing column."
            );

        } else {

            System.out.println(
                    "Migration skipped: is_missing column already exists."
            );
        }
    }
    private static boolean tableExists(
            Connection connection,
            String tableName
    ) throws SQLException {

        String sql = """
                SELECT name
                FROM sqlite_master
                WHERE type = 'table' AND name = ?
                """;

        try (
            var statement = connection.prepareStatement(sql)
        ) {

            statement.setString(1, tableName);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }
    private static boolean columnExists(
            Connection connection,
            String tableName,
            String columnName
    ) throws SQLException {

        String sql = "PRAGMA table_info(" + tableName + ")";

        try (
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(sql)
        ) {

            while (resultSet.next()) {

                String existingColumn =
                        resultSet.getString("name");

                if (columnName.equalsIgnoreCase(existingColumn)) {
                    return true;
                }
            }
        }

        return false;
    }
   private static void executeSchema(
            Connection connection,
            String schema
    ) throws SQLException {

        String[] statements = schema.split(";");

        try (Statement statement = connection.createStatement()) {

            for (String sql : statements) {

                sql = sql.trim();

                if (!sql.isEmpty()) {
                    statement.execute(sql);
                }
            }
        }
    }
}