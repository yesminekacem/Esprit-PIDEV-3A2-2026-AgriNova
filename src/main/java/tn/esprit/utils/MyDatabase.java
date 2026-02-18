package tn.esprit.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDatabase {
    private final String URL = "jdbc:mysql://localhost:3306/agrinova";
    private final String USERNAME = "root";
    private final String PWD = "";
    private Connection conx;
    public static MyDatabase instance;

    private MyDatabase() {
        try {
            conx = DriverManager.getConnection(URL, USERNAME, PWD);
            System.out.println("✓ Database connection established!");
        } catch (SQLException e) {
            System.out.println("✗ Connection failed: " + e.getMessage());
        }
    }

    public static MyDatabase getInstance() {
        if (instance == null) {
            instance = new MyDatabase();
        }
        return instance;
    }

    public Connection getConx() {
        return conx;
    }
}
