package com.student.cloudjavafx;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;

import javafx.event.ActionEvent;

import com.student.cloudjavafx.auth.SessionManager;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import com.student.cloudjavafx.utils.LogManager;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class AdminDashboardController implements Initializable {


    
    @FXML
    private Button btnuser;
    @FXML
    private Button btnflie;
    @FXML
    private Button btnrole;
    @FXML
    private Button btnlogs;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Allow all logged-in users to access the dashboard.
        // The original check for 'admin' role has been removed.
        // We will now disable specific buttons based on the user's role.

        if (SessionManager.isLoggedIn() && !"admin".equalsIgnoreCase(SessionManager.getCurrentRole())) {
            // Disable buttons that are only for admins
            if (btnuser != null) {
                btnuser.setDisable(true);
            }
            if (btnrole != null) {
                btnrole.setDisable(true);
            }
            if (btnlogs != null) {
                btnlogs.setDisable(true);
            }
        }
        
        // The 'Files' button remains enabled for all users.
        if (btnflie != null) {
            btnflie.setDisable(false);
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

    @FXML
    private void manageUsers(ActionEvent event) {
        try {
            // Check if the user is an admin before allowing navigation
            if ("admin".equalsIgnoreCase(SessionManager.getCurrentRole())) {
                Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(App.loadFXML("User")));
                stage.setTitle("Cloud File System - User Management");
                stage.show();
            } else {
                // Deny access and show a message
                showErrorAlert("Error","Access denied. This feature is for admins only.");
            }
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Error","loading user management view");
        }
    }

    @FXML
    private void manageFiles(ActionEvent event) {
        try {
            // Allow all logged-in users to manage files
            Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(App.loadFXML("Files")));
            stage.setTitle("Cloud File System - File Management");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void managePermissions(ActionEvent event) {
        try {
            // Check if the user is an admin before allowing navigation
            if ("admin".equalsIgnoreCase(SessionManager.getCurrentRole())) {
                Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(App.loadFXML("Permissions")));
                stage.setTitle("Cloud File System - Permissions Management");
                stage.show();
            } else {
                // Deny access and show a message
                showErrorAlert("Error","Access denied. This feature is for admins only.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void manageLogs(ActionEvent event) {
        try {
            // Check if the user is an admin before allowing navigation
            if ("admin".equalsIgnoreCase(SessionManager.getCurrentRole())) {
                Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(App.loadFXML("Logs")));
                stage.setTitle("Cloud File System - Logs");
                stage.show();
            } else {
                // Deny access and show a message
                showErrorAlert("Error","Access denied. This feature is for admins only.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
     @FXML
    private void manageSession(ActionEvent event) {
        try {
            // Check if the user is an admin before allowing navigation
            if ("admin".equalsIgnoreCase(SessionManager.getCurrentRole())) {
                Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(App.loadFXML("Session")));
                stage.setTitle("Cloud File System - Session");
                stage.show();
            } else {
                // Deny access and show a message
                showErrorAlert("Error","Access denied. This feature is for admins only.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showErrorAlert(String error, String loading_user_management_view) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
}
