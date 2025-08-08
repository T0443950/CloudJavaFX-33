package com.student.cloudjavafx;

import com.student.cloudjavafx.auth.SessionManager;
import com.student.cloudjavafx.utils.DatabaseConnection;
import com.student.cloudjavafx.utils.DatabaseConnection.DatabaseType;
import com.student.cloudjavafx.utils.LogManager;
import com.student.cloudjavafx.utils.MySqlConnect;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ResourceBundle;

/**
 * Controller for the logs view. It handles fetching log data from the database
 * and populating the TableView.
 */
public class LogsController implements Initializable {

    @FXML
    private TableView<LogManager> logsTable; // Updated to use LogManager model class as per table columns
    @FXML
    private TableColumn<LogManager, String> usernameColumn;
    @FXML
    private TableColumn<LogManager, String> actionColumn; // Changed to match table column `action`
    @FXML
    private TableColumn<LogManager, String> detailsColumn;
    @FXML
    private TableColumn<LogManager, String> logLevelColumn; // Added column for log_level
    @FXML
    private TableColumn<LogManager, String> timestampColumn;
    
    @FXML
    private Button backButton;
    @FXML
    private Button logoutButton;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Set up the columns
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        actionColumn.setCellValueFactory(new PropertyValueFactory<>("action")); // Updated to match the new field name
        detailsColumn.setCellValueFactory(new PropertyValueFactory<>("details"));
        logLevelColumn.setCellValueFactory(new PropertyValueFactory<>("logLevel")); // Added to match the new field
        timestampColumn.setCellValueFactory(new PropertyValueFactory<>("timestampFormatted"));

        // Load logs from the database
        loadLogs();
    }

    /**
     * Loads log data from the `logs` table in the database.
     */
     DatabaseType dbType = SessionManager.getCurrentDatabaseType();

    private void loadLogs() {
        // Clear existing data and set a loading message
        logsTable.getItems().clear();

        ObservableList<LogManager> logsList = FXCollections.observableArrayList();
        String sql = "SELECT `log_id`, `user_id`, `username`, `action`, `log_level`, `details`, `timestamp` FROM `logs` ORDER BY `timestamp` DESC";
        try (Connection conn = DatabaseConnection.getConnection(dbType);
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                int logId = rs.getInt("log_id");
                int userId = rs.getInt("user_id");
                String username = rs.getString("username");
                String action = rs.getString("action");
                String logLevel = rs.getString("log_level");
                String details = rs.getString("details");
                java.sql.Timestamp timestamp = rs.getTimestamp("timestamp");

                // Handle cases where the user might have been deleted (username is null)
                String displayUsername = (username != null) ? username : "N/A (User Deleted)";

                logsList.add(new LogManager(logId, userId, displayUsername, action, logLevel, details, timestamp));
            }
            logsTable.setItems(logsList);
        } catch (SQLException e) {
            System.err.println("❌ Error loading logs: " + e.getMessage());
            e.printStackTrace();
            showErrorAlert("Database Error", "Could not load system logs from the database.");
        }
    }

    @FXML
    private void backhome(ActionEvent event) {
        try {
            // Navigate back to the admin dashboard
            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(new Scene(App.loadFXML("AdminDashboard")));
            stage.setTitle("Cloud File System - Admin Dashboard");
            stage.show();
        } catch (IOException e) {
            System.err.println("Error loading AdminDashboard: " + e.getMessage());
            showErrorAlert("Navigation Error", "Could not load the admin dashboard.");
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
    
    /**
     * Helper method to show an error alert dialog.
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
}