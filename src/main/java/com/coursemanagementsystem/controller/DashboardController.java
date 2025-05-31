package com.coursemanagementsystem.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.scene.layout.HBox;

import com.coursemanagementsystem.model.Tugas;
import com.coursemanagementsystem.database.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class DashboardController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterStatus;
    @FXML private TableView<Tugas> taskTable;
    @FXML private TableColumn<Tugas, String> titleColumn;
    @FXML private TableColumn<Tugas, String> deadlineColumn;
    @FXML private TableColumn<Tugas, String> priorityColumn;
    @FXML private TableColumn<Tugas, String> subjectColumn;
    @FXML private TableColumn<Tugas, String> typeColumn;
    @FXML private TableColumn<Tugas, String> statusColumn;
    @FXML private TableColumn<Tugas, Void> actionColumn;

    @FXML private Label urgentCount;
    @FXML private Label inProgressCount;
    @FXML private Label completedCount;

    private ObservableList<Tugas> tugasList = FXCollections.observableArrayList();
    private FilteredList<Tugas> filteredTugas;

    @FXML
    public void initialize() {
        // Set up TableView columns
        titleColumn.setCellValueFactory(cellData -> cellData.getValue().judulProperty());
        deadlineColumn.setCellValueFactory(cellData -> cellData.getValue().deadlineProperty());
        priorityColumn.setCellValueFactory(cellData -> cellData.getValue().prioritasProperty());
        subjectColumn.setCellValueFactory(cellData -> cellData.getValue().mataKuliahProperty());
        typeColumn.setCellValueFactory(cellData -> cellData.getValue().tipeProperty());
        statusColumn.setCellValueFactory(cellData -> cellData.getValue().statusProperty());

        setupActionColumn();

        // Filter ComboBox setup
        filterStatus.setItems(FXCollections.observableArrayList("Semua", "Belum Dikerjakan", "Sedang Dikerjakan", "Selesai"));
        filterStatus.getSelectionModel().selectFirst();
        filterStatus.valueProperty().addListener((obs, oldVal, newVal) -> filterTugas());
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterTugas());

        filteredTugas = new FilteredList<>(tugasList, p -> true);
        taskTable.setItems(filteredTugas);

        loadTugasFromDatabase();
        updateSummaryCards();
    }

    @FXML
    private void handleAddNewTask() {
        tambahTugas();
    }

    private void tambahTugas() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/coursemanagementsystem/tambah_tugas.fxml"));
            Stage stage = new Stage();
            stage.setTitle("Tambah Tugas");
            stage.setScene(new Scene(loader.load()));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

            TambahTugasController controller = loader.getController();
            controller.setOnTugasAdded(() -> {
                loadTugasFromDatabase();
                updateSummaryCards();
            });

            stage.showAndWait();
        } catch (Exception e) {
            showAlert("Error", "Gagal membuka form tambah tugas: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void loadTugasFromDatabase() {
        tugasList.clear();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT id, judul, deskripsi, deadline, prioritas, mata_kuliah, tipe, status FROM tugas";
            PreparedStatement st = conn.prepareStatement(sql);
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                Tugas tugas = new Tugas(
                        rs.getInt("id"),
                        rs.getString("judul"),
                        rs.getString("deskripsi"),
                        rs.getString("deadline"),
                        rs.getString("prioritas"),
                        rs.getString("mata_kuliah"),
                        rs.getString("tipe"),
                        rs.getString("status") == null ? "Belum Dikerjakan" : rs.getString("status")
                );
                tugasList.add(tugas);
            }
        } catch (SQLException e) {
            showAlert("Database Error", "Gagal mengambil data tugas: " + e.getMessage(), Alert.AlertType.ERROR);
        }
        filterTugas();
        updateSummaryCards();
    }

    private void filterTugas() {
        String search = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
        String selectedStatus = filterStatus.getValue();
        filteredTugas.setPredicate(tugas -> {
            boolean matchesStatus = selectedStatus == null || selectedStatus.equals("Semua") || tugas.getStatus().equals(selectedStatus);
            boolean matchesSearch = search.isEmpty()
                    || tugas.getJudul().toLowerCase().contains(search)
                    || tugas.getMataKuliah().toLowerCase().contains(search)
                    || tugas.getPrioritas().toLowerCase().contains(search);
            return matchesStatus && matchesSearch;
        });
    }

    private void setupActionColumn() {
        actionColumn.setCellFactory(param -> new TableCell<>() {
            private final Button btnEdit = new Button("Edit");
            private final Button btnHapus = new Button("Hapus");
            private final HBox hbox = new HBox(8, btnEdit, btnHapus);

            {
                btnEdit.setOnAction(e -> {
                    Tugas tugas = getTableView().getItems().get(getIndex());
                    showAlert("Info", "Fitur edit belum tersedia.", Alert.AlertType.INFORMATION);
                });
                btnHapus.setOnAction(e -> {
                    Tugas tugas = getTableView().getItems().get(getIndex());
                    Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
                    confirmDialog.setTitle("Konfirmasi Hapus");
                    confirmDialog.setHeaderText("Hapus Tugas");
                    confirmDialog.setContentText("Apakah Anda yakin ingin menghapus tugas \"" + tugas.getJudul() + "\"?");
                    confirmDialog.showAndWait().ifPresent(result -> {
                        if (result == ButtonType.OK) {
                            hapusTugas(tugas);
                        }
                    });
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : hbox);
            }
        });
    }

    private void hapusTugas(Tugas tugas) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "DELETE FROM tugas WHERE id = ?";
            PreparedStatement st = conn.prepareStatement(sql);
            st.setInt(1, tugas.getId());
            st.executeUpdate();
            tugasList.remove(tugas);
            updateSummaryCards();
        } catch (SQLException e) {
            showAlert("Database Error", "Gagal menghapus tugas: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void updateSummaryCards() {
        int urgent = 0, inProgress = 0, completed = 0;
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (Tugas tugas : tugasList) {
            try {
                LocalDate deadline = LocalDate.parse(tugas.getDeadline(), formatter);
                long daysDiff = ChronoUnit.DAYS.between(today, deadline);
                if (daysDiff <= 3 && daysDiff >= 0 && !"Selesai".equalsIgnoreCase(tugas.getStatus())) {
                    urgent++;
                }
            } catch (Exception ignored) {}
            if ("Sedang Dikerjakan".equalsIgnoreCase(tugas.getStatus())) {
                inProgress++;
            } else if ("Selesai".equalsIgnoreCase(tugas.getStatus())) {
                completed++;
            }
        }
        urgentCount.setText(String.valueOf(urgent));
        inProgressCount.setText(String.valueOf(inProgress));
        completedCount.setText(String.valueOf(completed));
    }

    private void showAlert(String title, String msg, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}