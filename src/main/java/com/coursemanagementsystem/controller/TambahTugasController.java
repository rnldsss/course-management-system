package com.coursemanagementsystem.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import com.coursemanagementsystem.database.DatabaseConnection;

public class TambahTugasController {
    @FXML private TextField txtJudul, txtMataKuliah;
    @FXML private TextArea txtDeskripsi;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> cbPrioritas, cbTipe;
    @FXML private Button btnSimpan, btnBatal;

    private Runnable onTugasAdded;

    @FXML
    public void initialize() {
        cbPrioritas.getItems().addAll("Rendah", "Menengah", "Tinggi");
        cbTipe.getItems().addAll("Individu", "Kelompok");
        cbPrioritas.getSelectionModel().clearSelection();
        cbTipe.getSelectionModel().clearSelection();

        btnSimpan.setOnAction(e -> simpanTugas());
        btnBatal.setOnAction(e -> close());
    }

    private void simpanTugas() {
        String judul = txtJudul.getText().trim();
        String deskripsi = txtDeskripsi.getText() == null ? "" : txtDeskripsi.getText().trim();
        LocalDate deadline = datePicker.getValue();
        String prioritas = cbPrioritas.getValue();
        String mataKuliah = txtMataKuliah.getText().trim();
        String tipe = cbTipe.getValue();

        if (judul.isEmpty() || deadline == null || prioritas == null || prioritas.isEmpty() ||
                mataKuliah.isEmpty() || tipe == null || tipe.isEmpty()) {
            showAlert("Validasi", "Semua field dengan tanda * wajib diisi!", Alert.AlertType.WARNING);
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "INSERT INTO tugas (judul, deskripsi, deadline, prioritas, mata_kuliah, tipe) VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement st = conn.prepareStatement(sql);
            st.setString(1, judul);
            st.setString(2, deskripsi);
            st.setDate(3, java.sql.Date.valueOf(deadline));
            st.setString(4, prioritas);
            st.setString(5, mataKuliah);
            st.setString(6, tipe);
            st.executeUpdate();

            showAlert("Sukses", "Tugas berhasil ditambahkan.", Alert.AlertType.INFORMATION);
            if (onTugasAdded != null) onTugasAdded.run();
            close();
        } catch (Exception ex) {
            showAlert("Error", "Gagal menambah tugas: " + ex.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void close() {
        ((Stage) btnBatal.getScene().getWindow()).close();
    }

    private void showAlert(String title, String msg, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    public void setOnTugasAdded(Runnable callback) {
        this.onTugasAdded = callback;
    }
}