package tn.esprit.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyConnection {
    private static MyConnection instance;
    private Connection cnx;

    private final String URL = "jdbc:mysql://127.0.0.1:3306/agrinova?useSSL=false&serverTimezone=UTC";
    private final String USER = "root";
    private final String PASS = ""; // put your password if you have one

    private MyConnection() {
        try {
            cnx = DriverManager.getConnection(URL, USER, PASS);
            System.out.println("✅ DB connected");
        } catch (SQLException e) {
            System.out.println("❌ DB connection failed: " + e.getMessage());
        }
    }

    public static MyConnection getInstance() {
        if (instance == null) instance = new MyConnection();
        return instance;
    }

    public Connection getCnx() {
        return cnx;
    }
}
