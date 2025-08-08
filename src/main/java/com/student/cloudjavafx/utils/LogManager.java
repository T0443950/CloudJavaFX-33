package com.student.cloudjavafx.utils;

import com.student.cloudjavafx.auth.SessionManager;
import com.student.cloudjavafx.utils.DatabaseConnection.DatabaseType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Manages logging of various system actions to the database.
 * This class provides static methods for logging different event types,
 * associating them with users and providing details. It also serves as a
 * data model for log entries to be displayed in a TableView.
 */
public class LogManager {

    // --- Log Levels ---
    public static final String INFO = "INFO";
    public static final String WARNING = "WARNING";
    public static final String ERROR = "ERROR";

    // Log Event Types
    public static final String LOGIN = "LOGIN";
    public static final String LOGOUT = "LOGOUT";
    public static final String USER_REGISTER = "USER_REGISTER";
    public static final String UPDATE_USER = "UPDATE_USER";
    public static final String DELETE_USER = "DELETE_USER";
    public static final String FILE_UPLOAD = "FILE_UPLOAD";
    public static final String FILE_DOWNLOAD = "FILE_DOWNLOAD";
    public static final String FILE_DELETE = "FILE_DELETE";
    public static final String FILE_SHARE = "FILE_SHARE";
    public static final String SERVER_ACTION = "SERVER_ACTION";
    public static final String SERVER_ERROR = "SERVER_ERROR";
    public static final String LOAD_BALANCER_ACTION = "LOAD_BALANCER_ACTION";
    public static final String LOAD_BALANCER_ERROR = "LOAD_BALANCER_ERROR";

    // Additional constants needed for FileController
    public static final String NAVIGATE_BACK = "NAVIGATE_BACK";
    public static final String FILE_BROWSE = "FILE_BROWSE";
    public static final String FILE_BROWSE_CANCEL = "FILE_BROWSE_CANCEL";
    public static final String FILE_UPLOAD_INIT = "FILE_UPLOAD_INIT";
    public static final String FILE_UPLOAD_FAILED = "FILE_UPLOAD_FAILED";
    public static final String FILE_UPLOAD_ERROR = "FILE_UPLOAD_ERROR";
    public static final String FILE_UPLOAD_CHUNK_FAILED = "FILE_UPLOAD_CHUNK_FAILED";
    public static final String FILE_DOWNLOAD_INIT = "FILE_DOWNLOAD_INIT";
    public static final String FILE_DOWNLOAD_FAILED = "FILE_DOWNLOAD_FAILED";
    public static final String FILE_DOWNLOAD_ERROR = "FILE_DOWNLOAD_ERROR";
    public static final String FILE_DOWNLOAD_CANCEL = "FILE_DOWNLOAD_CANCEL";
    public static final String FILE_DOWNLOAD_CHUNK_FAILED = "FILE_DOWNLOAD_CHUNK_FAILED";
    public static final String FILE_DOWNLOAD_CHUNK_CORRUPTED = "FILE_DOWNLOAD_CHUNK_CORRUPTED";
    public static final String FILE_DELETE_INIT = "FILE_DELETE_INIT";
    public static final String FILE_DELETE_FAILED = "FILE_DELETE_FAILED";
    public static final String FILE_DELETE_ERROR = "FILE_DELETE_ERROR";
    public static final String FILE_DELETE_CANCEL = "FILE_DELETE_CANCEL";
    public static final String FILE_DELETE_CHUNK_FAILED = "FILE_DELETE_CHUNK_FAILED";
    public static final String FILE_SHARE_INIT = "FILE_SHARE_INIT";
    public static final String FILE_SHARE_INFO = "FILE_SHARE_INFO";
    public static final String FILE_REFRESH = "FILE_REFRESH";
    public static final String FILE_SEARCH = "FILE_SEARCH";
    public static final String FILE_SEARCH_CLEAR = "FILE_SEARCH_CLEAR";
    public static final String DATABASE_ACTION = "DATABASE_ACTION";
    public static final String DATABASE_ERROR = "DATABASE_ERROR";
    public static final String SYSTEM_ERROR = "SYSTEM_ERROR";
    public static final String CHUNK_STORED = "CHUNK_STORED";
    public static final String UPLOAD_FAILED = "UPLOAD_FAILED";
    public static final String UPLOAD_ERROR = "UPLOAD_ERROR";
    public static final String UPLOAD_ROLLBACK_FAILED = "UPLOAD_ROLLBACK_FAILED";
    public static final String FILE_UPLOAD_START = "FILE_UPLOAD_START";
    public static final String FILE_DOWNLOAD_START = "FILE_DOWNLOAD_START";
    public static final String FILE_METADATA_INSERT = "FILE_METADATA_INSERT";
    public static final String FILE_ENCRYPTION_KEY_UPDATE = "FILE_ENCRYPTION_KEY_UPDATE";
    public static final String CHUNK_METADATA_INSERT = "CHUNK_METADATA_INSERT";
    public static final String CHUNK_METADATA_DELETE = "CHUNK_METADATA_DELETE";
    public static final String FILE_METADATA_DELETE = "FILE_METADATA_DELETE";
    public static final String DOWNLOAD_COUNT_INCREMENT = "DOWNLOAD_COUNT_INCREMENT";
    public static final String FILE_SHARE_ERROR = "FILE_SHARE_ERROR";
    public static final String SYSTEM_INFO = "SYSTEM_INFO";
    public static final String SYSTEM_WARNING = "SYSTEM_WARNING";
    public static final String USER_LOGIN_SUCCESS = "USER_LOGIN_SUCCESS";
    public static final String USER_LOGIN_FAILURE = "USER_LOGIN_FAILURE";


