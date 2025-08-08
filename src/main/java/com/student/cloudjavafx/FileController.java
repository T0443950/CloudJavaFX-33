package com.student.cloudjavafx;

import com.student.cloudjavafx.auth.SessionManager;
import com.student.cloudjavafx.models.FileItem;
import com.student.cloudjavafx.utils.DatabaseConnection;
import com.student.cloudjavafx.utils.DatabaseConnection.DatabaseType;
import com.student.cloudjavafx.utils.LogManager;
import com.student.cloudjavafx.utils.MySqlConnect;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class FileController implements Initializable {

    @FXML
    private TableView<FileItem> allFilesTable;
    @FXML
    private TableColumn<FileItem, String> nameColumn;
    @FXML
    private TableColumn<FileItem, String> ownerColumn;
    @FXML
    private TableColumn<FileItem, String> sizeColumn;
    @FXML
    private TableColumn<FileItem, String> uploadDateColumn;
    @FXML
    private TableColumn<FileItem, String> typeColumn;
    @FXML
    private TableColumn<FileItem, String> permissionsColumn;

    @FXML
    private TextField searchField;
    @FXML
    private Button uploadButton;
    @FXML
    private Button downloadButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Button shareButton;
    @FXML
    private TextField filePathField;

    private final FileManager fileManager = new FileManager();
    private ObservableList<FileItem> masterData = FXCollections.observableArrayList();

     DatabaseType dbType = SessionManager.getCurrentDatabaseType();
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Initialize table columns
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("originalName"));
        ownerColumn.setCellValueFactory(new PropertyValueFactory<>("owner"));
        sizeColumn.setCellValueFactory(new PropertyValueFactory<>("sizeFormatted"));
        uploadDateColumn.setCellValueFactory(new PropertyValueFactory<>("createdAtFormatted"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("fileType"));
        permissionsColumn.setCellValueFactory(new PropertyValueFactory<>("permissions"));


        loadMyFiles();

        // Add listeners for table selection
        allFilesTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                // Enable/disable buttons based on permissions
                boolean isOwner = newSelection.getOwnerId() == SessionManager.getCurrentUserId();
                downloadButton.setDisable(false); // Assume download is always allowed if they have access
                deleteButton.setDisable(!isOwner);
                shareButton.setDisable(!isOwner);
            } else {
                // Disable all buttons if no file is selected
                downloadButton.setDisable(true);
                deleteButton.setDisable(true);
                shareButton.setDisable(true);
            }
        });
    }

    /**
     * Loads files owned by the current user into the table.
     */

    @FXML
    private void loadMyFiles() {
        int currentUserId = SessionManager.getCurrentUserId();
        masterData.clear();
        String sql = "SELECT f.*, u.username as owner_name, "
                + "(SELECT GROUP_CONCAT(permission_type) FROM file_permissions fp WHERE fp.file_id = f.file_id AND fp.user_id = ?) AS user_permission "
                + "FROM files f JOIN users u ON f.owner_id = u.id WHERE f.owner_id = ?";
        try (Connection conn = DatabaseConnection.getConnection(dbType); 
    PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, currentUserId);
            pstmt.setInt(2, currentUserId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                // The user is the owner, so they have all permissions.
                String permissions = "Owner";
                masterData.add(new FileItem(
                        rs.getInt("file_id"),
                        rs.getString("original_name"),
                        rs.getString("storage_path"),
                        rs.getLong("size"),
                        rs.getInt("owner_id"),
                        rs.getString("owner_name"),
                        rs.getString("file_type"),
                        rs.getTimestamp("created_at"),
                        rs.getBoolean("is_encrypted"),
                        permissions
                ));
            }
        } catch (SQLException e) {
            showErrorAlert("Database Error", "Failed to load files.");
            e.printStackTrace();
        }
        allFilesTable.setItems(masterData);
