package tn.esprit.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DbConnect {

    private static DbConnect instance;
    private Connection connection;

    private final String url = "jdbc:mysql://127.0.0.1:3306/agrinova?useSSL=false&serverTimezone=UTC";
    private final String user = "root";
    private final String pass = "";

    private DbConnect() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC Driver not found", e);
        }
    }

    public static synchronized DbConnect getInstance() {
        if (instance == null) {
            instance = new DbConnect();
        }
        return instance;
    }

    public synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(url, user, pass);
            System.out.println("✅ DB connected to forum_db");
        }
        return connection;
    }
}
