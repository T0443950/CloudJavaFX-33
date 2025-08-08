package com.student.cloudjavafx.models;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Button;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents an active user session for display in the TableView.
 */
public class UserSession {
    private final StringProperty session_id;
    private final SimpleIntegerProperty userId;
    private final StringProperty username;
    private final LocalDateTime loginTime;
    private final StringProperty loginTimeFormatted;
    private final LocalDateTime lastActivity;
    private final StringProperty lastActivityFormatted;
    private final StringProperty ipAddress;
    private final ObjectProperty<Button> actionButton; // For "End Session" button

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public UserSession(String session_id, int userId, String username, LocalDateTime loginTime, LocalDateTime lastActivity, String ipAddress) {
        this.session_id = new SimpleStringProperty(session_id);
        this.userId = new SimpleIntegerProperty(userId);
        this.username = new SimpleStringProperty(username);
        this.loginTime = loginTime;
        this.loginTimeFormatted = new SimpleStringProperty(loginTime != null ? loginTime.format(FORMATTER) : "N/A");
        this.lastActivity = lastActivity;
        this.lastActivityFormatted = new SimpleStringProperty(lastActivity != null ? lastActivity.format(FORMATTER) : "N/A");
        this.ipAddress = new SimpleStringProperty(ipAddress);

        // Initialize action button (e.g., "End Session")
        Button endButton = new Button("End");
        endButton.getStyleClass().add("action-button-danger"); // Apply CSS style
        // Action for this button will be set in the controller's cell factory
        this.actionButton = new SimpleObjectProperty<>(endButton);
    }

    // --- Getters for TableView PropertyValueFactory ---
    public String getsession_id() { return session_id.get(); }
    public StringProperty session_idProperty() { return session_id; }

    public String getUsername() { return username.get(); }
    public StringProperty usernameProperty() { return username; }

    public String getLoginTimeFormatted() { return loginTimeFormatted.get(); }
    public StringProperty loginTimeFormattedProperty() { return loginTimeFormatted; }

    public String getLastActivityFormatted() { return lastActivityFormatted.get(); }
    public StringProperty lastActivityFormattedProperty() { return lastActivityFormatted; }

    public String getIpAddress() { return ipAddress.get(); }
    public StringProperty ipAddressProperty() { return ipAddress; }

    public Button getActionButton() { return actionButton.get(); }
    public ObjectProperty<Button> actionButtonProperty() { return actionButton; }

    // --- Other Getters ---
    public int getUserId() { return userId.get(); }
    public LocalDateTime getLoginTime() { return loginTime; }
    public LocalDateTime getLastActivity() { return lastActivity; }

    @Override
    public String toString() {
        return "Session{" +
               "session_id='" + session_id.get() + '\'' +
               ", username='" + username.get() + '\'' +
               '}';
    }
}
