package com.student.cloudjavafx.utils;

import java.sql.*;

/**
 * A utility class for synchronizing data from a MySQL database to a SQLite database.
 * This class handles one-way synchronization, inserting or updating data in SQLite
 * based on the data present in MySQL.
 */
public class SyncManager {

    /**
     * Synchronizes data for a specific user from MySQL to SQLite.
     * This method assumes a user has already been authenticated and the connections
     * to both databases are active.
     *
     * @param userId The ID of the user whose data should be synchronized.
     */
    public static void synchronizeUserData(int userId) {
        System.out.println("Starting synchronization for user ID: " + userId);
        try (Connection mysqlConn = DatabaseConnection.getConnection(DatabaseConnection.DatabaseType.MYSQL);
             Connection sqliteConn = DatabaseConnection.getConnection(DatabaseConnection.DatabaseType.SQLITE)) {

            sqliteConn.setAutoCommit(false); // Begin transaction for SQLite
            
            // Synchronize the main user's data
            syncUser(mysqlConn, sqliteConn, userId);

            // Synchronize the files owned by the user
            syncFiles(mysqlConn, sqliteConn, userId);

            // Synchronize the file permissions for the user
            syncFilePermissions(mysqlConn, sqliteConn, userId);

            // Synchronize file chunks for the user's files
            syncFileChunks(mysqlConn, sqliteConn, userId);

            // Synchronize logs related to the user
            syncLogs(mysqlConn, sqliteConn, userId);

            sqliteConn.commit(); // Commit the transaction
            System.out.println("✅ Synchronization completed successfully for user ID: " + userId);
        } catch (SQLException e) {
            System.err.println("❌ Synchronization failed: " + e.getMessage());
            try {
                // Rollback the transaction in case of an error
                if (DatabaseConnection.getConnection(DatabaseConnection.DatabaseType.SQLITE) != null) {
                    DatabaseConnection.getConnection(DatabaseConnection.DatabaseType.SQLITE).rollback();
                }
            } catch (SQLException rollbackException) {
                System.err.println("❌ Error during transaction rollback: " + rollbackException.getMessage());
            }
        }
    }

