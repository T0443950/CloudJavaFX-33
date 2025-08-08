package com.student.cloudjavafx;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.student.cloudjavafx.auth.SessionManager;
import com.student.cloudjavafx.models.User;
import com.student.cloudjavafx.utils.DatabaseConnection;
import com.student.cloudjavafx.utils.DatabaseConnection.DatabaseType;
import com.student.cloudjavafx.utils.LogManager;
import com.student.cloudjavafx.utils.PasswordUtil;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

public class UserController implements Initializable {

    @FXML
    private TextField fullNameField;
    @FXML
    private TextField usernameField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField SearchField;
    @FXML
    private PasswordField passwordField;

    @FXML
    private ChoiceBox<String> roleComboBox;
    @FXML
    private Label statusLabel;
    @FXML
    private Button butAdd;
    @FXML
    private Button butEdit;
    @FXML
    private Button butDelete;

    @FXML
    private TableView<User> usersTable;
    @FXML
    private TableColumn<User, String> usernameColumn;
    @FXML
    private TableColumn<User, String> emailColumn;
    @FXML
    private TableColumn<User, String> roleColumn;
    @FXML
    private TableColumn<User, String> createdAtColumn;

    private User userData;

    // Removed the class-level DatabaseType variable to avoid stale data.
    // Instead, we will retrieve the database type just before each connection.

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Initialize ChoiceBox with correct values
        roleComboBox.setItems(FXCollections.observableArrayList("admin", "standard"));
        roleComboBox.setValue("standard");

        // Check user permissions
        if (!SessionManager.isLoggedIn() || !"admin".equalsIgnoreCase(SessionManager.getCurrentRole())) {
            showAlert("🚫 Access denied. Admins only.");
            navigateToLogin();
            return;
        }
        butEdit.setDisable(true);
        butDelete.setDisable(true);

        // Add listener to selection change
        usersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            boolean disable = (newSel == null);
            butEdit.setDisable(disable);
            butDelete.setDisable(disable);

