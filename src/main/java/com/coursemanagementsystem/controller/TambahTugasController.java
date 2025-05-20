package com.coursemanagementsystem.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.ObservableList;
import javafx.stage.Stage;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import com.coursemanagementsystem.model.*;

public class TambahTugasController {
    @FXML private TextField txtNama;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> cbPrioritas;
    @FXML private TextField txtMataKuliah;
    @FXML private ComboBox<String> cbTipe;
    @FXML private TextField txtAnggotaKelompok;
    @FXML private ListView<String> lvAnggota;
    @FXML private Button btnTambahAnggota;
    @FXML private Button btnSimpan;
    @FXML private Button btnBatal;
    @FXML private VBox vboxAnggota;
    
    private ObservableList<Tugas> tugasList;
    private ArrayList<String> daftarAnggota = new ArrayList<>();
    private boolean isEditMode = false;
    private Tugas tugasToEdit;
    
    @FXML
    public void initialize() {
        // Setup ComboBox prioritas
        cbPrioritas.getItems().addAll("Rendah", "Menengah", "Tinggi");
        cbPrioritas.setValue("Menengah");
        
        // Setup ComboBox tipe tugas
        cbTipe.getItems().addAll("Individu", "Kelompok");
        cbTipe.setValue("Individu");
        
        // Tentukan visibilitas panel anggota kelompok
        cbTipe.setOnAction(e -> {
            boolean isKelompok = "Kelompok".equals(cbTipe.getValue());
            vboxAnggota.setVisible(isKelompok);
            vboxAnggota.setManaged(isKelompok);
        });
        
        // Button untuk menambahkan anggota ke list
        btnTambahAnggota.setOnAction(e -> {
            String anggota = txtAnggotaKelompok.getText().trim();
            if (!anggota.isEmpty() && !daftarAnggota.contains(anggota)) {
                daftarAnggota.add(anggota);
                lvAnggota.getItems().add(anggota);
                txtAnggotaKelompok.clear();
            }
        });
        
        // Button simpan
        btnSimpan.setOnAction(e -> simpanTugas());
        
        // Button batal
        btnBatal.setOnAction(e -> {
            Stage stage = (Stage) btnBatal.getScene().getWindow();
            stage.close();
        });
        
        // Set visibilitas awal panel anggota
        vboxAnggota.setVisible(false);
        vboxAnggota.setManaged(false);
    }
    
    public void setTugasList(ObservableList<Tugas> tugasList) {
        this.tugasList = tugasList;
    }
    
    public void setEditMode(boolean editMode) {
        this.isEditMode = editMode;
    }
    
    public void setTugas(Tugas tugas) {
        this.tugasToEdit = tugas;
        
        // Isi form dengan data tugas
        txtNama.setText(tugas.getNama());
        
        try {
            LocalDate date = LocalDate.parse(tugas.getDeadline());
            datePicker.setValue(date);
        } catch (Exception e) {
            System.err.println("Format tanggal tidak valid: " + tugas.getDeadline());
        }
        
        cbPrioritas.setValue(tugas.getPrioritas());
        txtMataKuliah.setText(tugas.getMataKuliah());
        cbTipe.setValue(tugas.getTipe());
        
        // Jika tugas kelompok, tambahkan daftar anggota
        if (tugas instanceof TugasKelompok) {
            vboxAnggota.setVisible(true);
            vboxAnggota.setManaged(true);
            
            TugasKelompok tugasKelompok = (TugasKelompok) tugas;
            for (Mahasiswa anggota : tugasKelompok.getAnggota()) {
                daftarAnggota.add(anggota.getNama());
                lvAnggota.getItems().add(anggota.getNama());
            }
        }
    }
    
    private void simpanTugas() {
        String nama = txtNama.getText().trim();
        LocalDate deadline = datePicker.getValue();
        String prioritas = cbPrioritas.getValue();
        String mataKuliah = txtMataKuliah.getText().trim();
        String tipe = cbTipe.getValue();
        
        // Validasi input
        if (nama.isEmpty() || deadline == null || mataKuliah.isEmpty()) {
            showAlert("Data tidak lengkap. Mohon isi semua field yang diperlukan.");
            return;
        }
        
        // Format tanggal
        String deadlineStr = deadline.format(DateTimeFormatter.ISO_DATE);
        
        if (isEditMode && tugasToEdit != null) {
            // Mode edit: update tugas yang ada
            tugasToEdit.setNama(nama);
            tugasToEdit.setDeadline(deadlineStr);
            tugasToEdit.setPrioritas(prioritas);
            tugasToEdit.setMataKuliah(mataKuliah);
            
            // Jika tipe berubah, perlu buat objek baru
            if (!tugasToEdit.getTipe().equals(tipe)) {
                int index = tugasList.indexOf(tugasToEdit);
                tugasList.remove(tugasToEdit);
                
                if ("Kelompok".equals(tipe)) {
                    TugasKelompok tugasBaru = new TugasKelompok(nama, deadlineStr, prioritas, mataKuliah);
                    for (String anggotaNama : daftarAnggota) {
                        tugasBaru.tambahAnggota(new Mahasiswa(anggotaNama));
                    }
                    tugasList.add(index, tugasBaru);
                } else {
                    tugasList.add(index, new TugasIndividu(nama, deadlineStr, prioritas, mataKuliah));
                }
            } else if (tugasToEdit instanceof TugasKelompok) {
                // Update daftar anggota jika masih tugas kelompok
                TugasKelompok tugasKelompok = (TugasKelompok) tugasToEdit;
                tugasKelompok.getAnggota().clear();
                for (String anggotaNama : daftarAnggota) {
                    tugasKelompok.tambahAnggota(new Mahasiswa(anggotaNama));
                }
            }
        } else {
            // Mode tambah: buat tugas baru
            if ("Kelompok".equals(tipe)) {
                TugasKelompok tugasBaru = new TugasKelompok(nama, deadlineStr, prioritas, mataKuliah);
                for (String anggotaNama : daftarAnggota) {
                    tugasBaru.tambahAnggota(new Mahasiswa(anggotaNama));
                }
                tugasList.add(tugasBaru);
            } else {
                tugasList.add(new TugasIndividu(nama, deadlineStr, prioritas, mataKuliah));
            }
        }
        
        // Tutup form
        Stage stage = (Stage) btnSimpan.getScene().getWindow();
        stage.close();
    }
    
    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Peringatan");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}