    /**
     * Synchronizes user data.
     * @param mysqlConn The connection to the MySQL database.
     * @param sqliteConn The connection to the SQLite database.
     * @param userId The ID of the user to synchronize.
     * @throws SQLException if a database error occurs.
     */
    private static void syncUser(Connection mysqlConn, Connection sqliteConn, int userId) throws SQLException {
        String mysqlQuery = "SELECT * FROM users WHERE id = ?";
        String sqliteInsert = "INSERT OR REPLACE INTO users (id, username, full_name, password_hash, email, role, created_at, last_login) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement mysqlPstmt = mysqlConn.prepareStatement(mysqlQuery);
             PreparedStatement sqlitePstmt = sqliteConn.prepareStatement(sqliteInsert)) {

            mysqlPstmt.setInt(1, userId);
            ResultSet rs = mysqlPstmt.executeQuery();

            if (rs.next()) {
                sqlitePstmt.setInt(1, rs.getInt("id"));
                sqlitePstmt.setString(2, rs.getString("username"));
                sqlitePstmt.setString(3, rs.getString("full_name"));
                sqlitePstmt.setString(4, rs.getString("password_hash"));
                sqlitePstmt.setString(5, rs.getString("email"));
                sqlitePstmt.setString(6, rs.getString("role"));
                sqlitePstmt.setTimestamp(7, rs.getTimestamp("created_at"));
                sqlitePstmt.setTimestamp(8, rs.getTimestamp("last_login"));
                sqlitePstmt.executeUpdate();
                System.out.println("User " + rs.getString("username") + " synchronized.");
            }
        }
    }

    /**
     * Synchronizes file data.
     * @param mysqlConn The connection to the MySQL database.
     * @param sqliteConn The connection to the SQLite database.
     * @param userId The ID of the user whose files to synchronize.
     * @throws SQLException if a database error occurs.
     */
    private static void syncFiles(Connection mysqlConn, Connection sqliteConn, int userId) throws SQLException {
        String mysqlQuery = "SELECT * FROM files WHERE owner_id = ?";
        String sqliteInsert = "INSERT OR REPLACE INTO files (file_id, original_name, storage_path, size, owner_id, file_type, created_at, is_encrypted, last_modified, download_count, encryption_key) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement mysqlPstmt = mysqlConn.prepareStatement(mysqlQuery);
             PreparedStatement sqlitePstmt = sqliteConn.prepareStatement(sqliteInsert)) {

            mysqlPstmt.setInt(1, userId);
            ResultSet rs = mysqlPstmt.executeQuery();

            while (rs.next()) {
                sqlitePstmt.setInt(1, rs.getInt("file_id"));
                sqlitePstmt.setString(2, rs.getString("original_name"));
                sqlitePstmt.setString(3, rs.getString("storage_path"));
                sqlitePstmt.setLong(4, rs.getLong("size"));
                sqlitePstmt.setInt(5, rs.getInt("owner_id"));
                sqlitePstmt.setString(6, rs.getString("file_type"));
                sqlitePstmt.setTimestamp(7, rs.getTimestamp("created_at"));
                sqlitePstmt.setBoolean(8, rs.getBoolean("is_encrypted"));
                sqlitePstmt.setTimestamp(9, rs.getTimestamp("last_modified"));
                sqlitePstmt.setInt(10, rs.getInt("download_count"));
                sqlitePstmt.setString(11, rs.getString("encryption_key"));
                sqlitePstmt.executeUpdate();
                System.out.println("File " + rs.getString("original_name") + " synchronized.");
            }
        }
    }

    /**
     * Synchronizes file permissions.
     * @param mysqlConn The connection to the MySQL database.
     * @param sqliteConn The connection to the SQLite database.
     * @param userId The ID of the user whose permissions to synchronize.
     * @throws SQLException if a database error occurs.
     */
    private static void syncFilePermissions(Connection mysqlConn, Connection sqliteConn, int userId) throws SQLException {
        // First, get the file IDs for the user
        String getFileIdsQuery = "SELECT file_id FROM files WHERE owner_id = ?";
        // Then, get permissions for those files
        String mysqlQuery = "SELECT fp.* FROM file_permissions fp JOIN files f ON fp.file_id = f.file_id WHERE f.owner_id = ?";
        String sqliteInsert = "INSERT OR REPLACE INTO file_permissions (permission_id, file_id, user_id, permission_type, granted_at) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement mysqlPstmt = mysqlConn.prepareStatement(mysqlQuery);
             PreparedStatement sqlitePstmt = sqliteConn.prepareStatement(sqliteInsert)) {

            mysqlPstmt.setInt(1, userId);
            ResultSet rs = mysqlPstmt.executeQuery();

            while (rs.next()) {
                sqlitePstmt.setInt(1, rs.getInt("permission_id"));
                sqlitePstmt.setInt(2, rs.getInt("file_id"));
                sqlitePstmt.setInt(3, rs.getInt("user_id"));
                sqlitePstmt.setString(4, rs.getString("permission_type"));
                sqlitePstmt.setTimestamp(5, rs.getTimestamp("granted_at"));
                sqlitePstmt.executeUpdate();
                System.out.println("File permission " + rs.getInt("permission_id") + " synchronized.");
            }
        }
    }

    /**
     * Synchronizes file chunks.
     * @param mysqlConn The connection to the MySQL database.
     * @param sqliteConn The connection to the SQLite database.
     * @param userId The ID of the user whose file chunks to synchronize.
     * @throws SQLException if a database error occurs.
     */
    private static void syncFileChunks(Connection mysqlConn, Connection sqliteConn, int userId) throws SQLException {
        String mysqlQuery = "SELECT fc.* FROM file_chunks fc JOIN files f ON fc.file_id = f.file_id WHERE f.owner_id = ?";
        String sqliteInsert = "INSERT OR REPLACE INTO file_chunks (chunk_id, file_id, chunk_number, server_id, checksum) VALUES (?, ?, ?, ?, ?)";
        
        try (PreparedStatement mysqlPstmt = mysqlConn.prepareStatement(mysqlQuery);
             PreparedStatement sqlitePstmt = sqliteConn.prepareStatement(sqliteInsert)) {

            mysqlPstmt.setInt(1, userId);
            ResultSet rs = mysqlPstmt.executeQuery();

            while (rs.next()) {
                sqlitePstmt.setInt(1, rs.getInt("chunk_id"));
                sqlitePstmt.setInt(2, rs.getInt("file_id"));
                sqlitePstmt.setInt(3, rs.getInt("chunk_number"));
                sqlitePstmt.setInt(4, rs.getInt("server_id"));
                sqlitePstmt.setString(5, rs.getString("checksum"));
                sqlitePstmt.executeUpdate();
                System.out.println("File chunk " + rs.getInt("chunk_id") + " synchronized.");
            }
        }
    }

    /**
     * Synchronizes logs.
     * @param mysqlConn The connection to the MySQL database.
     * @param sqliteConn The connection to the SQLite database.
     * @param userId The ID of the user whose logs to synchronize.
     * @throws SQLException if a database error occurs.
     */
    private static void syncLogs(Connection mysqlConn, Connection sqliteConn, int userId) throws SQLException {
        String mysqlQuery = "SELECT * FROM logs WHERE user_id = ?";
        String sqliteInsert = "INSERT OR REPLACE INTO logs (log_id, user_id, username, action, timestamp, details) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement mysqlPstmt = mysqlConn.prepareStatement(mysqlQuery);
             PreparedStatement sqlitePstmt = sqliteConn.prepareStatement(sqliteInsert)) {

            mysqlPstmt.setInt(1, userId);
            ResultSet rs = mysqlPstmt.executeQuery();

            while (rs.next()) {
                sqlitePstmt.setInt(1, rs.getInt("log_id"));
                sqlitePstmt.setInt(2, rs.getInt("user_id"));
                sqlitePstmt.setString(3, rs.getString("username"));
                sqlitePstmt.setString(4, rs.getString("action"));
                sqlitePstmt.setTimestamp(5, rs.getTimestamp("timestamp"));
                sqlitePstmt.setString(6, rs.getString("details"));
                sqlitePstmt.executeUpdate();
                System.out.println("Log entry " + rs.getInt("log_id") + " synchronized.");
            }
        }
    }
}
