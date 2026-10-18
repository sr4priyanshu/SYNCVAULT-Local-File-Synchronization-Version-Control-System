package com.syncvault.db;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
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
            String schema = new String(
                    inputStream.readAllBytes(),
                    StandardCharsets.UTF_8
            );
            executeSchema(connection, schema);
        } catch (SQLException | IOException e) {
            throw new RuntimeException(
                    "Failed to initialize SyncVault database.",
                    e
            );
        }
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