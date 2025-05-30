package com.coursemanagementsystem.controller;

import com.coursemanagementsystem.database.DatabaseConnection;
import com.coursemanagementsystem.model.Mahasiswa;
import com.coursemanagementsystem.model.Tugas;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.*;
import java.time.LocalDate;

public class TambahTugasController {
    @FXML private TextField txtJudul;
    @FXML private TextArea txtDeskripsi;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> cbPrioritas;
    @FXML private TextField txtMataKuliah;
    @FXML private ComboBox<String> cbTipe;
    @FXML private Button btnSimpan;
    @FXML private Button btnBatal;

    private Mahasiswa mahasiswa;
    private Tugas tugas;
    private boolean editMode = false;
    private Runnable onTugasSaved;

    public void setMahasiswa(Mahasiswa mahasiswa) {
        this.mahasiswa = mahasiswa;
    }

    public void setTugas(Tugas tugas) {
        this.tugas = tugas;
        if (tugas != null) {
            txtJudul.setText(tugas.getJudul());
            txtDeskripsi.setText(tugas.getDeskripsi());
            datePicker.setValue(LocalDate.parse(tugas.getDeadline()));
            cbPrioritas.setValue(tugas.getPrioritas());
            txtMataKuliah.setText(tugas.getMataKuliah());
            cbTipe.setValue(tugas.getTipe());
        }
    }

    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
        btnSimpan.setText(editMode ? "Update" : "Simpan");
    }

    public void setOnTugasSaved(Runnable callback) {
        this.onTugasSaved = callback;
    }

    @FXML
    public void initialize() {
        cbPrioritas.getItems().addAll("Rendah", "Menengah", "Tinggi");
        cbTipe.getItems().addAll("Individu", "Kelompok");
        cbPrioritas.setValue("Menengah");
        cbTipe.setValue("Individu");

        btnBatal.setOnAction(e -> ((Stage) btnBatal.getScene().getWindow()).close());
        btnSimpan.setOnAction(e -> handleSimpan());
    }

    private void handleSimpan() {
        String judul = txtJudul.getText().trim();
        String deskripsi = txtDeskripsi.getText().trim();
        LocalDate deadline = datePicker.getValue();
        String prioritas = cbPrioritas.getValue();
        String mataKuliah = txtMataKuliah.getText().trim();
        String tipe = cbTipe.getValue();

        if (judul.isEmpty() || deadline == null || prioritas == null || mataKuliah.isEmpty() || tipe == null) {
            showAlert("Semua field wajib diisi!", Alert.AlertType.ERROR);
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            if (editMode && tugas != null) {
                String sql = "UPDATE tugas SET judul=?, deskripsi=?, deadline=?, prioritas=?, mata_kuliah=?, tipe=? WHERE id=?";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, judul);
                stmt.setString(2, deskripsi);
                stmt.setString(3, deadline.toString());
                stmt.setString(4, prioritas);
                stmt.setString(5, mataKuliah);
                stmt.setString(6, tipe);
                stmt.setInt(7, tugas.getId());
                stmt.executeUpdate();
            } else {
                String sql = "INSERT INTO tugas (judul, deskripsi, deadline, prioritas, mata_kuliah, tipe) VALUES (?, ?, ?, ?, ?, ?)";
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                stmt.setString(1, judul);
                stmt.setString(2, deskripsi);
                stmt.setString(3, deadline.toString());
                stmt.setString(4, prioritas);
                stmt.setString(5, mataKuliah);
                stmt.setString(6, tipe);
                stmt.executeUpdate();
                ResultSet rs = stmt.getGeneratedKeys();
                int tugasId = -1;
                if (rs.next()) tugasId = rs.getInt(1);

                // Assign ke mahasiswa yang login (tabel tugas_mahasiswa)
                if (tugasId > 0 && mahasiswa != null) {
                    String sql2 = "INSERT INTO tugas_mahasiswa (id_tugas, id_mahasiswa) VALUES (?, ?)";
                    PreparedStatement stmt2 = conn.prepareStatement(sql2);
                    stmt2.setInt(1, tugasId);
                    stmt2.setInt(2, mahasiswa.getId());
                    stmt2.executeUpdate();
                }
            }
            if (onTugasSaved != null) onTugasSaved.run();
            ((Stage) btnSimpan.getScene().getWindow()).close();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Gagal menyimpan tugas! " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void showAlert(String msg, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(type == Alert.AlertType.ERROR ? "Error" : "Info");
        alert.setContentText(msg);
        alert.showAndWait();
    }
}