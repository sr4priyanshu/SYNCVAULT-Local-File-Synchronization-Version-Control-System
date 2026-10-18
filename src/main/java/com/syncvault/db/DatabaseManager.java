package com.syncvault.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private static final String DEFAULT_DATABASE_URL =
            "jdbc:sqlite:database/syncvault.db";

    private static final String DATABASE_URL_PROPERTY =
            "syncvault.database.url";


    public static Connection getConnection()
            throws SQLException {

        // Use a separate database URL during automated tests.
        // Normal application runs still use syncvault.db.
        String databaseUrl = System.getProperty(
                DATABASE_URL_PROPERTY,
                DEFAULT_DATABASE_URL
        );

        Connection connection =
                DriverManager.getConnection(databaseUrl);

        try (Statement statement = connection.createStatement()) {

            statement.execute("PRAGMA foreign_keys = ON");
        }

        return connection;
    }
}