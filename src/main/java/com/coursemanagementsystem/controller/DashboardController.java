package com.coursemanagementsystem.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.*;
import javafx.collections.transformation.FilteredList;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.File;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import com.coursemanagementsystem.model.*;
import com.coursemanagementsystem.database.DatabaseConnection;

public class DashboardController {
    @FXML
    private TableView<Tugas> tableTugas;
    @FXML
    private TableColumn<Tugas, String> colNama, colDeadline, colPrioritas, colMataKuliah, colTipe;
    @FXML
    private TableColumn<Tugas, Void> colAksi;
    @FXML
    private TableColumn<Tugas, Void> colStatus;
    @FXML
    private Button btnTambah;
    @FXML
    private ComboBox<String> comboFilter;
    @FXML
    private TextField searchField;

    private ObservableList<Tugas> tugasList = FXCollections.observableArrayList();
    private FilteredList<Tugas> filteredTugas;

    @FXML
    public void initialize() {
        comboFilter.getItems().addAll("Semua", "Mendesak", "Sedang Dikerjakan", "Selesai");
        comboFilter.getSelectionModel().selectFirst();

        colNama.setCellValueFactory(new PropertyValueFactory<>("judul"));
        colDeadline.setCellValueFactory(new PropertyValueFactory<>("deadline"));
        colPrioritas.setCellValueFactory(new PropertyValueFactory<>("prioritas"));
        colMataKuliah.setCellValueFactory(new PropertyValueFactory<>("mataKuliah"));
        colTipe.setCellValueFactory(new PropertyValueFactory<>("tipe"));

        colStatus.setCellFactory(param -> new TableCell<Tugas, Void>() {
            private final CheckBox checkBox = new CheckBox();
            private final Button uploadBtn = new Button();
            private final Label lblStatus = new Label();
            private final Label lblUploadDone = new Label("Selesai diupload");
            private final HBox barisAtas = new HBox(16, checkBox, uploadBtn);
            private final VBox vbox = new VBox(8, barisAtas, lblStatus);

            {
                barisAtas.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                vbox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                vbox.setPadding(new javafx.geometry.Insets(4, 0, 4, 0));

                checkBox.setOnAction(event -> {
                    Tugas tugas = getTableView().getItems().get(getIndex());
                    if (checkBox.isSelected()) {
                        tugas.setStatus("Sedang Dikerjakan");
                        lblStatus.setText("Sedang Dikerjakan");
                    } else {
                        tugas.setStatus("Belum Dikerjakan");
                        lblStatus.setText("Belum Dikerjakan");
                    }
                });

                uploadBtn.setOnAction(event -> {
                    FileChooser fileChooser = new FileChooser();
                    fileChooser.setTitle("Upload Tugas");
                    Stage stage = (Stage) getTableView().getScene().getWindow();
                    File file = fileChooser.showOpenDialog(stage);
                    if (file != null) {
                        Tugas tugas = getTableView().getItems().get(getIndex());
                        tugas.setUploadPath(file.getAbsolutePath());
                        getTableView().refresh();
                    }
                });

                checkBox.getStyleClass().add("custom-checkbox");
                uploadBtn.getStyleClass().add("upload-btn");
                lblStatus.getStyleClass().add("status-label");
                lblUploadDone.setStyle("-fx-text-fill: #16a34a; -fx-font-size: 11px;");
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Tugas tugas = getTableView().getItems().get(getIndex());
                    boolean sedangDikerjakan = "Sedang Dikerjakan".equals(tugas.getStatus());
                    checkBox.setSelected(sedangDikerjakan);
                    lblStatus.setText(sedangDikerjakan ? "Sedang Dikerjakan" : "Belum Dikerjakan");

                    if (tugas.getUploadPath() != null && !tugas.getUploadPath().isEmpty()) {
                        uploadBtn.setText("Selesai");
                        uploadBtn.setDisable(true);
                        if (!vbox.getChildren().contains(lblUploadDone)) {
                            vbox.getChildren().add(lblUploadDone);
                        }
                    } else {
                        uploadBtn.setText("Upload Tugas");
                        uploadBtn.setDisable(false);
                        vbox.getChildren().remove(lblUploadDone);
                    }
                    setGraphic(vbox);
                }
            }
        });

        setupAksiColumn();

        filteredTugas = new FilteredList<>(tugasList, p -> true);
        tableTugas.setItems(filteredTugas);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterTugas());
        comboFilter.valueProperty().addListener((obs, oldVal, newVal) -> filterTugas());

        btnTambah.setOnAction(e -> tambahTugas());

        loadTugasFromDatabase();
        cekNotifikasiDeadline();
    }

    private void loadTugasFromDatabase() {
        tugasList.clear();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql =
                "SELECT id, judul, deskripsi, deadline, prioritas, mata_kuliah, tipe " +
                "FROM tugas";
            PreparedStatement st = conn.prepareStatement(sql);
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
                case "Mendesak": return isMendesak(tugas);
                case "Sedang Dikerjakan": return isSedangDikerjakan(tugas);
                case "Selesai": return isSelesai(tugas);
                default: return true;
            }
        });
    }

    private boolean isMendesak(Tugas tugas) {
        try {
            LocalDate today = LocalDate.now();
            LocalDate deadline = LocalDate.parse(tugas.getDeadline());
            long daysDiff = ChronoUnit.DAYS.between(today, deadline);
            return daysDiff >= 0 && daysDiff <= 3 && !isSelesai(tugas);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isSedangDikerjakan(Tugas tugas) {
        return "Sedang Dikerjakan".equals(tugas.getStatus());
    }

    private boolean isSelesai(Tugas tugas) {
        return "Selesai".equals(tugas.getStatus());
    }

    private void setupAksiColumn() {
        colAksi.setCellFactory(param -> new TableCell<Tugas, Void>() {
            private final Button btnEdit = new Button("Edit");
            private final Button btnHapus = new Button("Hapus");
            private final HBox pane = new HBox(5, btnEdit, btnHapus);

            {
                btnEdit.setOnAction((event) -> {
                    Tugas tugas = getTableView().getItems().get(getIndex());
                    editTugas(tugas);
                });

                btnHapus.setOnAction((event) -> {
                    Tugas tugas = getTableView().getItems().get(getIndex());
                    hapusTugas(tugas);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    private void tambahTugas() {
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/coursemanagementsystem/tambah_tugas.fxml"));
        Stage stage = new Stage();
        stage.setTitle("Tambah Tugas");
        stage.setScene(new Scene(loader.load()));
        stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

        TambahTugasController controller = loader.getController();
        controller.setOnTugasAdded(() -> loadTugasFromDatabase());

        stage.showAndWait();
    } catch (Exception e) {
        showAlert("Error", "Gagal membuka form tambah tugas: " + e.getMessage(), Alert.AlertType.ERROR);
    }
}

    private void editTugas(Tugas tugas) {
        showAlert("Info", "Fitur edit tugas belum diimplementasikan.", Alert.AlertType.INFORMATION);
    }

    private void hapusTugas(Tugas tugas) {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Konfirmasi Hapus");
        confirmDialog.setHeaderText("Hapus Tugas");
        confirmDialog.setContentText("Apakah Anda yakin ingin menghapus tugas \"" + tugas.getJudul() + "\"?");

        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            tugasList.remove(tugas);
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

    private void showAlert(String title, String msg, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}