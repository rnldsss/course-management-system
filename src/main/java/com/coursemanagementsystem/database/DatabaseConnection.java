package com.coursemanagementsystem.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/coursemanagementdb"; // Ganti sesuai nama DB kamu
    private static final String USER = "root"; // Ganti jika username MySQL kamu berbeda
    private static final String PASSWORD = ""; // Ganti jika MySQL kamu pakai password

    private static Connection connection;

    public static Connection getConnection() {
        if (connection == null) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver"); // Load driver MySQL
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("✅ Koneksi ke database berhasil.");
            } catch (ClassNotFoundException e) {
                System.out.println("❌ JDBC Driver tidak ditemukan.");
                e.printStackTrace();
            } catch (SQLException e) {
                System.out.println("❌ Gagal menghubungkan ke database.");
                e.printStackTrace();
            }
        }
        return connection;
    }

    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("🔌 Koneksi database ditutup.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
