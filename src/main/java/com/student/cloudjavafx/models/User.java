package com.student.cloudjavafx.models;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.student.cloudjavafx.auth.SessionManager;
import com.student.cloudjavafx.utils.DatabaseConnection;
import com.student.cloudjavafx.utils.DatabaseConnection.DatabaseType;

/**
 * A comprehensive model class to represent a user.
 * This version uses JavaFX properties for better integration with TableView.
 */
public class User {

    private final SimpleIntegerProperty id;
    private final StringProperty username;
    private final StringProperty fullName;
    private final StringProperty email;
    private final StringProperty role;
    private final StringProperty createdAt;
    private final StringProperty last_login;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Constructor to create a new User object with full details.
     * @param id The unique user ID.
     * @param username The user's username.
     * @param fullName The user's full name.
     * @param email The user's email address.
     * @param role The user's role (e.g., "admin", "user").
     * @param createdAt The date and time the user account was created.
     * @param last_login The date and time of the user's last login.
     */
    public User(int id, String username, String fullName, String email, String role,
                LocalDateTime createdAt, LocalDateTime last_login) {
        this.id = new SimpleIntegerProperty(id);
        this.username = new SimpleStringProperty(username);
        this.fullName = new SimpleStringProperty(fullName);
        this.email = new SimpleStringProperty(email);
        this.role = new SimpleStringProperty(role);
        this.createdAt = new SimpleStringProperty(formatDateTime(createdAt));
        this.last_login = new SimpleStringProperty(formatDateTime(last_login));
    }

    /**
     * Helper method to format LocalDateTime into a readable string.
     * @param dateTime The LocalDateTime object to format.
     * @return A formatted string or "N/A" if the date is null.
     */
    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(formatter) : "N/A";
    }

    // --- Getters for JavaFX Properties (for TableView cell value factories) ---
    public StringProperty usernameProperty() { return username; }
    public StringProperty fullNameProperty() { return fullName; }
    public StringProperty emailProperty() { return email; }
    public StringProperty roleProperty() { return role; }
    public StringProperty createdAtProperty() { return createdAt; }
    public StringProperty last_loginProperty() { return last_login; }

    // --- Standard Getters ---
    public int getId() { return id.get(); }
    public String getUsername() { return username.get(); }
    public String getFullName() { return fullName.get(); }
    public String getEmail() { return email.get(); }
    public String getRole() { return role.get(); }
    public String getCreatedAt() { return createdAt.get(); }
    public String getLast_login() { return last_login.get(); }


    /**
     * Static method to retrieve a user's full name from the database.
     * @param ownerId The ID of the user.
     * @return A StringProperty containing the user's full name or "Unknown" if not found.
     */
    public static StringProperty getFullNameOwner(int ownerId) {
        String fullName = "Unknown"; // Default value in case of error or not found
        String sql = "SELECT fullname FROM users WHERE id = ?";
        // Retrieve the database type at the time of the method call for accuracy.
        DatabaseType dbType = SessionManager.getCurrentDatabaseType();

        try (Connection conn = DatabaseConnection.getConnection(dbType);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, ownerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    fullName = rs.getString("fullname");
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Error retrieving user's full name for ID: " + ownerId);
            e.printStackTrace();
        }

        return new SimpleStringProperty(fullName);
    }

    // --- Additional methods to retrieve dates as LocalDateTime objects ---
    public LocalDateTime getCreatedAtAsDateTime() {
        return createdAt.get().equals("N/A") ? null :
                LocalDateTime.parse(createdAt.get(), formatter);
    }

    public LocalDateTime getLastLoginAsDateTime() {
        return last_login.get().equals("N/A") ? null :
                LocalDateTime.parse(last_login.get(), formatter);
    }

    @Override
    public String toString() {
        return getUsername();
    }
}
