package com.coursemanagementsystem.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.*;
import model.*;

public class DashboardController {
    @FXML private TableView<Tugas> tableTugas;
    @FXML private TableColumn<Tugas, String> colNama, colDeadline, colPrioritas, colMataKuliah;
    @FXML private TableColumn<Tugas, Void> colAksi;
    @FXML private Button btnTambah;

    private ObservableList<Tugas> tugasList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colNama.setCellValueFactory(new PropertyValueFactory<>("nama"));
        colDeadline.setCellValueFactory(new PropertyValueFactory<>("deadline"));
        colPrioritas.setCellValueFactory(new PropertyValueFactory<>("prioritas"));
        colMataKuliah.setCellValueFactory(new PropertyValueFactory<>("mataKuliah"));

        tableTugas.setItems(tugasList);

        btnTambah.setOnAction(e -> tambahTugas());

        // TODO: Implementasi Edit, Hapus, Notifikasi, Kolaborasi
    }

    private void tambahTugas() {
        // TODO: Buka dialog FXML tambah tugas dan tambahkan ke tugasList jika OK
    }
}