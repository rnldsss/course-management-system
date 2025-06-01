package com.coursemanagementsystem.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.prefs.Preferences;

import com.coursemanagementsystem.database.DatabaseConnection;
import com.coursemanagementsystem.model.Tugas;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

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
    @FXML private ToggleButton toggleThemeBtn;

    private ObservableList<Tugas> tugasList = FXCollections.observableArrayList();
    private FilteredList<Tugas> filteredTugas;
    private Preferences prefs = Preferences.userNodeForPackage(DashboardController.class);

    @FXML
    public void initialize() {
        filteredTugas = new FilteredList<>(tugasList, p -> true);
        taskTable.setItems(filteredTugas);

        filterStatus.getItems().addAll("Semua", "Belum Dikerjakan", "Sedang Dikerjakan", "Selesai");
        filterStatus.setValue("Semua");

        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterTugas());
        filterStatus.valueProperty().addListener((obs, oldVal, newVal) -> filterTugas());

        titleColumn.setCellValueFactory(data -> data.getValue().judulProperty());
        deadlineColumn.setCellValueFactory(data -> data.getValue().deadlineProperty());
        priorityColumn.setCellValueFactory(data -> data.getValue().prioritasProperty());
        subjectColumn.setCellValueFactory(data -> data.getValue().mataKuliahProperty());
        typeColumn.setCellValueFactory(data -> data.getValue().tipeProperty());
        statusColumn.setCellValueFactory(data -> data.getValue().statusProperty());

        setupStatusColumn();
        setupActionColumn();
        loadTugasFromDatabase();
    }

    @FXML
    private void handleAddNewTask() {
        openFormTugas(null);
    }

    private void openFormTugas(Tugas tugasToEdit) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/coursemanagementsystem/tambah_tugas.fxml"));
            Stage stage = new Stage();
            stage.setTitle(tugasToEdit == null ? "Tambah Tugas" : "Edit Tugas");
            stage.setScene(new Scene(loader.load()));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

            TambahTugasController controller = loader.getController();
            if (tugasToEdit != null) {
                controller.setTugasToEdit(tugasToEdit);
            }
            controller.setOnTugasAdded(() -> {
                loadTugasFromDatabase(); // ini sudah otomatis updateSummaryCards
            });

            stage.showAndWait();
        } catch (Exception e) {
            showAlert("Error", "Gagal membuka form tugas: " + e.getMessage(), Alert.AlertType.ERROR);
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

    private void setupStatusColumn() {
        statusColumn.setCellFactory(param -> new TableCell<Tugas, String>() {
            private final ComboBox<String> statusComboBox = new ComboBox<>();

            {
                statusComboBox.getItems().addAll("Belum Dikerjakan", "Sedang Dikerjakan", "Selesai");
                statusComboBox.setOnAction(e -> {
                    Tugas tugas = getTableView().getItems().get(getIndex());
                    String newStatus = statusComboBox.getValue();
                    if (newStatus != null && !newStatus.equals(tugas.getStatus())) {
                        updateStatusInDatabase(tugas, newStatus);
                    }
                });
            }

            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                } else {
                    statusComboBox.setValue(status);
                    setGraphic(statusComboBox);
                }
            }
        });
    }

    private void updateStatusInDatabase(Tugas tugas, String newStatus) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "UPDATE tugas SET status = ? WHERE id = ?";
            PreparedStatement st = conn.prepareStatement(sql);
            st.setString(1, newStatus);
            st.setInt(2, tugas.getId());
            int rowsAffected = st.executeUpdate();

            if (rowsAffected > 0) {
                tugas.setStatus(newStatus);
                updateSummaryCards();
                filterTugas();
                showAlert("Sukses", "Status tugas berhasil diperbarui!", Alert.AlertType.INFORMATION);
            }
        } catch (SQLException e) {
            showAlert("Database Error", "Gagal mengupdate status: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void setupActionColumn() {
        actionColumn.setCellFactory(param -> new TableCell<>() {
            private final Button btnEdit = new Button("Edit");
            private final Button btnHapus = new Button("Hapus");
            private final HBox hbox = new HBox(8, btnEdit, btnHapus);

            {
                btnEdit.getStyleClass().addAll("action-button", "edit-button");
                btnHapus.getStyleClass().addAll("action-button", "delete-button");

                btnEdit.setOnAction(e -> {
                    Tugas tugas = getTableView().getItems().get(getIndex());
                    openFormTugas(tugas);
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
            showAlert("Sukses", "Tugas berhasil dihapus!", Alert.AlertType.INFORMATION);
        } catch (SQLException e) {
            showAlert("Database Error", "Gagal menghapus tugas: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // Final version of updateSummaryCards with deadline reminders
    private void updateSummaryCards() {
        int urgent = 0, inProgress = 0, completed = 0;
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        StringBuilder urgentTasks = new StringBuilder();

        for (Tugas tugas : tugasList) {
            try {
                String deadlineStr = tugas.getDeadline();
                LocalDate deadline = deadlineStr.contains(" ")
                        ? LocalDate.parse(deadlineStr.split(" ")[0], formatter)
                        : LocalDate.parse(deadlineStr, formatter);

                long daysDiff = ChronoUnit.DAYS.between(today, deadline);

                if (daysDiff <= 3 && daysDiff >= 0 && !"Selesai".equalsIgnoreCase(tugas.getStatus())) {
                    urgent++;
                    String formattedDeadline = deadline.atStartOfDay()
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                    urgentTasks.append("Tugas: \"").append(tugas.getJudul())
                            .append("\" deadline: ").append(formattedDeadline)
                            .append(" (").append(daysDiff).append(" hari lagi)\n");
                }
            } catch (Exception e) {
                System.err.println("Gagal parsing deadline: " + tugas.getDeadline() + " - " + e.getMessage());
            }

            if ("Sedang Dikerjakan".equalsIgnoreCase(tugas.getStatus())) {
                inProgress++;
            } else if ("Selesai".equalsIgnoreCase(tugas.getStatus())) {
                completed++;
            }
        }

        if (urgent > 0 && urgentTasks.length() > 0) {
            showAlert("Pengingat Tugas", "Beberapa tugas memiliki deadline mendekati:\n\n" + urgentTasks, Alert.AlertType.WARNING);
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
