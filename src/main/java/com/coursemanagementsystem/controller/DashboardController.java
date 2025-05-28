package com.coursemanagementsystem.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.*;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.event.ActionEvent;
import javafx.scene.layout.HBox;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import com.coursemanagementsystem.model.*;

public class DashboardController {
    @FXML
    private TableView<Tugas> tableTugas;
    @FXML
    private TableColumn<Tugas, String> colNama, colDeadline, colPrioritas, colMataKuliah, colTipe;
    @FXML
    private TableColumn<Tugas, Void> colAksi;
    @FXML
    private Button btnTambah;

    private ObservableList<Tugas> tugasList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Konfigurasi kolom tabel
        colNama.setCellValueFactory(new PropertyValueFactory<>("nama"));
        colDeadline.setCellValueFactory(new PropertyValueFactory<>("deadline"));
        colPrioritas.setCellValueFactory(new PropertyValueFactory<>("prioritas"));
        colMataKuliah.setCellValueFactory(new PropertyValueFactory<>("mataKuliah"));
        colTipe.setCellValueFactory(new PropertyValueFactory<>("tipe"));

        // Tambahkan kolom Aksi dengan tombol Edit dan Hapus
        setupAksiColumn();

        // Set data ke tabel
        tableTugas.setItems(tugasList);

        // Event handler untuk tombol Tambah
        btnTambah.setOnAction(e -> tambahTugas());

        // Tambahkan beberapa data contoh
        tambahDataContoh();

        // Cek notifikasi tugas mendekati deadline
        cekNotifikasiDeadline();
    }

    private void setupAksiColumn() {
        Callback<TableColumn<Tugas, Void>, TableCell<Tugas, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<Tugas, Void> call(final TableColumn<Tugas, Void> param) {
                return new TableCell<>() {
                    private final Button btnEdit = new Button("Edit");
                    private final Button btnHapus = new Button("Hapus");
                    private final HBox pane = new HBox(5, btnEdit, btnHapus);

                    {
                        btnEdit.getStyleClass().add("edit-btn");
                        btnHapus.getStyleClass().add("delete-btn");

                        btnEdit.setOnAction((ActionEvent event) -> {
                            Tugas tugas = getTableView().getItems().get(getIndex());
                            editTugas(tugas);
                        });

                        btnHapus.setOnAction((ActionEvent event) -> {
                            Tugas tugas = getTableView().getItems().get(getIndex());
                            hapusTugas(tugas);
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(pane);
                        }
                    }
                };
            }
        };

        colAksi.setCellFactory(cellFactory);
    }

    private void tambahDataContoh() {
        // Tambahkan beberapa contoh tugas untuk demo
        tugasList.add(new TugasIndividu("Membuat ERD", "2025-05-27", "Tinggi", "Basis Data"));
        tugasList.add(new TugasIndividu("Laporan Praktikum", "2025-05-25", "Menengah", "Struktur Data"));

        TugasKelompok tugasKelompok = new TugasKelompok("Proyek Akhir", "2025-06-15", "Tinggi", "Pemrograman OOP");
        tugasKelompok.tambahAnggota(new Mahasiswa("Budi"));
        tugasKelompok.tambahAnggota(new Mahasiswa("Ani"));
        tugasList.add(tugasKelompok);
    }

    private void cekNotifikasiDeadline() {
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (Tugas tugas : tugasList) {
            try {
                LocalDate deadline = LocalDate.parse(tugas.getDeadline(), formatter);
                long daysDiff = ChronoUnit.DAYS.between(today, deadline);

                if (daysDiff <= 3 && daysDiff >= 0) {
                    if (tugas instanceof Notifikasi) {
                        ((Notifikasi) tugas).kirimPengingat(tugas);

                        // Tampilkan dialog notifikasi
                        Alert alert = new Alert(Alert.AlertType.WARNING);
                        alert.setTitle("Pengingat Tugas");
                        alert.setHeaderText("Deadline Mendekati");
                        alert.setContentText(
                                "Tugas \"" + tugas.getNama() + "\" akan berakhir dalam " + daysDiff + " hari lagi!");
                        alert.showAndWait();
                    }
                }
            } catch (Exception e) {
                System.err.println("Format tanggal tidak valid: " + tugas.getDeadline());
            }
        }
    }

    private void tambahTugas() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/coursemanagementsystem/tambah_tugas.fxml"));
            Parent root = loader.load();

            TambahTugasController controller = loader.getController();
            controller.setTugasList(tugasList);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Tambah Tugas Baru");
            Scene scene = new Scene(root);
            scene.getStylesheets()
                    .add(getClass().getResource("/com/coursemanagementsystem/tailwindfx.css").toExternalForm());
            stage.setScene(scene);
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Gagal Membuka Form");
            alert.setContentText("Tidak dapat membuka form tambah tugas: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private void editTugas(Tugas tugas) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/coursemanagementsystem/tambah_tugas.fxml"));
            Parent root = loader.load();

            TambahTugasController controller = loader.getController();
            controller.setTugasList(tugasList);
            controller.setEditMode(true);
            controller.setTugas(tugas);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Edit Tugas");
            Scene scene = new Scene(root);
            scene.getStylesheets()
                    .add(getClass().getResource("/com/coursemanagementsystem/tailwindfx.css").toExternalForm());
            stage.setScene(scene);
            stage.showAndWait();

            // ⬇️ Tambahan penting agar TableView menampilkan data yang diperbarui
            tableTugas.refresh();

        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Gagal Membuka Form");
            alert.setContentText("Tidak dapat membuka form edit tugas: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private void hapusTugas(Tugas tugas) {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Konfirmasi Hapus");
        confirmDialog.setHeaderText("Hapus Tugas");
        confirmDialog.setContentText("Apakah Anda yakin ingin menghapus tugas \"" + tugas.getNama() + "\"?");

        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            tugasList.remove(tugas);
        }
    }
}