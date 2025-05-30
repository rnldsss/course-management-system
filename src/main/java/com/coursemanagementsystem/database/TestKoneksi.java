package com.coursemanagementsystem.database;

import java.sql.Connection;
import java.sql.SQLException;

public class TestKoneksi {
    public static void main(String[] args) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn != null) {
                System.out.println("✅ Koneksi ke database berhasil!");
            } else {
                System.out.println("❌ Gagal: objek Connection null.");
            }
        } catch (SQLException e) {
            System.out.println("❌ Terjadi kesalahan koneksi:");
            e.printStackTrace();
        }
    }
}