    // Instance fields for the log entry data model
    private int logId;
    private int userId;
    private String username;
    private String action; // Changed field name to match table
    private String logLevel; // Changed field name to match table
    private String details; // Changed field name to match table
    private Timestamp timestamp;

    /**
     * Constructor for the data model.
     * @param logId The log entry's unique ID.
     * @param userId The ID of the user associated with the log.
     * @param username The username associated with the log.
     * @param action The type of action.
     * @param logLevel The level of the log (e.g. INFO, ERROR).
     * @param details The details of the event.
     * @param timestamp The timestamp of the event.
     */
    public LogManager(int logId, int userId, String username, String action, String logLevel, String details, Timestamp timestamp) {
        this.logId = logId;
        this.userId = userId;
        this.username = username;
        this.action = action;
        this.logLevel = logLevel;
        this.details = details;
        this.timestamp = timestamp;
    }
    
    // --- Getters and Setters for the Log Data Model ---
    public int getLogId() {
        return logId;
    }
    
    public void setLogId(int logId) {
        this.logId = logId;
    }

    public int getUserId() {
        return userId;
    }
    
    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }

    // Getters and setters for the new fields
    public String getAction() {
        return action;
    }
    
    public void setAction(String action) {
        this.action = action;
    }
    
    public String getLogLevel() {
        return logLevel;
    }
    
    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }
    
    public String getDetails() {
        return details;
    }
    
    public void setDetails(String details) {
        this.details = details;
    }
    
    public Timestamp getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getTimestampFormatted() {
        if (timestamp == null) {
            return "";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return timestamp.toLocalDateTime().format(formatter);
    }
    
    // --- Static Logging Methods ---

    /**
     * Inserts a log entry into the `logs` table in the database.
     * This is a generic method used by all other specific logging methods.
     * It handles both log levels and action types.
     *
     * @param level       The log level (e.g., INFO, WARNING, ERROR).
     * @param action      The type of action being logged (e.g., USER_LOGIN_SUCCESS).
     * @param details     The detailed log message.
     * @param userId      The ID of the user associated with the action, or -1 if not applicable.
     * @param username    The username associated with the action, or null if not applicable.
     */
    private static void logAction(String level, String action, String details, int userId, String username) {
        DatabaseType dbType = SessionManager.getCurrentDatabaseType();
        String sql = "INSERT INTO logs (user_id, username, action, log_level, details) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection(dbType);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (userId > 0) {
                pstmt.setInt(1, userId);
            } else {
                pstmt.setNull(1, java.sql.Types.INTEGER);
            }
            pstmt.setString(2, username);
            pstmt.setString(3, action);
            pstmt.setString(4, level);
            pstmt.setString(5, details);

            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Failed to log to database: " + e.getMessage());
        }
    }

    
    public static void logAction(String action, String details) {
        // This method does not take a log level, so we assume INFO.
        logAction(INFO, action, details, SessionManager.getCurrentUserId(), SessionManager.getCurrentUsername());
    }

    /**
     * Logs an informational message with a SYSTEM_INFO action type.
     *
     * @param message The message to log.
     */
    public static void logInfo(String message) {
        logAction(INFO, SYSTEM_INFO, message, SessionManager.getCurrentUserId(), SessionManager.getCurrentUsername());
    }

    /**
     * Logs a warning message with a SYSTEM_WARNING action type.
     *
     * @param message The message to log.
     */
    public static void logWarning(String message) {
        logAction(WARNING, SYSTEM_WARNING, message, SessionManager.getCurrentUserId(), SessionManager.getCurrentUsername());
    }

    /**
     * Logs an error message with a SYSTEM_ERROR action type.
     *
     * @param message The message to log.
     */
    public static void logError(String message) {
        logAction(ERROR, SYSTEM_ERROR, message, SessionManager.getCurrentUserId(), SessionManager.getCurrentUsername());
    }

    // --- Specific Logging Methods for User Actions ---
    public static void logLogin(String username) {
        logAction(INFO, LOGIN, "User '" + username + "' logged in.", SessionManager.getCurrentUserId(), username);
    }

    public static void logLogout(String username) {
        logAction(INFO, LOGOUT, "User '" + username + "' logged out.", SessionManager.getCurrentUserId(), username);
    }

    public static void logUserRegistration(String username) {
        logAction(INFO, USER_REGISTER, "New user '" + username + "' registered.", SessionManager.getCurrentUserId(), username);
    }

    public static void logUserUpdate(String username) {
        logAction(INFO, UPDATE_USER, "User '" + username + "' updated.", SessionManager.getCurrentUserId(), username);
    }

    public static void logUserDelete(String username) {
        logAction(WARNING, DELETE_USER, "User '" + username + "' deleted.", SessionManager.getCurrentUserId(), username);
    }

    // --- Specific Logging Methods for File Actions ---
    public static void logFileUpload(String fileName) {
        logAction(INFO, FILE_UPLOAD, "File '" + fileName + "' uploaded.", SessionManager.getCurrentUserId(), SessionManager.getCurrentUsername());
    }

    public static void logFileDownload(String fileName) {
        logAction(INFO, FILE_DOWNLOAD, "File '" + fileName + "' downloaded.", SessionManager.getCurrentUserId(), SessionManager.getCurrentUsername());
    }

    public static void logFileDelete(String fileName) {
        logAction(WARNING, FILE_DELETE, "File '" + fileName + "' deleted.", SessionManager.getCurrentUserId(), SessionManager.getCurrentUsername());
    }

    public static void logFileShare(String fileName, String sharedWithUsername, String permissionType) {
        logAction(INFO, FILE_SHARE, "File '" + fileName + "' shared with '" + sharedWithUsername + "' with '" + permissionType
                + "' permission.", SessionManager.getCurrentUserId(), SessionManager.getCurrentUsername());
    }

    public static void logFileShareError(String details) {
        logAction(ERROR, FILE_SHARE_ERROR, details, SessionManager.getCurrentUserId(), SessionManager.getCurrentUsername());
    }

    // --- Specific Logging Methods for Server & Load Balancer Actions ---
    public static void logServerAction(int serverId, String actionType, String details) {
        logAction(INFO, SERVER_ACTION, "Server " + serverId + " - " + actionType + ": " + details, -1, null);
    }

    public static void logServerError(int serverId, String errorType, String details) {
        logAction(ERROR, SERVER_ERROR, "Server " + serverId + " - Error: " + errorType + " - " + details, -1, null);
    }

    public static void logLoadBalancerAction(String actionType, String details) {
        logAction(INFO, LOAD_BALANCER_ACTION, "Load Balancer - " + actionType + ": " + details, -1, null);
    }

    public static void logLoadBalancerError(String errorType, String details) {
        logAction(ERROR, LOAD_BALANCER_ERROR, "Load Balancer - Error: " + errorType + " - " + details, -1, null);
    }
}