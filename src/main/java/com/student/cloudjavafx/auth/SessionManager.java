package com.student.cloudjavafx.auth;

import com.student.cloudjavafx.models.User;
import com.student.cloudjavafx.models.UserSession;
import com.student.cloudjavafx.utils.DatabaseConnection;
import com.student.cloudjavafx.utils.LogManager;
import com.student.cloudjavafx.utils.DatabaseConnection.DatabaseType; // Import the DatabaseType enum
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID; // For generating session IDs
import java.util.logging.Level;
import java.util.logging.Logger;

public class SessionManager {
    private static User currentUser;
    private static int currentUserId;
    private static String currentUsername;
    private static String currentRole;
    private static LocalDateTime loginTime;
    private static String currentSessionId; // To store the unique session ID
    private static String currentIpAddress; // To store the IP address for the session
    // Add a field to store the current database type
    private static DatabaseType currentDatabaseType;

    private static final Logger LOGGER = Logger.getLogger(SessionManager.class.getName());

    /**
     * Creates a new user session, stores it in memory, and persists it to the
     * database.
     *
     * @param userId    The ID of the logged-in user.
     * @param username  The username of the logged-in user.
     * @param role      The role of the logged-in user (e.g., "admin", "standard").
     * @param ipAddress The IP address from which the user logged in.
     * @param dbType    The database type (MySQL or SQLite) to use for the connection.
     */
    public static void createSession(int userId, String username, String role, String ipAddress, DatabaseType dbType) {
        currentUserId = userId;
        currentUsername = username;
        currentRole = role;
        loginTime = LocalDateTime.now();
        currentSessionId = UUID.randomUUID().toString(); // Generate a unique session ID
        currentIpAddress = ipAddress;
        currentDatabaseType = dbType; // Store the database type for future use in this session

        // Log the login action
        LogManager.logLogin(username);

        // Persist session to database, including the new columns
        String sql = "INSERT INTO user_sessions (session_id, user_id, login_time, last_activity, ip_address, dbType, expires_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection(currentDatabaseType);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, currentSessionId);
            pstmt.setInt(2, currentUserId);
            pstmt.setTimestamp(3, java.sql.Timestamp.valueOf(loginTime));
            pstmt.setTimestamp(4, java.sql.Timestamp.valueOf(LocalDateTime.now())); // Initial last_activity
            pstmt.setString(5, currentIpAddress);
            pstmt.setString(6, dbType.name()); // Set the new dbType column
            // Set the session expiration time to 24 hours from now
            LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);
            pstmt.setTimestamp(7, java.sql.Timestamp.valueOf(expiresAt));
            pstmt.executeUpdate();
            LOGGER.log(Level.INFO, "Session created and persisted for user: {0}", username);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating session in database for user: " + username, e);
        }
    }

    /**
     * Clears the current user session from memory and deletes it from the database.
     */
    public static void clearSession() {
        if (isLoggedIn()) {
            // Log the logout action before clearing the username
            LogManager.logLogout(currentUsername);

            // Delete session from database
            if (currentSessionId != null && currentDatabaseType != null) {
                String sql = "DELETE FROM user_sessions WHERE session_id = ?";
                try (Connection conn = DatabaseConnection.getConnection(currentDatabaseType);
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, currentSessionId);
                    pstmt.executeUpdate();
                    LOGGER.log(Level.INFO, "Session {0} deleted from database.", currentSessionId);
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Error deleting session " + currentSessionId + " from database.", e);
                }
            }
        }
        // Clear in-memory session data
        currentUserId = 0;
        currentUsername = null;
        currentRole = null;
        loginTime = null;
        currentSessionId = null;
        currentIpAddress = null;
        currentDatabaseType = null;
    }

    /**
     * Updates the last activity timestamp for the current session in the database.
     * This should be called periodically or on significant user actions.
     */
    public static void updateLastActivity() {
        if (currentSessionId != null && currentDatabaseType != null) {
            String sql = "UPDATE user_sessions SET last_activity = ? WHERE session_id = ?";
            try (Connection conn = DatabaseConnection.getConnection(currentDatabaseType);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setTimestamp(1, java.sql.Timestamp.valueOf(LocalDateTime.now()));
                pstmt.setString(2, currentSessionId);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error updating last activity for session " + currentSessionId, e);
            }
        }
    }

    /**
     * Checks if a user is currently logged in.
     *
     * @return true if a user is logged in, false otherwise.
     */
    public static boolean isLoggedIn() {
        return currentUsername != null && !currentUsername.isEmpty();
    }

    // Consistent naming with "Current" prefix
    public static String getCurrentUsername() {
        return currentUsername;
    }

    public static String getCurrentRole() {
        return currentRole;
    }

    public static int getCurrentUserId() {
        return currentUserId;
    }

    public static LocalDateTime getLoginTime() {
        return loginTime;
    }

    public static String getCurrentSessionId() {
        return currentSessionId;
    }

    public static String getCurrentIpAddress() {
        return currentIpAddress;
    }
    
    // Add a getter for the current database type
    public static DatabaseType getCurrentDatabaseType() {
        return currentDatabaseType;
    }

    public static void logout() {
        clearSession();
    }
    
    public static void logout(DatabaseType dbType) {
        clearSession();
    }

    public static boolean isAdmin() {
        // Assuming a role check, e.g., if the user's role is "admin"
        return isLoggedIn() && "admin".equalsIgnoreCase(currentRole);
    }


    /**
     * Retrieves all active user sessions from the database.
     * @return A List of UserSession objects.
     * @param dbType The database type (MySQL or SQLite) to use for the connection.
     */
    public static List<UserSession> getAllActiveSessions(DatabaseType dbType) {
        List<UserSession> sessions = new ArrayList<>();
        String sql = "SELECT us.session_id, u.username, us.login_time, us.last_activity, us.ip_address, u.id FROM user_sessions us JOIN users u ON us.user_id = u.id";
        try (Connection conn = DatabaseConnection.getConnection(dbType);
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                sessions.add(new UserSession(
                    rs.getString("session_id"),
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getTimestamp("login_time").toLocalDateTime(),
                    rs.getTimestamp("last_activity").toLocalDateTime(),
                    rs.getString("ip_address")
                ));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving all active sessions.", e);
        }
        return sessions;
    }

    /**
     * Ends a specific user session by its ID.
     * @param sessionId The ID of the session to end.
     * @return true if the session was successfully ended, false otherwise.
     * @param dbType The database type (MySQL or SQLite) to use for the connection.
     */
    public static boolean endSession(String sessionId, DatabaseType dbType) {
        String sql = "DELETE FROM user_sessions WHERE session_id = ?";
        try (Connection conn = DatabaseConnection.getConnection(dbType);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, sessionId);
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                LOGGER.log(Level.INFO, "Session {0} was ended by an administrator.", sessionId);
                return true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error ending session " + sessionId + " from database.", e);
        }
        return false;
    }
}
