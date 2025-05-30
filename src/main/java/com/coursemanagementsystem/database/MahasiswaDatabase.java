package com.coursemanagementsystem.database;

import com.coursemanagementsystem.model.Mahasiswa;
import java.sql.*;

public class MahasiswaDatabase {
    public static Mahasiswa login(String email, String password) throws SQLException {
        String sql = "SELECT * FROM mahasiswa WHERE email = ? AND password = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new Mahasiswa(
                        rs.getInt("id"),
                        rs.getString("nama"),
                        rs.getString("nim"),
                        rs.getString("email"),
                        null
                );
            }
        }
        return null;
    }
}