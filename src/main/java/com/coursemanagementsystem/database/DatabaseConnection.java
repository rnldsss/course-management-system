package com.coursemanagementsystem.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/coursemanagementdb";
    private static final String DB_USER = "root"; // ganti sesuai usermu
    private static final String DB_PASS = "";     // ganti sesuai passwordmu

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASS);
    }
}