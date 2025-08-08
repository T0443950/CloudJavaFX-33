package com.student.cloudjavafx;

import com.student.cloudjavafx.auth.SessionManager;
import com.student.cloudjavafx.loadbalancing.LoadBalancerService;
import com.student.cloudjavafx.loadbalancing.FileServerClient;
import com.student.cloudjavafx.utils.DatabaseConnection;
import com.student.cloudjavafx.utils.DatabaseConnection.DatabaseType;
import com.student.cloudjavafx.utils.LogManager;
import com.student.cloudjavafx.utils.MySqlConnect;
import com.student.cloudjavafx.models.FileItem;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Timestamp;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID; // For generating unique keys

/**
 * Manages all file-related operations, including database interactions
 * and distribution of file chunks to file servers.
 * This version of the FileManager has been updated to include logging for key actions.
 */
public class FileManager {

    private final LoadBalancerService loadBalancerService;
    private static final int CHUNK_SIZE = 1024 * 1024; // 1MB

    public FileManager() {
        this.loadBalancerService = new LoadBalancerService();
    }
     DatabaseType dbType = SessionManager.getCurrentDatabaseType();


    /**
     * Uploads a file to the cloud storage system.
     * This method handles storing file metadata, splitting the file into chunks,
     * and distributing chunks to available file servers.
     *
     * @param originalName The original name of the file.
     * @param fileContent The input stream of the file content.
     * @param fileSize The size of the file in bytes.
     * @param fileType The MIME type of the file.
     * @return The ID of the uploaded file, or -1 if the upload fails.
     */
    public int uploadFile(String originalName, InputStream fileContent, long fileSize, String fileType) {
        int fileId = -1;
        String sql = "INSERT INTO files (original_name, storage_path, size, owner_id, file_type) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection(dbType);
             PreparedStatement pstmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            // Log the upload attempt
            // Assuming logFileUpload exists based on previous conversations.
            LogManager.logFileUpload(originalName);

            // 1. Store file metadata in the database
            String storagePath = UUID.randomUUID().toString(); // Generate a unique path
            int ownerId = SessionManager.getCurrentUserId();
            pstmt.setString(1, originalName);
            pstmt.setString(2, storagePath);
            pstmt.setLong(3, fileSize);
            pstmt.setInt(4, ownerId);
            pstmt.setString(5, fileType);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                System.err.println("❌ Creating file metadata failed, no rows affected.");
                // Assuming logLoadBalancerError exists based on previous conversations.
                LogManager.logLoadBalancerError("UPLOAD_FAILED", "Failed to create file metadata for " + originalName);
                return -1;
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    fileId = generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Creating file metadata failed, no ID obtained.");
                }
            }

            // 2. Split file into chunks and distribute to servers
            byte[] buffer = new byte[CHUNK_SIZE];
            int bytesRead;
            int chunkNumber = 0;
            while ((bytesRead = fileContent.read(buffer)) != -1) {
                ByteArrayInputStream chunkData = new ByteArrayInputStream(buffer, 0, bytesRead);
                int selectedServerId = loadBalancerService.selectServer();
                if (selectedServerId == -1) {
                    throw new IOException("No active servers available for chunk storage.");
                }

                // Simulate storing the chunk
                FileServerClient client = new FileServerClient(selectedServerId);
                FileChunk chunk = new FileChunk(fileId, chunkNumber, selectedServerId, "checksum_placeholder", chunkData);
                client.storeChunk(chunk);

                // Store chunk metadata in the database
                try {
                    storeChunkMetadata(fileId, chunkNumber, selectedServerId, "checksum_placeholder");
                } catch (SQLException e) {
                    System.err.println("❌ Database error during chunk metadata storage: " + e.getMessage());
                    // Rethrow the exception to be caught by the outer block for proper cleanup.
                    throw e;
                }

                chunkNumber++;
            }

        } catch (SQLException e) {
            System.err.println("❌ Database error during file upload: " + e.getMessage());
            // Assuming logLoadBalancerError exists based on previous conversations.
            LogManager.logLoadBalancerError("UPLOAD_FAILED", "Database error for file " + originalName + ": " + e.getMessage());
            // Clean up all related data on error
            if (fileId != -1) {
                cleanupFile(fileId);
            }
            fileId = -1;
        } catch (IOException e) {
            System.err.println("❌ I/O error during file upload: " + e.getMessage());
            // Assuming logLoadBalancerError exists based on previous conversations.
            LogManager.logLoadBalancerError("UPLOAD_FAILED", "I/O error for file " + originalName + ": " + e.getMessage());
            // Clean up all related data on error
            if (fileId != -1) {
                cleanupFile(fileId);
            }
            fileId = -1;
        }
        return fileId;
    }

    /**
     * Downloads a file from the cloud storage system.
     *
     * @param fileId The ID of the file to download.
     * @param destinationPath The path to save the downloaded file to.
     * @return An InputStream of the reconstructed file, or null if download fails.
     * @throws IOException If an I/O error occurs.
     */
    public InputStream downloadFile(int fileId, String destinationPath) throws IOException {
        // Log the download attempt
        // The original method `LogManager.logFileDownload` was not found.
        // Replaced with a System.out.println for compilation.
        System.out.println("Attempting to download file with ID: " + fileId);

        List<FileChunk> chunks = getFileChunks(fileId);
        if (chunks.isEmpty()) {
            System.err.println("❌ No chunks found for file ID: " + fileId);
            // Assuming logLoadBalancerError exists based on previous conversations.
            LogManager.logLoadBalancerError("DOWNLOAD_FAILED", "No chunks found for file ID: " + fileId);
            return null;
        }

        ByteArrayOutputStream reconstructedFile = new ByteArrayOutputStream();
        for (FileChunk chunk : chunks) {
            FileServerClient client = new FileServerClient(chunk.getServerId());
            InputStream chunkData = client.retrieveChunk(chunk.getFileId(), chunk.getChunkNumber());
            if (chunkData != null) {
                reconstructedFile.write(chunkData.readAllBytes());
            } else {
                System.err.println("❌ Failed to retrieve chunk " + chunk.getChunkNumber() + " from server " + chunk.getServerId());
                // Assuming logLoadBalancerError exists based on previous conversations.
                LogManager.logLoadBalancerError("DOWNLOAD_FAILED", "Failed to retrieve chunk " + chunk.getChunkNumber() + " for file " + fileId);
                return null;
            }
        }
        
        // The original method `LogManager.logFileDownloadSuccess` was not found.
        // Replaced with a System.out.println for compilation.
        System.out.println("Download successful for fileId: " + fileId);
        return new ByteArrayInputStream(reconstructedFile.toByteArray());
    }

    /**
     * Deletes a file and all its associated metadata and chunks.
     *
     * @param fileId The ID of the file to delete.
     * @return true if deletion is successful, false otherwise.
     */
    public boolean deleteFile(int fileId) {
        // Log the deletion attempt
        // The original method `LogManager.logFileDeletion` was not found.
        // Replaced with a System.out.println for compilation.
        System.out.println("Attempting to delete file with ID: " + fileId);
        
        try {
            // Get chunk metadata to know where to delete chunks from
            List<FileChunk> chunks = getFileChunks(fileId);
            
            // 1. Delete chunks from all file servers
            for (FileChunk chunk : chunks) {
                FileServerClient client = new FileServerClient(chunk.getServerId());
                client.deleteChunk(chunk.getFileId(), chunk.getChunkNumber());
            }

            // 2. Delete chunk metadata from the database
            deleteChunkMetadata(fileId);

            // 3. Delete file metadata from the database
            deleteFileMetadata(fileId);
            
            // The original method `LogManager.logFileDeletionSuccess` was not found.
            // Replaced with a System.out.println for compilation.
            System.out.println("File with ID: " + fileId + " deleted successfully.");
            return true;
        } catch (SQLException e) {
            System.err.println("❌ Database error during file deletion: " + e.getMessage());
            // Assuming logLoadBalancerError exists based on previous conversations.
            LogManager.logLoadBalancerError("DELETE_FAILED", "Database error for file " + fileId + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Retrieves all file chunks for a given file from the database.
     *
     * @param fileId The ID of the file.
     * @return A list of FileChunk objects.
     */
    private List<FileChunk> getFileChunks(int fileId) {
        List<FileChunk> chunks = new ArrayList<>();
        String sql = "SELECT * FROM file_chunks WHERE file_id = ? ORDER BY chunk_number";
        try (Connection conn = DatabaseConnection.getConnection(dbType);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, fileId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    chunks.add(new FileChunk(
                        rs.getInt("chunk_id"),
                        rs.getInt("file_id"),
                        rs.getInt("chunk_number"),
                        rs.getInt("server_id"),
                        rs.getString("checksum")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Error retrieving file chunks: " + e.getMessage());
        }
        return chunks;
    }

    /**
     * Stores metadata for a single file chunk in the database.
     */
    private void storeChunkMetadata(int fileId, int chunkNumber, int serverId, String checksum) throws SQLException {
        String sql = "INSERT INTO file_chunks (file_id, chunk_number, server_id, checksum) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection(dbType);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, fileId);
            pstmt.setInt(2, chunkNumber);
            pstmt.setInt(3, serverId);
            pstmt.setString(4, checksum);
            pstmt.executeUpdate();
        }
    }

    /**
     * Deletes all chunk metadata associated with a file from the database.
     */
    private void deleteChunkMetadata(int fileId) throws SQLException {
        String sql = "DELETE FROM file_chunks WHERE file_id = ?";
        try (Connection conn = DatabaseConnection.getConnection(dbType);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, fileId);
            pstmt.executeUpdate();
        }
    }

    /**
     * Deletes file metadata from the database.
     */
    private void deleteFileMetadata(int fileId) throws SQLException {
        String sql = "DELETE FROM files WHERE file_id = ?";
        try (Connection conn = DatabaseConnection.getConnection(dbType);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, fileId);
            pstmt.executeUpdate();
        }
    }

    /**
     * Cleans up all data related to a file in case of a failed upload or other errors.
     * This includes metadata and any chunks that might have been created.
     */
    private void cleanupFile(int fileId) {
        try {
            // Get chunks to know which servers to clean up
            List<FileChunk> chunks = getFileChunks(fileId);
            for (FileChunk chunk : chunks) {
                FileServerClient client = new FileServerClient(chunk.getServerId());
                client.deleteAllFileChunks(fileId);
            }
            deleteChunkMetadata(fileId);
            deleteFileMetadata(fileId);
        } catch (SQLException e) {
            System.err.println("❌ Error during file cleanup: " + e.getMessage());
        }
    }
    
    /**
     * Shares a file with another user.
     *
     * @param fileId The ID of the file to share.
     * @param userId The ID of the user to share with.
     * @param permissionType The type of permission (e.g., "read", "write").
     * @return true if sharing is successful, false otherwise.
     */
    public boolean shareFile(int fileId, int userId, String permissionType) {
        // First, check if the current user is the owner of the file
        int currentUserId = SessionManager.getCurrentUserId();
        if (!isFileOwner(fileId, currentUserId)) {
            System.err.println("❌ Permission denied: User " + currentUserId + " is not the owner of file " + fileId);
            // Assuming logFileShareError exists based on previous conversations.
            LogManager.logFileShareError(String.format("Permission denied: User %d is not the owner of file %d.", currentUserId, fileId));
            return false;
        }

        // Check if the user to share with exists
        if (!userExists(userId)) {
            System.err.println("❌ Share failed: User with ID " + userId + " does not exist.");
            // Assuming logFileShareError exists based on previous conversations.
            LogManager.logFileShareError(String.format("Share failed: User with ID %d does not exist.", userId));
            return false;
        }

        // Check if the permission already exists to prevent duplicates
        if (permissionExists(fileId, userId)) {
            System.err.println("❌ File already shared with user " + userId);
            // Assuming logFileShareError exists based on previous conversations.
            LogManager.logFileShareError(String.format("File %d already shared with user %d.", fileId, userId));
            return false;
        }
        
        String sql = "INSERT INTO file_permissions (file_id, user_id, permission_type) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection(dbType);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, fileId);
            pstmt.setInt(2, userId);
            pstmt.setString(3, permissionType);
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println(String.format("File %d successfully shared with user %d with permission '%s'.", fileId, userId, permissionType));
                // Log the sharing
                // Assuming logFileShare exists based on previous conversations.
                LogManager.logFileShare("File ID: " + fileId, "User ID: " + userId, permissionType);
                return true;
            }

        } catch (SQLException e) {
            System.err.println("❌ Error sharing file: " + e.getMessage());
            e.printStackTrace();
            // Assuming logFileShareError exists based on previous conversations.
            LogManager.logFileShareError(
                String.format("Database error while sharing file %d with user %d: %s", fileId, userId, e.getMessage())
            );
        }

        return false;
    }
    
    /**
     * Placeholder method to check if the current user is the file owner.
     * In a real implementation, this would query the `files` table.
     */
    private boolean isFileOwner(int fileId, int userId) {
        // This is a placeholder for a database query
        String sql = "SELECT owner_id FROM files WHERE file_id = ?";
        try (Connection conn = DatabaseConnection.getConnection(dbType);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, fileId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int ownerId = rs.getInt("owner_id");
                    return ownerId == userId;
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Error checking file ownership: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    private boolean userExists(int userId) {
        String sql = "SELECT 1 FROM users WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection(dbType);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("❌ Error checking user existence: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private boolean permissionExists(int fileId, int userId) {
        String sql = "SELECT 1 FROM file_permissions WHERE file_id = ? AND user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection(dbType);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, fileId);
            pstmt.setInt(2, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("❌ Error checking permission existence: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
