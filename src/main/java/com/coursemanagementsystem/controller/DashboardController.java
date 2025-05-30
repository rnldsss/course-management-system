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
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import com.coursemanagementsystem.model.*;
import com.coursemanagementsystem.database.DatabaseConnection;

public class DashboardController {
    @FXML private TableView<Tugas> tableTugas;
    @FXML private TableColumn<Tugas, String> colNama, colDeadline, colPrioritas, colMataKuliah, colTipe;
    @FXML private TableColumn<Tugas, Void> colAksi;
    @FXML private Button btnTambah;
    @FXML private ComboBox<String> comboFilter;
    @FXML private TextField searchField;

    private ObservableList<Tugas> tugasList = FXCollections.observableArrayList();
    private FilteredList<Tugas> filteredTugas;

    private Mahasiswa mahasiswa;

    public void setMahasiswa(Mahasiswa mhs) {
        this.mahasiswa = mhs;
        loadTugasFromDatabase();
        cekNotifikasiDeadline();
    }

    @FXML
    public void initialize() {
        comboFilter.getItems().addAll("Semua", "Mendesak", "Sedang dikerjakan", "Selesai");
        comboFilter.getSelectionModel().selectFirst();

        colNama.setCellValueFactory(new PropertyValueFactory<>("judul"));
        colDeadline.setCellValueFactory(new PropertyValueFactory<>("deadline"));
        colPrioritas.setCellValueFactory(new PropertyValueFactory<>("prioritas"));
        colMataKuliah.setCellValueFactory(new PropertyValueFactory<>("mataKuliah"));
        colTipe.setCellValueFactory(new PropertyValueFactory<>("tipe"));

        setupAksiColumn();

        filteredTugas = new FilteredList<>(tugasList, p -> true);
        tableTugas.setItems(filteredTugas);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterTugas());
        comboFilter.valueProperty().addListener((obs, oldVal, newVal) -> filterTugas());

        btnTambah.setOnAction(e -> tambahTugas());
    }

    private void loadTugasFromDatabase() {
        tugasList.clear();
        if (mahasiswa == null) return;

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql =
                "SELECT t.id, t.judul, t.deadline, t.prioritas, t.mata_kuliah, t.tipe, t.deskripsi " +
                "FROM tugas t " +
                "JOIN tugas_mahasiswa tm ON t.id = tm.id_tugas " +
                "WHERE tm.id_mahasiswa = ?";
            PreparedStatement st = conn.prepareStatement(sql);
            st.setInt(1, mahasiswa.getId());
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                Tugas tugas = new Tugas(
                    rs.getInt("id"),
                    rs.getString("judul"),
                    rs.getString("deskripsi"),
                    rs.getString("deadline") != null ? rs.getString("deadline").substring(0, 10) : "",
                    rs.getString("prioritas"),
                    rs.getString("mata_kuliah"),
                    rs.getString("tipe")
                );
                tugasList.add(tugas);
            }
        } catch (SQLException e) {
            showAlert("Database Error", "Gagal mengambil data tugas: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void filterTugas() {
        String searchText = searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();
        String filterStatus = comboFilter.getValue();

        filteredTugas.setPredicate(tugas -> {
            boolean matchesSearch = searchText.isEmpty() ||
                    tugas.getJudul().toLowerCase().contains(searchText) ||
                    tugas.getMataKuliah().toLowerCase().contains(searchText) ||
                    tugas.getPrioritas().toLowerCase().contains(searchText);

            if (!matchesSearch) return false;
            if (filterStatus == null || filterStatus.equals("Semua")) return true;

            switch (filterStatus) {
                case "Mendesak":
                    return isMendesak(tugas);
                case "Sedang dikerjakan":
                    return false;
                case "Selesai":
                    return false;
                default:
                    return true;
            }
        });
    }

    private boolean isMendesak(Tugas tugas) {
        try {
            LocalDate today = LocalDate.now();
            LocalDate deadline = LocalDate.parse(tugas.getDeadline());
            long daysDiff = ChronoUnit.DAYS.between(today, deadline);
            return daysDiff >= 0 && daysDiff <= 3;
        } catch (Exception e) {
            return false;
        }
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
                        setGraphic(empty ? null : pane);
                    }
                };
            }
        };
        colAksi.setCellFactory(cellFactory);
    }

    private void tambahTugas() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/coursemanagementsystem/tambah_tugas.fxml"));
            Parent root = loader.load();

            TambahTugasController controller = loader.getController();
            controller.setMahasiswa(mahasiswa);
            controller.setOnTugasSaved(() -> {
                loadTugasFromDatabase();
                tableTugas.refresh();
            });

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Tambah Tugas Baru");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (IOException e) {
            showAlert("Error", "Tidak dapat membuka form tambah tugas: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void editTugas(Tugas tugas) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/coursemanagementsystem/tambah_tugas.fxml"));
            Parent root = loader.load();

            TambahTugasController controller = loader.getController();
            controller.setMahasiswa(mahasiswa);
            controller.setTugas(tugas);
            controller.setEditMode(true);
            controller.setOnTugasSaved(() -> {
                loadTugasFromDatabase();
                tableTugas.refresh();
            });

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Edit Tugas");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (IOException e) {
            showAlert("Error", "Tidak dapat membuka form edit tugas: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void hapusTugas(Tugas tugas) {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Konfirmasi Hapus");
        confirmDialog.setHeaderText("Hapus Tugas");
        confirmDialog.setContentText("Apakah Anda yakin ingin menghapus tugas \"" + tugas.getJudul() + "\"?");

        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                PreparedStatement stmt = conn.prepareStatement("DELETE FROM tugas WHERE id=?");
                stmt.setInt(1, tugas.getId());
                stmt.executeUpdate();
                loadTugasFromDatabase();
            } catch (SQLException e) {
                showAlert("Error", "Gagal menghapus tugas dari database: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    private void cekNotifikasiDeadline() {
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (Tugas tugas : tugasList) {
            try {
                LocalDate deadline = LocalDate.parse(tugas.getDeadline(), formatter);
                long daysDiff = ChronoUnit.DAYS.between(today, deadline);

                if (daysDiff <= 3 && daysDiff >= 0) {
                    showAlert("Pengingat Tugas", "Tugas \"" + tugas.getJudul() + "\" akan berakhir dalam " + daysDiff + " hari lagi!", Alert.AlertType.WARNING);
                } else if (daysDiff < 0) {
                    showAlert("Tugas Terlambat", "Tugas \"" + tugas.getJudul() + "\" sudah melewati deadline sejak " + (-daysDiff) + " hari yang lalu!", Alert.AlertType.ERROR);
                }
            } catch (Exception ignored) { }
        }
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}