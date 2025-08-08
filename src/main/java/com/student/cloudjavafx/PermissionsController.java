package com.student.cloudjavafx;

import com.student.cloudjavafx.auth.SessionManager;
import com.student.cloudjavafx.FileManager;
import com.student.cloudjavafx.models.FileItem; // Assuming FileItem is in this package
import com.student.cloudjavafx.models.User;     // Assuming User is in this package
import com.student.cloudjavafx.models.PermissionItem; // Import the PermissionItem model
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
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class PermissionsController implements Initializable {

    @FXML
    private ComboBox<FileItem> fileComboBox;
    @FXML
    private ComboBox<User> userComboBox;
    @FXML
    private ChoiceBox<String> permissionTypeChoiceBox;
    @FXML
    private Label permissionsStatusLabel;
    @FXML
    private Label statusLabel; // For the bottom status bar
    @FXML
    private TableView<PermissionItem> currentPermissionsTable; // Optional: Table to show existing permissions

    private FileManager fileManager;
         DatabaseType dbType = SessionManager.getCurrentDatabaseType();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        fileManager = new FileManager();

        // Check user role: only admins can manage permissions
        if (!SessionManager.isLoggedIn() || !"admin".equalsIgnoreCase(SessionManager.getCurrentRole())) {
            showErrorAlert("Access Denied", "🚫 Access denied. Only administrators can manage permissions.");
            navigateToLogin();
            return;
        }


        // Populate permission types
        permissionTypeChoiceBox.setItems(FXCollections.observableArrayList("read", "write", "download"));
        permissionTypeChoiceBox.setValue("read"); // Default selection

        // Load files and users into ComboBoxes
        loadFilesIntoComboBox();
        loadUsersIntoComboBox();

        updatePermissionsStatus("Ready to assign permissions.");

        // Optional: Setup for currentPermissionsTable if you decide to implement it fully
        // You'd also need a PermissionItem model and corresponding cell value factories
        // currentPermissionsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
        //     if (newSelection != null) {
        //         // Logic to populate fields for editing or display details
        //     }
        // });
    }

    /**
     * Loads all files from the database into the fileComboBox.
     */
    private void loadFilesIntoComboBox() {
        ObservableList<FileItem> files = FXCollections.observableArrayList();
        String sql = "SELECT f.file_id, f.original_name, f.storage_path, f.size, f.owner_id, u.username AS owner_username, f.file_type, f.created_at, f.is_encrypted " +
                     "FROM files f JOIN users u ON f.owner_id = u.id";

        try (Connection conn = DatabaseConnection.getConnection(dbType);
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                files.add(new FileItem(
                    rs.getInt("file_id"),
                    rs.getString("original_name"),
                    rs.getString("storage_path"),
                    rs.getLong("size"),
                    rs.getInt("owner_id"),
                    rs.getString("owner_username"),
                    rs.getString("file_type"),
                    rs.getTimestamp("created_at"),
                    rs.getBoolean("is_encrypted"),
                    "N/A" // Permissions are not relevant when selecting file for sharing
                ));
            }
            fileComboBox.setItems(files);
        } catch (SQLException e) {
            System.err.println("❌ Error loading files for ComboBox: " + e.getMessage());
            showErrorAlert("Database Error", "Failed to load files for selection.\n" + e.getMessage());
        }
    }

    /**
     * Loads all users from the database into the userComboBox.
     */
    private void loadUsersIntoComboBox() {
        ObservableList<User> users = FXCollections.observableArrayList();
        String sql = "SELECT id, username, full_name, email, role, created_at, last_login FROM users";

        try (Connection conn = DatabaseConnection.getConnection(dbType);
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                users.add(new User(
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getString("full_name"),
                    rs.getString("email"),
                    rs.getString("role"),
                    rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null,
                    rs.getTimestamp("last_login") != null ? rs.getTimestamp("last_login").toLocalDateTime() : null
                ));
            }
            userComboBox.setItems(users);
        } catch (SQLException e) {
            System.err.println("❌ Error loading users for ComboBox: " + e.getMessage());
            showErrorAlert("Database Error", "Failed to load users for selection.\n" + e.getMessage());
        }
    }

    /**
     * Handles the action to apply selected permission to the chosen file and user.
     * @param event The action event.
     */
    @FXML
    private void applyPermission(ActionEvent event) {
        FileItem selectedFile = fileComboBox.getSelectionModel().getSelectedItem();
        User selectedUser = userComboBox.getSelectionModel().getSelectedItem();
        String permissionType = permissionTypeChoiceBox.getSelectionModel().getSelectedItem();

        if (selectedFile == null) {
            updatePermissionsStatus("⚠️ Please select a file.");
            return;
        }
        if (selectedUser == null) {
            updatePermissionsStatus("⚠️ Please select a user.");
            return;
        }
        if (permissionType == null || permissionType.isEmpty()) {
            updatePermissionsStatus("⚠️ Please select a permission type.");
            return;
        }

        updatePermissionsStatus("Applying permission...");
        boolean success = fileManager.shareFile(selectedFile.getFileId(), selectedUser.getId(), permissionType);

        if (success) {
            updatePermissionsStatus("✅ Permission '" + permissionType + "' granted for file '" + selectedFile.getOriginalName() + "' to user '" + selectedUser.getUsername() + "'.");
            // Optional: Refresh the currentPermissionsTable if it's implemented
        } else {
            updatePermissionsStatus("❌ Failed to apply permission. Check logs for details.");
            showErrorAlert("Permission Error", "Could not apply permission. Ensure you have the necessary rights (file owner or admin) and the user/file exists.");
        }
    }

    /**
     * Updates the status label specific to permissions management.
     * @param message The message to display.
     */
    private void updatePermissionsStatus(String message) {
        if (permissionsStatusLabel != null) {
            permissionsStatusLabel.setText(message);
        }
    }

    /**
     * Updates the general status label at the bottom of the UI.
     * @param message The message to display.
     */
    private void updateStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }

    /**
     * Displays an error alert dialog.
     * @param title The title of the alert.
     * @param message The content message of the alert.
     */
    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Handles the logout action, clearing the session and returning to the login screen.
     * @param event The action event.
     */
    @FXML
    private void logout(ActionEvent event) {
        LogManager.logLogout(SessionManager.getCurrentUsername());
        SessionManager.logout();
        try {
            Parent root = App.loadFXML("Login"); // Assuming App.loadFXML is public static
            Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Cloud File System - Login");
            stage.show();
        } catch (IOException e) {
            System.err.println("Error loading login screen: " + e.getMessage());
            showErrorAlert("Navigation Error", "Could not load login screen.");
        }
    }


    @FXML
    private void backhome(ActionEvent event) {

         try {
            // No logout here, just navigate
            Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(App.loadFXML("AdminDashboard")));
            stage.setTitle("Cloud File System - Logs");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    

    /**
     * Navigates the user back to the login screen.
     */
    private void navigateToLogin() {
        try {
            // Get the current stage and set the root to Login
            Stage stage = (Stage) permissionsStatusLabel.getScene().getWindow(); // Assuming permissionsStatusLabel is always present
            stage.setScene(new Scene(App.loadFXML("Login")));
            stage.setTitle("Cloud File System - Login");
            stage.show();
        } catch (IOException e) {
            System.err.println("Error loading login screen: " + e.getMessage());
            showErrorAlert("Navigation Error", "Could not load login screen.");
        }
    }
}
