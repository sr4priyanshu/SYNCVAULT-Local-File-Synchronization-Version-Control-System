package com.syncvault.db.dao;
import com.syncvault.db.DatabaseManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
public class UserDao {
    public long createUser(String username) throws SQLException {
        String sql = """
                INSERT INTO users (username)
                VALUES (?)
                """;
        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(
                                sql,
                                PreparedStatement.RETURN_GENERATED_KEYS
                        )
        ) {
            statement.setString(1, username);
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {

                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                }
            }
            throw new SQLException(
                    "User created, but no user ID was returned."
            );
        }
    }
    public Long findUserIdByUsername(String username)
            throws SQLException {
        String sql = """
                SELECT user_id
                FROM users
                WHERE username = ?
                """;
        try (
                Connection connection = DatabaseManager.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return resultSet.getLong("user_id");
                }

                return null;
            }
        }
    }
    public boolean userExists(String username)
            throws SQLException {
        return findUserIdByUsername(username) != null;
    }
}