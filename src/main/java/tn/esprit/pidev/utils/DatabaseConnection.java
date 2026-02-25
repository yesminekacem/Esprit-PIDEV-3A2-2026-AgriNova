package tn.esprit.pidev.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Singleton class for managing database connection to MySQL.
 * Uses double-checked locking for thread-safe lazy initialization.
 * PIDEV - AgriRent System
 */
public class DatabaseConnection {
    private static volatile DatabaseConnection instance;
    private static final String URL = "jdbc:mysql://localhost:3306/agrirent_db";
    private static final String USER = "root";
    private static final String PASSWORD = "";
    private Connection connection;

    /**
     * Private constructor - prevents instantiation from outside (Singleton pattern).
     */
    private DatabaseConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("[DatabaseConnection] MySQL Driver loaded successfully.");
        } catch (ClassNotFoundException e) {
            System.err.println("[DatabaseConnection] ERROR: MySQL Driver not found. " + e.getMessage());
        }
    }

    /**
     * Returns the single instance. Double-checked locking for thread safety.
     *
     * @return the DatabaseConnection instance
     */
    public static DatabaseConnection getInstance() {
        if (instance == null) {
            synchronized (DatabaseConnection.class) {
                if (instance == null) {
                    instance = new DatabaseConnection();
                }
            }
        }
        return instance;
    }

    /**
     * Gets a connection to the database. Creates new connection if not open.
     *
     * @return Connection object, or null on failure
     */
    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("[DatabaseConnection] Connected to agrirent_db.");
            }
        } catch (SQLException e) {
            System.err.println("[DatabaseConnection] ERROR getting connection: " + e.getMessage());
        }
        return connection;
    }

    /**
     * Closes the database connection.
     */
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                connection = null;
                System.out.println("[DatabaseConnection] Connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("[DatabaseConnection] ERROR closing connection: " + e.getMessage());
        }
    }
}
