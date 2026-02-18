package tn.esprit.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyConnection {

    private static final String URL = "jdbc:mysql://localhost:3306/agrinova";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    private static MyConnection instance;

    private MyConnection() {}

    public static MyConnection getInstance() {
        if (instance == null) {
            instance = new MyConnection();
        }
        return instance;
    }

    public Connection getCnx() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
