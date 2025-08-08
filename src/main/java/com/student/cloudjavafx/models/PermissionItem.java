package com.student.cloudjavafx.models;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Button;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a file permission item for display in the TableView.
 * This class provides properties that can be directly mapped to TableColumn cell value factories.
 */
public class PermissionItem {
    private final int permissionId;
    private final int fileId;
    private final StringProperty fileName;
    private final int userId;
    private final StringProperty userName;
    private final StringProperty permissionType;
    private final LocalDateTime grantedAt;
    private final StringProperty grantedAtFormatted;
    private final ObjectProperty<Button> actionButton; // For a "Remove" button in the table

    public PermissionItem(int permissionId, int fileId, String fileName, int userId, String userName, String permissionType, Timestamp grantedAt) {
        this.permissionId = permissionId;
        this.fileId = fileId;
        this.fileName = new SimpleStringProperty(fileName);
        this.userId = userId;
        this.userName = new SimpleStringProperty(userName);
        this.permissionType = new SimpleStringProperty(permissionType);
        this.grantedAt = grantedAt.toLocalDateTime();
        this.grantedAtFormatted = new SimpleStringProperty(formatDateTime(this.grantedAt));
        
        // This button is for a hypothetical action, for example, revoking the permission
        Button removeButton = new Button("Revoke");
        // You would set an event handler here to call a method in the controller to handle the action
        this.actionButton = new SimpleObjectProperty<>(removeButton);
    }

    // --- Properties for TableView display ---
    public String getFileName() { return fileName.get(); }
    public StringProperty fileNameProperty() { return fileName; }

    public String getUserName() { return userName.get(); }
    public StringProperty userNameProperty() { return userName; }

    public String getPermissionType() { return permissionType.get(); }
    public StringProperty permissionTypeProperty() { return permissionType; }

    public String getGrantedAtFormatted() { return grantedAtFormatted.get(); }
    public StringProperty grantedAtFormattedProperty() { return grantedAtFormatted; }

    public Button getActionButton() { return actionButton.get(); }
    public ObjectProperty<Button> actionButtonProperty() { return actionButton; }

    // --- Other Getters (not necessarily for TableView display) ---
    public int getPermissionId() { return permissionId; }
    public int getFileId() { return fileId; }
    public int getUserId() { return userId; }
    public LocalDateTime getGrantedAt() { return grantedAt; }

    /**
     * Helper method to format LocalDateTime into a readable date string.
     * @param dateTime The LocalDateTime object.
     * @return Formatted date string.
     */
    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return dateTime.format(formatter);
    }


@Override
public String toString() {
    // It's most useful to display the file name associated with the permission.
    // If you want to show more info, you could do something like:
    // return getFileName() + " (" + getUserName() + ": " + getPermissionType() + ")";
    return getFileName();
}
}
