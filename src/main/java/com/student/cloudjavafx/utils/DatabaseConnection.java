package com.student.cloudjavafx.utils;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConnection {

    // Define an enum to specify the database type with more descriptive names
    public enum DatabaseType {
        MYSQL, 
        SQLITE
    }

    /**
     * Retrieves a database connection based on the specified type.
     * @param type The desired database type (MySQL or SQLite).
     * @return A valid Connection object.
     * @throws SQLException if a database access error occurs.
     */
    public static Connection getConnection(DatabaseType type) throws SQLException {
        switch (type) {
            case MYSQL:
                // Return a connection from the MySqlConnect class
                return MySqlConnect.getConnection();
            case SQLITE:
                // Return a connection from the SqlLiteConnect class
                return SqlLiteConnect.getConnection();
            default:
                throw new UnsupportedOperationException("Unsupported database type: " + type);
        }
    }

    /**
     * Closes the connection pool based on the specified database type.
     * @param type The database type whose pool should be closed.
     */
    public static void closePool(DatabaseType type) {
        switch (type) {
            case MYSQL:
                MySqlConnect.closePool();
                break;
            case SQLITE:
                SqlLiteConnect.closePool();
                break;
        }
    }
}
