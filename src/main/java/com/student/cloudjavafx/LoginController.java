package com.student.cloudjavafx;

import com.student.cloudjavafx.auth.SessionManager;
import com.student.cloudjavafx.utils.PasswordUtil;
import com.student.cloudjavafx.utils.DatabaseConnection;
import com.student.cloudjavafx.utils.DatabaseConnection.DatabaseType;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
// Import Hyperlink
import javafx.scene.control.*;
import javafx.stage.Stage; // Import Stage
import javafx.scene.Scene; // Import Scene
import javafx.scene.Parent; // Import Parent
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.net.InetAddress; // For getting IP address
import java.net.URL;
import java.net.UnknownHostException; // For handling IP address errors
import java.util.ResourceBundle;
import javafx.fxml.Initializable;

public class LoginController implements Initializable {

    @FXML
    private TextField usernametxt;
    @FXML
    private PasswordField passtxt;
    @FXML
    private Button loginBtn; // Corrected fx:id to match FXML
    @FXML
    private Label statusLabel;
@FXML
private ComboBox<String> serverComboBox;

    @FXML
    private ComboBox<DatabaseType> dbTypeComboBox;
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
                // Populate the ComboBox with the available database types
        dbTypeComboBox.getItems().addAll(DatabaseType.values());
        // Select the first item by default
        dbTypeComboBox.getSelectionModel().selectFirst();

    }

      @FXML
    private void login(ActionEvent event) {
        String username = usernametxt.getText().trim();
        String password = passtxt.getText();
        DatabaseType selectedDbType = dbTypeComboBox.getSelectionModel().getSelectedItem();

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Please enter both username and password.");
            return;
        }
        
        if (selectedDbType == null) {
            statusLabel.setText("Please select a database type.");
            return;
        }

        // The query to check if the user exists. We use a PreparedStatement to prevent SQL injection.
        // This query is updated to select the password hash and user role for secure authentication.
        String query = "SELECT id, username, password_hash, role FROM users WHERE username = ?";
        
        try (Connection conn = DatabaseConnection.getConnection(selectedDbType);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, username);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                // Get the stored password hash from the database.
                String storedHash = rs.getString("password_hash").trim();
                
                // Use a secure utility to verify the password.
                if (PasswordUtil.verifyPassword(password, storedHash)) {
                    // Login successful
                    int userId = rs.getInt("id");
                    String userRole = rs.getString("role");

                    // Get IP Address for session logging
                    String ipAddress = "N/A"; // Default value
                    try {
                        ipAddress = InetAddress.getLocalHost().getHostAddress();
                    } catch (UnknownHostException e) {
                        System.err.println("Could not get IP address: " + e.getMessage());
                        // Fallback to N/A
                    }
                    
                    // Session recording - passing 4 arguments
                    SessionManager.createSession(userId, username, userRole, ipAddress, selectedDbType);

                    statusLabel.setText("✅ Welcome " + username + " (" + userRole + ")");
                    // Pass event to redirect method
                    redirectBasedOnRole(userRole, event); 

                } else {
                    // Login failed due to incorrect password
                    statusLabel.setText("❌ Incorrect password");
                }
            } else {
                // Login failed due to username not existing
                statusLabel.setText("❌ Username does not exist");
            }

        } catch (SQLException e) {
            statusLabel.setText("⚠️ Database error: " + e.getMessage());
            System.err.println("Database error during login: " + e.getMessage());
        }
    }

    private void redirectBasedOnRole(String role, ActionEvent event) { // Accept ActionEvent
        try {
            // Updated logic to always redirect to the AdminDashboard,
            // as the AdminDashboardController now handles showing content based on role.
            String fxml = "AdminDashboard";
            
            // Get the current stage from the event source
            Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
            Parent root = App.loadFXML(fxml); // Use App.loadFXML
            stage.setScene(new Scene(root));
            stage.setTitle("Cloud File System - Dashboard"); // Set a generic title
            stage.show();

        } catch (IOException e) {
            statusLabel.setText("⚠️ Error loading interface: " + e.getMessage()); // Show more specific error
            e.printStackTrace();
        }
    }

    @FXML
    public void initialize() {

        System.out.println("LoginController initialized");

        // Input validation for username length
        usernametxt.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.length() > 50) {
                usernametxt.setText(oldVal);
                statusLabel.setText("Username cannot exceed 50 characters.");
            } else {
                statusLabel.setText(""); // Clear status if length is okay
            }
        });

        // Input validation for password length
        passtxt.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.length() > 100) {
                passtxt.setText(oldVal);
                statusLabel.setText("Password cannot exceed 100 characters.");
            } else {
                statusLabel.setText(""); // Clear status if length is okay
            }
        });
    }

    /**
     * Handles the action for the "Register new account" hyperlink.
     * This method assumes you have a "Register.fxml" file.
     * * @param event The action event.
     */
    @FXML
    private void registerNewAccount(ActionEvent event) {
        try {
            Parent registerView = App.loadFXML("Register"); // Assuming Register.fxml exists
            Stage stage = (Stage) ((Hyperlink) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(registerView));
            stage.setTitle("Cloud File System - Register Account");
            stage.show();
        } catch (IOException e) {
            System.err.println("Error loading registration screen: " + e.getMessage());
            statusLabel.setText("⚠️ Error loading registration screen.");
        }
    }
}
