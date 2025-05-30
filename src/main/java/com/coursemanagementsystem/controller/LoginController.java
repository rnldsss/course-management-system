package com.coursemanagementsystem.controller;

import com.coursemanagementsystem.database.DatabaseConnection;
import com.coursemanagementsystem.model.Mahasiswa;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
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
                // Buat object Mahasiswa
                Mahasiswa mahasiswa = new Mahasiswa(
                    rs.getInt("id"),
                    rs.getString("nama"),
                    rs.getString("nim"),
                    rs.getString("email"),
                    null
                );
                
                // Pass ke dashboard
                showDashboardWithMahasiswa(mahasiswa);
            } else {
                errorLabel.setText("Email atau password salah");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            errorLabel.setText("Gagal koneksi ke database");
        }
    }
    
    private void showDashboardWithMahasiswa(Mahasiswa mahasiswa) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/coursemanagementsystem/dashboard.fxml"));
        Parent root = loader.load();
        
        // Get controller dan set mahasiswa
        DashboardController controller = loader.getController();
        controller.setMahasiswa(mahasiswa);
        
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/com/coursemanagementsystem/tailwindfx.css").toExternalForm());
        
        Stage stage = (Stage) loginButton.getScene().getWindow();
        stage.setTitle("Dashboard");
        stage.setScene(scene);
        stage.show();
    }
}