//        statusLabel.setText("Showing files owned by you.");
    }

    /**
     * Loads files shared with the current user into the table.
     */
    @FXML
    private void loadSharedWithMeFiles() {
        int currentUserId = SessionManager.getCurrentUserId();
        masterData.clear();
        String sql = "SELECT f.*, u.username as owner_name, fp.permission_type "
                + "FROM files f "
                + "JOIN users u ON f.owner_id = u.id "
                + "JOIN file_permissions fp ON f.file_id = fp.file_id "
                + "WHERE fp.user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection(dbType); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, currentUserId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String permissions = rs.getString("permission_type");
                masterData.add(new FileItem(
                        rs.getInt("file_id"),
                        rs.getString("original_name"),
                        rs.getString("storage_path"),
                        rs.getLong("size"),
                        rs.getInt("owner_id"),
                        rs.getString("owner_name"),
                        rs.getString("file_type"),
                        rs.getTimestamp("created_at"),
                        rs.getBoolean("is_encrypted"),
                        permissions
                ));
            }
        } catch (SQLException e) {
            showErrorAlert("Database Error", "Failed to load files shared with you.");
            e.printStackTrace();
        }
        allFilesTable.setItems(masterData);
    }

    @FXML
    private void uploadFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File to Upload");
        File file = fileChooser.showOpenDialog(allFilesTable.getScene().getWindow());

        if (file != null) {
            try (FileInputStream fileStream = new FileInputStream(file)) {
                if (fileManager.uploadFile(file.getName(), fileStream, file.length(), "application/octet-stream") != -1) {
                    showInfoAlert("Upload Successful", "File '" + file.getName() + "' uploaded successfully.");
                    loadMyFiles(); // Refresh the table
                } else {
                    showErrorAlert("Upload Failed", "Failed to upload file '" + file.getName() + "'.");
                }
            } catch (IOException e) {
                showErrorAlert("Upload Error", "An error occurred during file upload: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @FXML
//    private void downloadFile(ActionEvent event) {
    private void downloadSelectedFile(ActionEvent event) {
        FileItem selectedFile = allFilesTable.getSelectionModel().getSelectedItem();
        if (selectedFile != null) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save File As");
            fileChooser.setInitialFileName(selectedFile.getOriginalName());
            File file = fileChooser.showSaveDialog(allFilesTable.getScene().getWindow());

            if (file != null) {
                try {
                    // Corrected method call: fileManager.downloadFile now takes the file ID and the save path.
                    // The error was caused by passing two arguments (fileId and file.getAbsolutePath())
                    // to a method that only expected one (fileId).
                    fileManager.downloadFile(selectedFile.getFileId(), file.getAbsolutePath());
                    showInfoAlert("Download Successful", "File '" + selectedFile.getOriginalName() + "' downloaded successfully.");
                } catch (IOException e) {
                    showErrorAlert("Download Failed", "An error occurred during file download: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } else {
            showWarningAlert("No File Selected", "Please select a file to download.");
        }
    }

    @FXML
//    private void deleteFile(ActionEvent event) {
    private void deleteSelectedFile(ActionEvent event) {

        FileItem selectedFile = allFilesTable.getSelectionModel().getSelectedItem();
        if (selectedFile != null) {
            boolean isOwner = selectedFile.getOwnerId() == SessionManager.getCurrentUserId();
            if (isOwner) {
                if (fileManager.deleteFile(selectedFile.getFileId())) {
                    showInfoAlert("Deletion Successful", "File '" + selectedFile.getOriginalName() + "' deleted successfully.");
                    loadMyFiles();
                } else {
                    showErrorAlert("Deletion Failed", "Failed to delete file '" + selectedFile.getOriginalName() + "'.");
                }
            } else {
                showErrorAlert("Permission Denied", "You can only delete files you own.");
            }
        } else {
            showWarningAlert("No File Selected", "Please select a file to delete.");
        }
    }
 /**
     * Handles the "Share Selected" button action.
     * Allows sharing the selected file with other users.
     * This would typically open a new dialog for selecting users and permissions.
     * @param event The action event.
     */
    @FXML
    private void shareSelectedFile(ActionEvent event) {
        FileItem selectedFile = allFilesTable.getSelectionModel().getSelectedItem();
        if (selectedFile == null) {
            showInfoAlert("Worng", "Please select a file to share.");
            return;
        }

        // updateStatus("Sharing file: " + selectedFile.getOriginalName() + "...");
        LogManager.logAction(LogManager.FILE_SHARE_INIT, "Initiating share for file: " + selectedFile.getOriginalName());

        // In a real application, open a new dialog for sharing:
        // - Allow selecting other users
        // - Allow setting read/write permissions
        // - Call a method to update the file_permissions table
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Share File");
        alert.setHeaderText("File Sharing Feature");
        alert.setContentText("This feature would open a dialog to select users and set permissions for '" + selectedFile.getOriginalName() + "'.\n\n(Not implemented in this basic example)");
        alert.showAndWait();
        LogManager.logAction(LogManager.FILE_SHARE_INFO, "User attempted to share file: " + selectedFile.getOriginalName());
    }

    @FXML
    private void browseFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File to Upload");
        File selectedFile = fileChooser.showOpenDialog(new Stage());
        if (selectedFile != null) {
            filePathField.setText(selectedFile.getAbsolutePath());
            showErrorAlert("Sucsses","File selected: " + selectedFile.getName());
            LogManager.logAction(LogManager.FILE_BROWSE, "User selected file: " + selectedFile.getName());
        } else {
            showErrorAlert("Error","File selection cancelled.");
            LogManager.logAction(LogManager.FILE_BROWSE_CANCEL, "User cancelled file selection.");
        }
    }

    @FXML
    private void searchFiles(ActionEvent event) {
        String query = searchField.getText().toLowerCase();
        ObservableList<FileItem> filteredList = masterData.filtered(file
                -> file.getOriginalName().toLowerCase().contains(query)
                || file.getOwner().toLowerCase().contains(query)
        );
        allFilesTable.setItems(filteredList);
    }
    
        /**
     * Handles the "Refresh" button action.
     * Reloads the list of files from the database.
     * @param event The action event.
     */
    @FXML
    private void refreshFiles(ActionEvent event) {
        loadMyFiles();
//        showErrorAlert("File list refreshed.");
        LogManager.logAction(LogManager.FILE_REFRESH, "File list refreshed by user.");
    }


    @FXML
    private void backhome(ActionEvent event) {
        try {
            Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(App.loadFXML("AdminDashboard")));
            stage.setTitle("Cloud File System - Logs");
            stage.show();
        } catch (IOException e) {
            System.err.println("Error loading home screen: " + e.getMessage());
            showErrorAlert("Navigation Error", "Could not load home screen.");
        }
    }

    @FXML
    private void logout(ActionEvent event) {
        // Pass the current username to logLogout
        LogManager.logLogout(SessionManager.getCurrentUsername());
        SessionManager.logout();
        try {
            // Get the current stage and set the root to Login
            Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(App.loadFXML("Login")));
            stage.setTitle("Cloud File System - Login");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void showInfoAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarningAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

//    private void updateStatus(String message) {
//        javafx.application.Platform.runLater(() -> statusLabel.setText(message));
//    }
}
