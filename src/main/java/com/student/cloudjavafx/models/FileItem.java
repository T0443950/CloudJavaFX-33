package com.student.cloudjavafx.models;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.student.cloudjavafx.models.User;

/**
 * Represents a file item for display in the TableView.
 * This class provides properties that can be directly mapped to TableColumn cell value factories.
 */
public class FileItem {
    private final int fileId;
    private final StringProperty originalName;
    private final StringProperty storagePath;
    private final long size; // Size in bytes
    private final StringProperty sizeFormatted; // Formatted size (e.g., 1.2 MB)
    private final int ownerId;
    private final StringProperty owner; // Owner's username
    private final StringProperty fileType;
    private final LocalDateTime createdAt;
    private final StringProperty createdAtFormatted; // Formatted creation date
    private final boolean isEncrypted;
    private final StringProperty permissions; // User's permissions for this file

    public FileItem(int fileId, String originalName, String storagePath, long size, int ownerId, String owner,
                    String fileType, Timestamp createdAt, boolean isEncrypted, String permissions) {
        this.fileId = fileId;
        this.originalName = new SimpleStringProperty(originalName);
        this.storagePath = new SimpleStringProperty(storagePath);
        this.size = size;
        this.sizeFormatted = new SimpleStringProperty(formatFileSize(size));
        this.ownerId = ownerId;
        this.owner = new SimpleStringProperty(owner);
        this.fileType = new SimpleStringProperty(fileType);
        this.createdAt = createdAt.toLocalDateTime();
        this.createdAtFormatted = new SimpleStringProperty(formatDateTime(this.createdAt));
        this.isEncrypted = isEncrypted;
        this.permissions = new SimpleStringProperty(permissions);
    }

    // --- Properties for TableView ---
    public StringProperty originalNameProperty() {
        return originalName;
    }

    public StringProperty sizeFormattedProperty() {
        return sizeFormatted;
    }

    public StringProperty ownerProperty() {
        return owner;
    }

    public StringProperty fileTypeProperty() {
        return fileType;
    }

    public StringProperty createdAtFormattedProperty() {
        return createdAtFormatted;
    }

    public StringProperty permissionsProperty() {
        return permissions;
    }

    // --- Other Getters (not necessarily for TableView display) ---
    public int getFileId() {
        return fileId;
    }

    public String getStoragePath() {
        return storagePath.get();
    }

    public long getSize() {
        return size;
    }

    public int getOwnerId() {

        return ownerId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

      public String getOriginalName() {
        return originalName.get();
    }

      public String getOwner() {
         return User.getFullNameOwner(ownerId).get();
    }

    public boolean isEncrypted() {
        return isEncrypted;
    }

    /**
     * Helper method to format file size into a human-readable string.
     * @param bytes The file size in bytes.
     * @return Formatted size string.
     */
    private String formatFileSize(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(bytes / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

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
}