            if (newSel != null) {
                setUserData(newSel);
            }
        });

        // Initialize table
        usersTable.setItems(getAllUsers());
        configureTableColumns();
    }

    private void configureTableColumns() {
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        createdAtColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void navigateToLogin() {
        try {
            // Get the current stage and set the root to Login
            Stage stage = (Stage) statusLabel.getScene().getWindow(); // Assuming statusLabel is always present
            stage.setScene(new Scene(App.loadFXML("Login")));
            stage.setTitle("Cloud File System - Login");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
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
    private void searchUsers() {
        String searchTerm = SearchField.getText().trim();
        if (!searchTerm.isEmpty()) {
            usersTable.setItems(getSearchUsers(searchTerm));
        } else {
            usersTable.setItems(getAllUsers());
        }
    }

    public ObservableList<User> getAllUsers() {
        ObservableList<User> users = FXCollections.observableArrayList();
        // Retrieve the database type just before the connection
        DatabaseType dbType = SessionManager.getCurrentDatabaseType();
        try (Connection connection = DatabaseConnection.getConnection(dbType);
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM users");
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                users.add(createUserFromResultSet(resultSet));
            }
        } catch (SQLException e) {
            Logger.getLogger(UserController.class.getName()).log(Level.SEVERE, null, e);
        }
        return users;
    }

    public ObservableList<User> getSearchUsers(String searchTerm) {
        ObservableList<User> users = FXCollections.observableArrayList();
        String query = "SELECT * FROM users WHERE username LIKE ? OR full_name LIKE ? OR email LIKE ?";

        // Retrieve the database type just before the connection
        DatabaseType dbType = SessionManager.getCurrentDatabaseType();
        try (Connection connection = DatabaseConnection.getConnection(dbType);
             PreparedStatement statement = connection.prepareStatement(query)) {

            String searchPattern = "%" + searchTerm + "%";
            statement.setString(1, searchPattern);
            statement.setString(2, searchPattern);
            statement.setString(3, searchPattern);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    users.add(createUserFromResultSet(resultSet));
                }
            }
        } catch (SQLException e) {
            Logger.getLogger(UserController.class.getName()).log(Level.SEVERE, null, e);
        }
        return users;
    }

    private User createUserFromResultSet(ResultSet resultSet) throws SQLException {
        int id = resultSet.getInt("id");
        String username = resultSet.getString("username");
        String fullName = resultSet.getString("full_name");
        String email = resultSet.getString("email");
        String role = resultSet.getString("role");

        // Handle null values for created_at
        LocalDateTime createdAt = null;
        Timestamp createdAtTimestamp = resultSet.getTimestamp("created_at");
        if (createdAtTimestamp != null) {
            createdAt = createdAtTimestamp.toLocalDateTime();
        }

        // Handle null values for last_login
        LocalDateTime lastLogin = null;
        Timestamp lastLoginTimestamp = resultSet.getTimestamp("last_login");
        if (lastLoginTimestamp != null) {
            lastLogin = lastLoginTimestamp.toLocalDateTime();
        }

        return new User(id, username, fullName, email, role, createdAt, lastLogin);
    }

    @FXML
    private void insert() {
        String fullName = fullNameField.getText().trim();
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String role = roleComboBox.getValue();

        if (!validateUserInput(fullName, username, email, password, role)) {
            return;
        }

        String hashedPassword = PasswordUtil.hashPassword(password);
        String insertSQL = "INSERT INTO users (username, full_name, email, password_hash, role) VALUES (?, ?, ?, ?, ?)";
        
        // Retrieve the database type just before the connection
        DatabaseType dbType = SessionManager.getCurrentDatabaseType();
        try (Connection conn = DatabaseConnection.getConnection(dbType);
             PreparedStatement stmt = conn.prepareStatement(insertSQL)) {

            stmt.setString(1, username);
            stmt.setString(2, fullName);
            stmt.setString(3, email);
            stmt.setString(4, hashedPassword);
            stmt.setString(5, role);

            if (stmt.executeUpdate() > 0) {
                showSuccessAlert("✅ User added successfully.");
                // Use the specific logging method
                LogManager.logUserRegistration(username);
                clearFields();
                usersTable.setItems(getAllUsers());
            } else {
                statusLabel.setText("❌ User addition failed.");
                // For internal errors, use System.err.println or a dedicated error logging method
                System.err.println("Failed to add user: " + username);
            }
        } catch (SQLException ex) {
            handleDatabaseError("Database error while creating user: ", ex);
        }
    }

    private boolean validateUserInput(String fullName, String username, String email, String password, String role) {
        if (fullName.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty() || role == null) {
            statusLabel.setText("⚠️ Please fill in all fields.");
            return false;
        }

        if (password.length() < 8) {
            statusLabel.setText("⚠️ Password must be at least 8 characters.");
            return false;
        }

        if (!email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            statusLabel.setText("⚠️ Please enter a valid email address.");
            return false;
        }

        if (usernameExists(username)) {
            statusLabel.setText("⚠️ Username already exists.");
            return false;
        }

        if (emailExists(email)) {
            statusLabel.setText("⚠️ Email already exists.");
            return false;
        }

        return true;
    }

    private void showSuccessAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void clearFields() {
        fullNameField.clear();
        usernameField.clear();
        emailField.clear();
        passwordField.clear();
        roleComboBox.setValue("standard");
    }

    private void handleDatabaseError(String message, SQLException ex) {
        statusLabel.setText("⚠️ A database error occurred.");
        // For internal errors, use System.err.println or a dedicated error logging method
        System.err.println(message + ex.getMessage());
        ex.printStackTrace();
    }

    private boolean usernameExists(String username) {
        String query = "SELECT COUNT(*) FROM users WHERE username = ?";
        // Retrieve the database type just before the connection
        DatabaseType dbType = SessionManager.getCurrentDatabaseType();
        try (Connection conn = DatabaseConnection.getConnection(dbType); PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private boolean emailExists(String email) {
        String query = "SELECT COUNT(*) FROM users WHERE email = ?";
        // Retrieve the database type just before the connection
        DatabaseType dbType = SessionManager.getCurrentDatabaseType();
        try (Connection conn = DatabaseConnection.getConnection(dbType); PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public void setUserData(User user) {
        this.userData = user;
        populateFields();
    }

    private void populateFields() {
        if (userData != null) {
            fullNameField.setText(userData.getFullName());
            usernameField.setText(userData.getUsername());
            emailField.setText(userData.getEmail());
            roleComboBox.setValue(userData.getRole());
            passwordField.clear();
        }
    }

    @FXML
    private void update() {
        String fullName = fullNameField.getText().trim();
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String role = roleComboBox.getValue();

        if (!validateUpdateInput(fullName, username, email, role)) {
            return;
        }

        try {
            if (password.isEmpty()) {
                updateUserWithoutPassword(fullName, username, email, role);
            } else {
                updateUserWithPassword(fullName, username, email, password, role);
            }
        } catch (SQLException | IOException ex) { // IOException added for App.setRoot if it were used here
            handleDatabaseError("Database error while updating user: ", (SQLException) ex);
        }
    }

    private boolean validateUpdateInput(String fullName, String username, String email, String role) {
        if (username.isEmpty() || fullName.isEmpty() || email.isEmpty() || role == null) {
            statusLabel.setText("⚠️ Please fill in all required fields.");
            return false;
        }

        if (!email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            statusLabel.setText("⚠️ Please enter a valid email address.");
            return false;
        }

        if (!username.equals(userData.getUsername()) && usernameExists(username)) {
            statusLabel.setText("⚠️ Username already exists.");
            return false;
        }

        if (!email.equals(userData.getEmail()) && emailExists(email)) {
            statusLabel.setText("⚠️ Email already exists.");
            return false;
        }

        return true;
    }

    private void updateUserWithoutPassword(String fullName, String username, String email, String role)
            throws SQLException, IOException {
        String updateSQL = "UPDATE users SET username = ?, full_name = ?, email = ?, role = ? WHERE id = ?";

        // Retrieve the database type just before the connection
        DatabaseType dbType = SessionManager.getCurrentDatabaseType();
        try (Connection conn = DatabaseConnection.getConnection(dbType);
             PreparedStatement stmt = conn.prepareStatement(updateSQL)) {

            stmt.setString(1, username);
            stmt.setString(2, fullName);
            stmt.setString(3, email);
            stmt.setString(4, role);
            stmt.setInt(5, userData.getId());

            handleUpdateResult(stmt.executeUpdate(), username);
        }
    }

    private void updateUserWithPassword(String fullName, String username, String email, String password, String role)
            throws SQLException, IOException {
        if (password.length() < 8) {
            statusLabel.setText("⚠️ Password must be at least 8 characters.");
            return;
        }

        String hashedPassword = PasswordUtil.hashPassword(password);
        String updateSQL = "UPDATE users SET username = ?, full_name = ?, email = ?, password_hash = ?, role = ? WHERE id = ?";

        // Retrieve the database type just before the connection
        DatabaseType dbType = SessionManager.getCurrentDatabaseType();
        try (Connection conn = DatabaseConnection.getConnection(dbType);
             PreparedStatement stmt = conn.prepareStatement(updateSQL)) {

            stmt.setString(1, username);
            stmt.setString(2, fullName);
            stmt.setString(3, email);
            stmt.setString(4, hashedPassword);
            stmt.setString(5, role);
            stmt.setInt(6, userData.getId());

            handleUpdateResult(stmt.executeUpdate(), username);
        }
    }

    private void handleUpdateResult(int rowsAffected, String username) throws IOException {
        if (rowsAffected > 0) {
            showSuccessAlert("✅ User updated successfully.");
            // Use the specific logging method
            LogManager.logUserUpdate(username);
            refreshView();
        } else {
            statusLabel.setText("❌ No changes were made.");
        }
    }

    @FXML
    private void refresh() {
        refreshView();
    }

    private void refreshView() {
        usersTable.setItems(getAllUsers());
        clearFields();
        userData = null;
    }

    @FXML
    private void deleteUser(ActionEvent event) {
        User selectedUser = usersTable.getSelectionModel().getSelectedItem();
        if (selectedUser != null) {
            if (confirmDelete(selectedUser.getUsername())) {
                // Retrieve the database type just before the connection
                DatabaseType dbType = SessionManager.getCurrentDatabaseType();
                try (Connection conn = DatabaseConnection.getConnection(dbType);
                     PreparedStatement stmt = conn.prepareStatement("DELETE FROM users WHERE id = ?")) {

                    stmt.setInt(1, selectedUser.getId());
                    int rowsAffected = stmt.executeUpdate();

                    if (rowsAffected > 0) {
                        statusLabel.setText("✅ User deleted successfully.");
                        // Use the specific logging method
                        LogManager.logUserDelete(selectedUser.getUsername());
                        usersTable.setItems(getAllUsers());
                    }
                } catch (SQLException ex) {
                    statusLabel.setText("⚠️ Error deleting user.");
                    Logger.getLogger(UserController.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    private boolean confirmDelete(String username) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setHeaderText(null);
        alert.setContentText("Are you sure you want to delete user: " + username + "?");

        return alert.showAndWait().get() == ButtonType.OK;
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
}
