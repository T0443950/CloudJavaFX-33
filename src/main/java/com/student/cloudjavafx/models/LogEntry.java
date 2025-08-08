package com.student.cloudjavafx.models;

import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;

/**
 * A data model class to represent a single log entry.
 * This class is used to hold log data fetched from the database
 * and is bound to the TableView in LogsController.
 */
public class LogEntry {
    private final int logId;
    private final int userId;
    private final String username;
    private final String action;
    private final String logLevel; // Added to match the database table
    private final String details;
    private final Timestamp timestamp;

    /**
     * Constructor for the LogEntry data model.
     * * @param logId The log entry's unique ID.
     * @param userId The ID of the user associated with the log.
     * @param username The username associated with the log.
     * @param action The type of event.
     * @param logLevel The log level (e.g., INFO, WARNING, ERROR).
     * @param details The detailed description of the event.
     * @param timestamp The timestamp of the event.
     */
    public LogEntry(int logId, int userId, String username, String action, String logLevel, String details, Timestamp timestamp) {
        this.logId = logId;
        this.userId = userId;
        this.username = username;
        this.action = action;
        this.logLevel = logLevel;
        this.details = details;
        this.timestamp = timestamp;
    }

    // Getters for TableView PropertyValueFactory
    
    public int getLogId() {
        return logId;
    }

    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getAction() {
        return action;
    }
    
    // Added getter for logLevel to match the new field
    public String getLogLevel() {
        return logLevel;
    }

    public String getDetails() {
        return details;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    /**
     * Returns a formatted string of the timestamp for display in the TableView.
     * This method is crucial for the PropertyValueFactory to work correctly.
     * * @return Formatted timestamp string (yyyy-MM-dd HH:mm:ss).
     */
    public String getTimestampFormatted() {
        if (timestamp == null) {
            return "";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return timestamp.toLocalDateTime().format(formatter);
    }
}