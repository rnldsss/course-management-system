package com.coursemanagementsystem.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.*;
import javafx.collections.transformation.FilteredList;
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

    @FXML
    private ComboBox<String> comboFilter; // Tambahan ComboBox filter

    @FXML
    private TextField searchField;

    private ObservableList<Tugas> tugasList = FXCollections.observableArrayList();
    private FilteredList<Tugas> filteredTugas;

    @FXML
    public void initialize() {
        // Inisialisasi comboFilter dengan pilihan filter
        comboFilter.getItems().addAll("Semua", "Mendesak", "Sedang dikerjakan", "Selesai");
        comboFilter.getSelectionModel().selectFirst(); // Default pilih "Semua"

        // Konfigurasi kolom tabel
        colNama.setCellValueFactory(new PropertyValueFactory<>("nama"));
        colDeadline.setCellValueFactory(new PropertyValueFactory<>("deadline"));
        colPrioritas.setCellValueFactory(new PropertyValueFactory<>("prioritas"));
        colMataKuliah.setCellValueFactory(new PropertyValueFactory<>("mataKuliah"));
        colTipe.setCellValueFactory(new PropertyValueFactory<>("tipe"));

        // Setup kolom Aksi
        setupAksiColumn();

        // Inisialisasi filtered list untuk search dan filter
        filteredTugas = new FilteredList<>(tugasList, p -> true);
        tableTugas.setItems(filteredTugas);

        // Listener searchField untuk filter realtime
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterTugas());

        // Listener comboFilter untuk filter berdasarkan status
        comboFilter.valueProperty().addListener((obs, oldVal, newVal) -> filterTugas());

        // Tambahkan data contoh
        tambahDataContoh();

        // Cek notifikasi deadline
        cekNotifikasiDeadline();
    }

    // Method untuk filter data tugas berdasarkan searchField dan comboFilter
    private void filterTugas() {
        String searchText = searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();
        String filterStatus = comboFilter.getValue();

        filteredTugas.setPredicate(tugas -> {
            // Filter berdasarkan search text (nama, mata kuliah, prioritas)
            boolean matchesSearch = searchText.isEmpty() ||
                    tugas.getNama().toLowerCase().contains(searchText) ||
                    tugas.getMataKuliah().toLowerCase().contains(searchText) ||
                    tugas.getPrioritas().toLowerCase().contains(searchText);

            if (!matchesSearch)
                return false;

            // Filter berdasarkan status tugas
            if (filterStatus == null || filterStatus.equals("Semua")) {
                return true; // tampilkan semua
            }

            switch (filterStatus) {
                case "Mendesak":
                    return isMendesak(tugas);
                case "Sedang dikerjakan":
                    return isSedangDikerjakan(tugas);
                case "Selesai":
                    return isSelesai(tugas);
                default:
                    return true;
            }
        });
    }

    // Contoh metode logika status, sesuaikan dengan atribut model tugas kamu
    private boolean isMendesak(Tugas tugas) {
        try {
            LocalDate today = LocalDate.now();
            LocalDate deadline = LocalDate.parse(tugas.getDeadline());
            long daysDiff = ChronoUnit.DAYS.between(today, deadline);
            // Mendesak: deadline 3 hari ke depan dan belum selesai
            return daysDiff >= 0 && daysDiff <= 3 && !isSelesai(tugas);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isSedangDikerjakan(Tugas tugas) {
        // Contoh: prioritas bukan "Selesai" dan bukan mendesak
        // Sesuaikan dengan atribut status yang kamu miliki di model Tugas
        return !isSelesai(tugas) && !isMendesak(tugas);
    }

    private boolean isSelesai(Tugas tugas) {
        // Jika kamu punya atribut status selesai, gunakan di sini
        // Contoh sementara:
        return false; // Ganti dengan logika sesungguhnya
    }

    // ... kode lain tetap sama seperti yang kamu berikan sebelumnya ...

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
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Pengingat Tugas");
                    alert.setHeaderText("Deadline Mendekati");
                    alert.setContentText(
                            "Tugas \"" + tugas.getNama() + "\" akan berakhir dalam " + daysDiff + " hari lagi!");
                    alert.showAndWait();
                } else if (daysDiff < 0) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Tugas Terlambat");
                    alert.setHeaderText("Deadline Sudah Lewat");
                    alert.setContentText(
                            "Tugas \"" + tugas.getNama() + "\" sudah melewati deadline sejak " + (-daysDiff)
                                    + " hari yang lalu!");
                    alert.showAndWait();
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
            stage.setWidth(400);
            stage.setHeight(600);
            stage.setMinWidth(350);
            stage.setMinHeight(500);
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
            stage.setWidth(400);
            stage.setHeight(600);
            stage.setMinWidth(350);
            stage.setMinHeight(500);
            Scene scene = new Scene(root);
            scene.getStylesheets()
                    .add(getClass().getResource("/com/coursemanagementsystem/tailwindfx.css").toExternalForm());
            stage.setScene(scene);
            stage.showAndWait();

            // Refresh tabel supaya data terbaru tampil
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
