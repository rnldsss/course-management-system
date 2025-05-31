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
        comboFilter.getItems().setAll("Semua", "Mendesak", "Sedang Dikerjakan", "Selesai");
        comboFilter.getSelectionModel().select(0);

        colNama.setCellValueFactory(new PropertyValueFactory<>("judul"));
        colDeadline.setCellValueFactory(new PropertyValueFactory<>("deadline"));
        colPrioritas.setCellValueFactory(new PropertyValueFactory<>("prioritas"));
        colMataKuliah.setCellValueFactory(new PropertyValueFactory<>("mataKuliah"));
        colTipe.setCellValueFactory(new PropertyValueFactory<>("tipe"));

        colStatus.setCellFactory(param -> new TableCell<Tugas, Void>() {
            private final CheckBox checkBox = new CheckBox();
            private final Button uploadBtn = new Button();
            private final Label statusLabel = new Label();
            private final Label uploadDone = new Label("Selesai diupload");
            private final HBox topRow = new HBox(16, checkBox, uploadBtn);
            private final VBox box = new VBox(8, topRow, statusLabel);

            {
                topRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                box.setPadding(new javafx.geometry.Insets(4, 0, 4, 0));

                checkBox.setOnAction(event -> {
                    int idx = getIndex();
                    if (idx >= 0 && idx < getTableView().getItems().size()) {
                        Tugas t = getTableView().getItems().get(idx);
                        if (checkBox.isSelected()) {
                            t.setStatus("Sedang Dikerjakan");
                            statusLabel.setText("Sedang Dikerjakan");
                        } else {
                            t.setStatus("Belum Dikerjakan");
                            statusLabel.setText("Belum Dikerjakan");
                        }
                    }
                });

                uploadBtn.setOnAction(event -> {
                    FileChooser fileChooser = new FileChooser();
                    fileChooser.setTitle("Upload Tugas");
                    Stage stage = (Stage) getTableView().getScene().getWindow();
                    File file = fileChooser.showOpenDialog(stage);
                    if (file != null) {
                        int idx = getIndex();
                        if (idx >= 0 && idx < getTableView().getItems().size()) {
                            Tugas t = getTableView().getItems().get(idx);
                            t.setUploadPath(file.getAbsolutePath());
                            getTableView().refresh();
                        }
                    }
                });

                checkBox.getStyleClass().add("custom-checkbox");
                uploadBtn.getStyleClass().add("upload-btn");
                statusLabel.getStyleClass().add("status-label");
                uploadDone.setStyle("-fx-text-fill: #16a34a; -fx-font-size: 11px;");
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    int idx = getIndex();
                    if (idx < 0 || idx >= getTableView().getItems().size()) {
                        setGraphic(null);
                        return;
                    }
                    Tugas t = getTableView().getItems().get(idx);
                    boolean sedang = "Sedang Dikerjakan".equals(t.getStatus());
                    checkBox.setSelected(sedang);
                    statusLabel.setText(sedang ? "Sedang Dikerjakan" : "Belum Dikerjakan");

                    if (t.getUploadPath() != null && !t.getUploadPath().isEmpty()) {
                        uploadBtn.setText("Selesai");
                        uploadBtn.setDisable(true);
                        if (!box.getChildren().contains(uploadDone)) box.getChildren().add(uploadDone);
                    } else {
                        uploadBtn.setText("Upload Tugas");
                        uploadBtn.setDisable(false);
                        box.getChildren().remove(uploadDone);
                    }
                    setGraphic(box);
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
            String sql = "SELECT id, judul, deskripsi, deadline, prioritas, mata_kuliah, tipe FROM tugas";
            PreparedStatement st = conn.prepareStatement(sql);
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                Tugas t = new Tugas(
                        rs.getInt("id"),
                        rs.getString("judul"),
                        rs.getString("deskripsi"),
                        rs.getString("deadline") != null ? rs.getString("deadline").substring(0, 10) : "",
                        rs.getString("prioritas"),
                        rs.getString("mata_kuliah"),
                        rs.getString("tipe")
                );
                tugasList.add(t);
            }
        } catch (SQLException e) {
            showAlert("Database Error", "Gagal mengambil data tugas: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void filterTugas() {
        String searchText = searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();
        String filterStatus = comboFilter.getValue();

        filteredTugas.setPredicate(t -> {
            boolean matchSearch = searchText.isEmpty() ||
                    t.getJudul().toLowerCase().contains(searchText) ||
                    t.getMataKuliah().toLowerCase().contains(searchText) ||
                    t.getPrioritas().toLowerCase().contains(searchText);
            if (!matchSearch) return false;
            if (filterStatus == null || filterStatus.equals("Semua")) return true;
            switch (filterStatus) {
                case "Mendesak": return isMendesak(t);
                case "Sedang Dikerjakan": return isSedangDikerjakan(t);
                case "Selesai": return isSelesai(t);
                default: return true;
            }
        });
    }

    private boolean isMendesak(Tugas t) {
        try {
            LocalDate now = LocalDate.now();
            LocalDate deadline = LocalDate.parse(t.getDeadline());
            long sisa = ChronoUnit.DAYS.between(now, deadline);
            return sisa >= 0 && sisa <= 3 && !isSelesai(t);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isSedangDikerjakan(Tugas t) {
        return "Sedang Dikerjakan".equals(t.getStatus());
    }

    private boolean isSelesai(Tugas t) {
        return "Selesai".equals(t.getStatus());
    }

    private void setupAksiColumn() {
        colAksi.setCellFactory(param -> new TableCell<Tugas, Void>() {
            private final Button btnEdit = new Button("Edit");
            private final Button btnHapus = new Button("Hapus");
            private final HBox pane = new HBox(5, btnEdit, btnHapus);

            {
                btnEdit.setOnAction((event) -> {
                    int idx = getIndex();
                    if (idx >= 0 && idx < getTableView().getItems().size()) {
                        Tugas t = getTableView().getItems().get(idx);
                        editTugas(t);
                    }
                });

                btnHapus.setOnAction((event) -> {
                    int idx = getIndex();
                    if (idx >= 0 && idx < getTableView().getItems().size()) {
                        Tugas t = getTableView().getItems().get(idx);
                        hapusTugas(t);
                    }
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
            controller.setOnTugasAdded(this::loadTugasFromDatabase);

            stage.showAndWait();
        } catch (Exception e) {
            showAlert("Error", "Gagal membuka form tambah tugas: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void editTugas(Tugas t) {
        showAlert("Info", "Fitur edit tugas belum diimplementasikan.", Alert.AlertType.INFORMATION);
    }

    private void hapusTugas(Tugas t) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Konfirmasi Hapus");
        confirm.setHeaderText("Hapus Tugas");
        confirm.setContentText("Apakah Anda yakin ingin menghapus tugas \"" + t.getJudul() + "\"?");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            tugasList.remove(t);
        }
    }

    private void cekNotifikasiDeadline() {
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (Tugas t : tugasList) {
            try {
                LocalDate deadline = LocalDate.parse(t.getDeadline(), formatter);
                long diff = ChronoUnit.DAYS.between(today, deadline);
                if (diff <= 3 && diff >= 0) {
                    showAlert("Pengingat Tugas", "Tugas \"" + t.getJudul() + "\" akan berakhir dalam " + diff + " hari lagi!", Alert.AlertType.WARNING);
                } else if (diff < 0) {
                    showAlert("Tugas Terlambat", "Tugas \"" + t.getJudul() + "\" sudah melewati deadline sejak " + (-diff) + " hari yang lalu!", Alert.AlertType.ERROR);
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