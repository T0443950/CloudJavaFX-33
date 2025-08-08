package com.student.cloudjavafx.loadbalancing;

import com.student.cloudjavafx.auth.SessionManager;
import com.student.cloudjavafx.utils.DatabaseConnection;
import com.student.cloudjavafx.utils.DatabaseConnection.DatabaseType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Random;

/**
 * Monitors and reports performance metrics for a specific file server.
 * This class simulates collecting metrics and updating the `performance_metrics`
 * table in the database. In a real-world scenario, this would run as a
 * scheduled task on each file server.
 */
public class PerformanceMonitor {
 DatabaseType dbType = SessionManager.getCurrentDatabaseType();
    // The ID of the server this monitor is running on.
    // This would typically be passed in or read from a configuration file.
    private final int serverId;
    private final Random random = new Random();

    public PerformanceMonitor(int serverId) {
        this.serverId = serverId;
    }

    /**
     * Simulates collecting performance metrics for a server.
     * In a real system, these values would be read from system APIs.
     *
     * @return An array of integers representing [cpuUsage, memoryUsage, currentConnections].
     */
    private int[] collectMetrics() {
        // Simulate real-time data for demonstration.
        int cpuUsage = random.nextInt(101); // 0-100%
        int memoryUsage = random.nextInt(101); // 0-100%
        int currentConnections = random.nextInt(500); // 0-500 connections

        System.out.printf("✅ PerformanceMonitor: Collected metrics for Server %d - CPU: %d%%, Memory: %d%%, Connections: %d\n",
                serverId, cpuUsage, memoryUsage, currentConnections);

        return new int[]{cpuUsage, memoryUsage, currentConnections};
    }

    /**
     * Inserts or updates the performance metrics for the server in the database.
     * It uses a single SQL statement to handle both cases efficiently.
     */
    public void updatePerformanceMetrics() {
        // Collect the simulated metrics
        int[] metrics = collectMetrics();
        int cpuUsage = metrics[0];
        int memoryUsage = metrics[1];
        int currentConnections = metrics[2];

        // SQL statement to insert new data or update existing data for a server.
        // This is a powerful feature in MySQL that prevents duplicates.
        String sql = "INSERT INTO performance_metrics (server_id, cpu_usage, memory_usage, current_connections) " +
                     "VALUES (?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE " +
                     "cpu_usage = VALUES(cpu_usage), " +
                     "memory_usage = VALUES(memory_usage), " +
                     "current_connections = VALUES(current_connections)";

        try (Connection conn = DatabaseConnection.getConnection(dbType);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Set the parameters for the prepared statement.
            pstmt.setInt(1, this.serverId);
            pstmt.setInt(2, cpuUsage);
            pstmt.setInt(3, memoryUsage);
            pstmt.setInt(4, currentConnections);

            // Execute the update.
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.printf("✅ PerformanceMonitor: Successfully updated metrics for Server %d.\n", this.serverId);
            } else {
                System.out.printf("❌ PerformanceMonitor: Failed to update metrics for Server %d.\n", this.serverId);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error updating performance metrics: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
