/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.student.cloudjavafx.loadbalancing;

import com.student.cloudjavafx.auth.SessionManager;
import com.student.cloudjavafx.utils.DatabaseConnection;
import com.student.cloudjavafx.utils.DatabaseConnection.DatabaseType;
import com.student.cloudjavafx.utils.LogManager;
import com.student.cloudjavafx.utils.MySqlConnect;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Manages the distribution of files/chunks across available file servers.
 * This service determines which server should be used for storage based on
 * available algorithms (e.g., Round Robin, Least Connections, Random).
 */
public class LoadBalancerService {

    private static int lastUsedServerIndex = 0; // For Round Robin
    private final Random random = new Random();

    /**
     * Selects an appropriate file server based on the configured load balancing algorithm.
     *
     * @return The ID of the selected server, or -1 if no active server is found.
     */
    public int selectServer() {
        // In a real system, the load balancing algorithm could be configured in the database or config file.
        // For simplicity, let's assume a default "Round Robin" for now, but we can switch.
        String loadAlgorithm = "Round Robin"; // Could be dynamically changed
        
        List<Integer> activeServerIds = getActiveServerIds();
        if (activeServerIds.isEmpty()) {
            System.err.println("❌ No active file servers available.");
            LogManager.logLoadBalancerAction("SERVER_SELECTION_FAILED", "No active servers found.");
            return -1;
        }

        switch (loadAlgorithm) {
            case "Round Robin":
                return selectRoundRobinServer(activeServerIds);
            case "Least Connections":
                return selectLeastConnectionsServer(activeServerIds);
            case "Random":
                return selectRandomServer(activeServerIds);
            default:
                System.err.println("⚠️ Unknown load balancing algorithm. Falling back to Round Robin.");
                return selectRoundRobinServer(activeServerIds);
        }
    }

    /**
     * Retrieves a list of active server IDs from the database.
     * This simulates checking the status of the file servers.
     *
     * @return A list of active server IDs.
     */
     DatabaseType dbType = SessionManager.getCurrentDatabaseType();

    private List<Integer> getActiveServerIds() {
        List<Integer> serverIds = new ArrayList<>();
        String sql = "SELECT server_id FROM servers WHERE status = 'active'";
        try (Connection conn = DatabaseConnection.getConnection(dbType);
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                serverIds.add(rs.getInt("server_id"));
            }
        } catch (SQLException e) {
            System.err.println("❌ Error fetching active server IDs: " + e.getMessage());
            LogManager.logLoadBalancerAction("DB_ERROR", "Failed to fetch active servers.");
        }
        return serverIds;
    }

    /**
     * Selects a server using the Round Robin algorithm.
     *
     * @param activeServerIds A list of currently active server IDs.
     * @return The selected server ID.
     */
    private int selectRoundRobinServer(List<Integer> activeServerIds) {
        int selectedServerId = activeServerIds.get(lastUsedServerIndex);
        lastUsedServerIndex = (lastUsedServerIndex + 1) % activeServerIds.size();
        System.out.println("✅ LoadBalancerService: Selected server " + selectedServerId + " using Round Robin algorithm.");
        LogManager.logLoadBalancerAction("SERVER_SELECTION", "Selected server " + selectedServerId + " using Round Robin algorithm.");
        return selectedServerId;
    }
    
    /**
     * Selects a server using the Random algorithm.
     *
     * @param activeServerIds A list of currently active server IDs.
     * @return The selected server ID.
     */
    private int selectRandomServer(List<Integer> activeServerIds) {
        int randomIndex = random.nextInt(activeServerIds.size());
        int selectedServerId = activeServerIds.get(randomIndex);
        System.out.println("✅ LoadBalancerService: Selected server " + selectedServerId + " using Random algorithm.");
        LogManager.logLoadBalancerAction("SERVER_SELECTION", "Selected server " + selectedServerId + " using Random algorithm.");
        return selectedServerId;
    }

    /**
     * Placeholder for selecting a server using the Least Connections algorithm.
     * This would require querying the `performance_metrics` table.
     *
     * @param activeServerIds A list of currently active server IDs.
     * @return The selected server ID.
     */
    private int selectLeastConnectionsServer(List<Integer> activeServerIds) {
        int leastConnectionsServerId = -1;
        int minConnections = Integer.MAX_VALUE;

        // In a real implementation, query performance_metrics for connection counts
        String sql = "SELECT server_id, current_connections FROM performance_metrics WHERE server_id IN (?) ORDER BY current_connections ASC LIMIT 1";
        
        // This is a simulation, so we'll just pick the first one as a placeholder
        // A real implementation would dynamically build the 'IN' clause
        if (!activeServerIds.isEmpty()) {
            leastConnectionsServerId = activeServerIds.get(0);
        }

        System.out.println("⚠️ LoadBalancerService: Simulating Least Connections, selected server: " + leastConnectionsServerId);
        LogManager.logLoadBalancerAction("SERVER_SELECTION", "Simulated Least Connections, selected server " + leastConnectionsServerId + ".");
        return leastConnectionsServerId;
    }
}
