package tn.esprit.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyConnection {
    private static MyConnection instance;
    private Connection cnx;

    private final String URL = "jdbc:mysql://127.0.0.1:3306/agrinova?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&autoReconnect=true";
    private final String USER = "root";
    private final String PASS = ""; // put your password if you have one

    private MyConnection() {
        connect();
    }

    private void connect() {
        try {
            cnx = DriverManager.getConnection(URL, USER, PASS);
            System.out.println("✅ DB connected");
        } catch (SQLException e) {
            System.out.println("❌ DB connection failed: " + e.getMessage());
            cnx = null;
        }
    }

    public static MyConnection getInstance() {
        if (instance == null) {
            instance = new MyConnection();
        }
        return instance;
    }

    public Connection getCnx() throws SQLException {
        // Auto-reconnect if closed or null
        if (cnx == null || cnx.isClosed()) {
            System.out.println("🔄 Reconnecting to DB...");
            connect();
        }
        return cnx;
    }
}
