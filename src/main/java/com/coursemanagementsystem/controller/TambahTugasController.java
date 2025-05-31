package com.coursemanagementsystem.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.Stage;
import javafx.scene.layout.VBox;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import com.coursemanagementsystem.database.DatabaseConnection;
import com.coursemanagementsystem.model.Mahasiswa;

public class TambahTugasController {
    @FXML private TextField txtJudul, txtMataKuliah, txtNamaAnggota, txtNimAnggota;
    @FXML private TextArea txtDeskripsi;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> cbPrioritas, cbTipe;
    @FXML private Button btnSimpan, btnBatal, btnTambahAnggota;
    @FXML private VBox vboxAnggotaKelompok;
    @FXML private ListView<String> listViewAnggota;

    private Runnable onTugasAdded;
    private ObservableList<Mahasiswa> anggotaKelompok = FXCollections.observableArrayList();
    private ObservableList<String> anggotaDisplayList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Setup combo boxes
        cbPrioritas.getItems().addAll("Rendah", "Menengah", "Tinggi");
        cbTipe.getItems().addAll("Individu", "Kelompok");
        cbPrioritas.getSelectionModel().clearSelection();
        cbTipe.getSelectionModel().clearSelection();
        
        // Setup ListView
        listViewAnggota.setItems(anggotaDisplayList);

        // Event handlers
        btnSimpan.setOnAction(_ -> simpanTugas());
        btnBatal.setOnAction(_ -> close());
        btnTambahAnggota.setOnAction(_ -> tambahAnggotaKelompok());

        // Show/hide anggota section based on tipe selection
        cbTipe.valueProperty().addListener((_, _, newVal) -> {
            boolean isKelompok = "Kelompok".equals(newVal);
            vboxAnggotaKelompok.setVisible(isKelompok);
            vboxAnggotaKelompok.setManaged(isKelompok);
            
            if (!isKelompok) {
                // Clear anggota list if switching from Kelompok to Individu
                anggotaKelompok.clear();
                anggotaDisplayList.clear();
            }
        });

        // Setup ListView context menu for removing members
        setupAnggotaListView();
    }

    private void setupAnggotaListView() {
        listViewAnggota.setCellFactory(_ -> {
            ListCell<String> cell = new ListCell<String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        setText(item);
                    }
                }
            };

            // Add context menu for removing members
            ContextMenu contextMenu = new ContextMenu();
            MenuItem removeItem = new MenuItem("🗑️ Hapus Anggota");
            removeItem.setOnAction(_ -> {
                int index = cell.getIndex();
                if (index >= 0 && index < anggotaKelompok.size()) {
                    String memberName = anggotaKelompok.get(index).getNama();
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Konfirmasi Hapus");
                    confirm.setHeaderText("Hapus Anggota Kelompok");
                    confirm.setContentText("Apakah Anda yakin ingin menghapus " + memberName + " dari kelompok?");
                    
                    if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                        anggotaKelompok.remove(index);
                        anggotaDisplayList.remove(index);
                    }
                }
            });
            contextMenu.getItems().add(removeItem);

            cell.emptyProperty().addListener((_, _, isNowEmpty) -> {
                if (isNowEmpty) {
                    cell.setContextMenu(null);
                } else {
                    cell.setContextMenu(contextMenu);
                }
            });

            return cell;
        });
    }

    private void tambahAnggotaKelompok() {
        String nama = txtNamaAnggota.getText().trim();
        String nim = txtNimAnggota.getText().trim();

        if (nama.isEmpty() || nim.isEmpty()) {
            showAlert("Validasi", "Nama dan NIM anggota harus diisi!", Alert.AlertType.WARNING);
            return;
        }

        // Check if NIM already exists
        boolean nimExists = anggotaKelompok.stream()
                .anyMatch(anggota -> anggota.getNim().equals(nim));
        
        if (nimExists) {
            showAlert("Validasi", "NIM " + nim + " sudah ada dalam kelompok!", Alert.AlertType.WARNING);
            return;
        }

        // Add member
        Mahasiswa anggota = new Mahasiswa(0, nama, nim, "", "");
        anggotaKelompok.add(anggota);
        anggotaDisplayList.add("👤 " + nama + " (" + nim + ")");

        // Clear input fields
        txtNamaAnggota.clear();
        txtNimAnggota.clear();
        txtNamaAnggota.requestFocus();

        showAlert("Sukses", "Anggota " + nama + " berhasil ditambahkan!", Alert.AlertType.INFORMATION);
    }

    private void simpanTugas() {
        // Validate basic fields
        String judul = txtJudul.getText().trim();
        String deskripsi = txtDeskripsi.getText() == null ? "" : txtDeskripsi.getText().trim();
        LocalDate deadline = datePicker.getValue();
        String prioritas = cbPrioritas.getValue();
        String mataKuliah = txtMataKuliah.getText().trim();
        String tipe = cbTipe.getValue();

        if (judul.isEmpty() || deadline == null || prioritas == null || prioritas.isEmpty() ||
                mataKuliah.isEmpty() || tipe == null || tipe.isEmpty()) {
            showAlert("Validasi", "Semua field dengan tanda (*) wajib diisi!", Alert.AlertType.WARNING);
            return;
        }

        // Additional validation for group tasks
        if ("Kelompok".equals(tipe) && anggotaKelompok.isEmpty()) {
            showAlert("Validasi", "Tugas kelompok harus memiliki minimal 1 anggota!", Alert.AlertType.WARNING);
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false); // Start transaction
            
            // Insert tugas
            String sqlTugas = "INSERT INTO tugas (judul, deskripsi, deadline, prioritas, mata_kuliah, tipe) VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement stTugas = conn.prepareStatement(sqlTugas, PreparedStatement.RETURN_GENERATED_KEYS);
            stTugas.setString(1, judul);
            stTugas.setString(2, deskripsi);
            stTugas.setDate(3, java.sql.Date.valueOf(deadline));
            stTugas.setString(4, prioritas);
            stTugas.setString(5, mataKuliah);
            stTugas.setString(6, tipe);
            stTugas.executeUpdate();

            // Get generated tugas ID
            ResultSet generatedKeys = stTugas.getGeneratedKeys();
            int tugasId = 0;
            if (generatedKeys.next()) {
                tugasId = generatedKeys.getInt(1);
            }

            // Insert anggota kelompok if it's a group task
            if ("Kelompok".equals(tipe) && !anggotaKelompok.isEmpty()) {
                String sqlAnggota = "INSERT INTO anggota_kelompok (tugas_id, nama, nim) VALUES (?, ?, ?)";
                PreparedStatement stAnggota = conn.prepareStatement(sqlAnggota);
                
                for (Mahasiswa anggota : anggotaKelompok) {
                    stAnggota.setInt(1, tugasId);
                    stAnggota.setString(2, anggota.getNama());
                    stAnggota.setString(3, anggota.getNim());
                    stAnggota.addBatch();
                }
                stAnggota.executeBatch();
            }

            conn.commit(); // Commit transaction
            
            String successMessage = "Tugas berhasil ditambahkan!";
            if ("Kelompok".equals(tipe)) {
                successMessage += "\nJumlah anggota kelompok: " + anggotaKelompok.size();
            }
            
            showAlert("Sukses", successMessage, Alert.AlertType.INFORMATION);
            
            if (onTugasAdded != null) onTugasAdded.run();
            close();
            
        } catch (Exception ex) {
            showAlert("Error", "Gagal menambah tugas: " + ex.getMessage(), Alert.AlertType.ERROR);
            ex.printStackTrace();
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