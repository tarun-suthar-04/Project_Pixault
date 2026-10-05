package com.pixault.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Database connection manager for Pixault.
 *
 * <p>
 * Provides a simple singleton-based JDBC connection to SQLite.
 */
public final class DBConnection {

    private static final Logger log = LoggerFactory.getLogger(DBConnection.class);

    private static final String JDBC_URL;

    static {
        String appDataPath = System.getenv("APPDATA");
        if (appDataPath == null || appDataPath.isBlank()) {
            appDataPath = System.getProperty("user.home");
        }
        File dbFile = new File(new File(appDataPath, "Pixault"), "pixault.db");
        JDBC_URL = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        log.info("Configured SQLite JDBC URL: {}", JDBC_URL);
    }

    private DBConnection() {
        throw new UnsupportedOperationException("DBConnection is a utility class.");
    }

    /**
     * Returns a new JDBC {@link Connection} to the Pixault SQLite database.
     *
     * <p>
     * Callers are responsible for closing the connection (use try-with-resources).
     *
     * @return An open JDBC connection
     * @throws SQLException if connection cannot be established
     */
    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection(JDBC_URL);
            conn.setAutoCommit(true);
            
            // Enforce foreign key constraints
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON;");
            }
            
            return conn;
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC driver not found.", e);
        }
    }

    /**
     * Tests the database connection.
     *
     * @return true if connection is healthy
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn.isValid(5);
        } catch (SQLException e) {
            log.error("DB connection test failed: {}", e.getMessage());
            return false;
        }
    }

}
