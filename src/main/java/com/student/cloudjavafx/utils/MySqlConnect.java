package com.student.cloudjavafx.utils;

import java.sql.Connection;
import java.sql.SQLException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class MySqlConnect {

    private static final String URL = "jdbc:mysql://localhost:3306/cloudloadbalancingsystem2";
    private static final String USER = "root";
    private static final String PASSWORD = "";
    private static HikariDataSource dataSource;

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(URL);
            config.setUsername(USER);
            config.setPassword(PASSWORD);
            config.setMaximumPoolSize(20);
            config.setMinimumIdle(10);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);

            // MySQL-specific optimizations
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "500");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "4096");
            config.addDataSourceProperty("useServerPrepStmts", "true");
            config.addDataSourceProperty("elideSetAutoCommits", "true");
            config.addDataSourceProperty("maintainTimeStats", "false");

            dataSource = new HikariDataSource(config);

            // Test the connection upon initialization
            try (Connection testConn = dataSource.getConnection()) {
                System.out.println("✅ Successful connection to MySQL database");
            }
        } catch (ClassNotFoundException e) {
            System.err.println("❌ MySQL Driver not found: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("❌ Database connection test failed: " + e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("MySQL DataSource not initialized");
        }
        return dataSource.getConnection();
    }

    public static void closePool() {
        if (dataSource != null) {
            dataSource.close();
            System.out.println("The MySQL connection pool was closed");
        }
    }

    // Helper function to verify the database connection
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn.isValid(2); // Test the connection within 2 seconds
        } catch (SQLException e) {
            System.err.println("Connection test failed: " + e.getMessage());
            return false;
        }
    }
}