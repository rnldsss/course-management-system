package com.coursemanagementsystem.controller;

import com.coursemanagementsystem.MainApp;
import com.coursemanagementsystem.database.DatabaseConnection;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoginController {
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;

    @FXML
    private void initialize() {
        loginButton.setOnAction(e -> handleLogin());
    }

    private void handleLogin() {
        String email = emailField.getText();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Email dan password wajib diisi");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT * FROM mahasiswa WHERE email = ? AND password = ?";
            PreparedStatement st = conn.prepareStatement(sql);
            st.setString(1, email);
            st.setString(2, password);
            ResultSet rs = st.executeQuery();
            if(rs.next()) {
                MainApp.showDashboard();
            } else {
                errorLabel.setText("Email atau password salah");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            errorLabel.setText("Gagal koneksi ke database");
        }
    }
}