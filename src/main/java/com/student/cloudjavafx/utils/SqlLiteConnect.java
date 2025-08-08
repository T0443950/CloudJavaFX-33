package com.student.cloudjavafx.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.sqlite.SQLiteConfig;

public class SqlLiteConnect {
    // The connection URL for the SQLite database.
    // NOTE: This path should be updated to a more dynamic path if needed.
    private static final String URL = "jdbc:sqlite:C:\\Users\\asyrn\\OneDrive\\Desktop\\CloudJavaFX\\cloudloadbalanc.db";
    
    // Use a single, shared connection to prevent SQLite busy errors.
    private static Connection connection;

    /**
     * Private constructor to prevent instantiation.
     */
    private SqlLiteConnect() {}

    /**
     * Initializes the SQLite connection. This method should be called once at application startup.
     * It synchronizes access to prevent race conditions during initialization.
     */
    public static synchronized void initializeConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                // Load the SQLite JDBC driver
                Class.forName("org.sqlite.JDBC");
                
                // Configure SQLite to be more lenient with concurrent access.
                SQLiteConfig config = new SQLiteConfig();
                config.setJournalMode(SQLiteConfig.JournalMode.WAL);
                config.setSynchronous(SQLiteConfig.SynchronousMode.NORMAL);

                // Establish the connection.
                connection = DriverManager.getConnection(URL, config.toProperties());
                System.out.println("✅ Successful connection to SQLite database");
            } catch (ClassNotFoundException e) {
                System.err.println("❌ SQLite Driver not found: " + e.getMessage());
                throw new SQLException("SQLite JDBC Driver not found", e);
            }
        }
    }

    /**
     * Retrieves the single, initialized SQLite database connection.
     * @return A valid Connection object.
     * @throws SQLException if the connection has not been initialized.
     */
    public static Connection getConnection() throws SQLException {
        // Ensure the connection is initialized before returning it.
        // Re-initialize if for some reason the connection was closed.
        if (connection == null || connection.isClosed()) {
            System.err.println("❌ SQLite connection pool was not initialized or has been closed. Re-initializing...");
            initializeConnection();
        }
        return connection;
    }

    /**
     * Closes the single SQLite database connection.
     * This should be called when the application is shutting down.
     */
    public static void closePool() {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("The SQLite connection was closed");
            } catch (SQLException e) {
                System.err.println("❌ Error closing SQLite connection: " + e.getMessage());
            }
        }
    }
}
