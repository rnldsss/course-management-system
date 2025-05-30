package com.coursemanagementsystem.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.*;
import javafx.collections.transformation.FilteredList;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.event.ActionEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.File;
import java.io.IOException;
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
    private Mahasiswa mahasiswa;

    public void setMahasiswa(Mahasiswa mhs) {
        this.mahasiswa = mhs;
        System.out.println("Setting mahasiswa: " + (mhs != null ? mhs.getNama() + " (ID: " + mhs.getId() + ")" : "null"));
        loadTugasFromDatabase();
        cekNotifikasiDeadline();
    }

    @FXML
    public void initialize() {
        comboFilter.getItems().addAll("Semua", "Mendesak", "Sedang Dikerjakan", "Selesai");
        comboFilter.getSelectionModel().selectFirst();

        colNama.setCellValueFactory(new PropertyValueFactory<>("judul"));
        colDeadline.setCellValueFactory(new PropertyValueFactory<>("deadline"));
        colPrioritas.setCellValueFactory(new PropertyValueFactory<>("prioritas"));
        colMataKuliah.setCellValueFactory(new PropertyValueFactory<>("mataKuliah"));
        colTipe.setCellValueFactory(new PropertyValueFactory<>("tipe"));

        // Kolom status: checkbox + label status + tombol upload
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
                    updateTugasStatus(tugas);
                });

                uploadBtn.setOnAction(event -> {
                    FileChooser fileChooser = new FileChooser();
                    fileChooser.setTitle("Upload Tugas");
                    Stage stage = (Stage) getTableView().getScene().getWindow();
                    File file = fileChooser.showOpenDialog(stage);
                    if (file != null) {
                        Tugas tugas = getTableView().getItems().get(getIndex());
                        tugas.setUploadPath(file.getAbsolutePath());
                        updateTugasUploadPath(tugas);
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
    }

    private void loadTugasFromDatabase() {
        tugasList.clear();
        
        if (mahasiswa == null) {
            System.err.println("ERROR: Mahasiswa object is null!");
            showAlert("Error", "Data mahasiswa tidak tersedia. Silakan login ulang.", Alert.AlertType.ERROR);
            return;
        }

        System.out.println("Loading tugas for mahasiswa ID: " + mahasiswa.getId());

        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) {
                throw new SQLException("Failed to establish database connection");
            }

            System.out.println("Database connection established successfully");

            // First, let's check if the tables exist
            if (!checkTablesExist(conn)) {
                createTables(conn);
            }

            // Check if there are any tasks for this student
            String countSql = "SELECT COUNT(*) as total FROM tugas_mahasiswa WHERE id_mahasiswa = ?";
            try (PreparedStatement countStmt = conn.prepareStatement(countSql)) {
                countStmt.setInt(1, mahasiswa.getId());
                ResultSet countRs = countStmt.executeQuery();
                if (countRs.next()) {
                    int totalTugas = countRs.getInt("total");
                    System.out.println("Total tugas for student: " + totalTugas);
                }
            }

            // Main query to get tasks
            String sql = 
                "SELECT t.id, t.judul, t.deadline, t.prioritas, t.mata_kuliah, t.tipe, t.deskripsi, t.status, t.upload_path " +
                "FROM tugas t " +
                "JOIN tugas_mahasiswa tm ON t.id = tm.id_tugas " +
                "WHERE tm.id_mahasiswa = ?";
            
            try (PreparedStatement st = conn.prepareStatement(sql)) {
                st.setInt(1, mahasiswa.getId());
                System.out.println("Executing query: " + sql);
                System.out.println("With parameter: " + mahasiswa.getId());
                
                ResultSet rs = st.executeQuery();
                int taskCount = 0;
                
                while (rs.next()) {
                    taskCount++;
                    try {
                        Tugas tugas = new Tugas(
                            rs.getInt("id"),
                            rs.getString("judul"),
                            rs.getString("deadline") != null ? rs.getString("deadline").substring(0, 10) : "",
                            rs.getString("prioritas"),
                            rs.getString("mata_kuliah"),
                            rs.getString("tipe")
                        );
                        
                        String status = rs.getString("status");
                        tugas.setStatus(status != null ? status : "Belum Dikerjakan");
                        
                        String uploadPath = rs.getString("upload_path");
                        tugas.setUploadPath(uploadPath != null ? uploadPath : "");
                        
                        tugasList.add(tugas);
                        
                        System.out.println("Added task: " + tugas.getJudul() + " (ID: " + tugas.getId() + ")");
                        
                    } catch (Exception e) {
                        System.err.println("Error creating Tugas object: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
                
                System.out.println("Successfully loaded " + taskCount + " tasks");
                
                if (taskCount == 0) {
                    System.out.println("No tasks found for this student. This might be normal for new users.");
                    showAlert("Info", "Belum ada tugas. Klik 'Tambah Tugas' untuk menambah tugas baru.", Alert.AlertType.INFORMATION);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("SQL Error in loadTugasFromDatabase: " + e.getMessage());
            System.err.println("SQL State: " + e.getSQLState());
            System.err.println("Error Code: " + e.getErrorCode());
            e.printStackTrace();
            
            showAlert("Database Error", 
                "Gagal mengambil data tugas: " + e.getMessage() + 
                "\n\nPastikan database sudah berjalan dan tabel sudah dibuat.", 
                Alert.AlertType.ERROR);
        } catch (Exception e) {
            System.err.println("Unexpected error in loadTugasFromDatabase: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Terjadi kesalahan tak terduga: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private boolean checkTablesExist(Connection conn) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        
        // Check if 'tugas' table exists
        ResultSet tugasTable = meta.getTables(null, null, "tugas", new String[]{"TABLE"});
        boolean tugasExists = tugasTable.next();
        tugasTable.close();
        
        // Check if 'tugas_mahasiswa' table exists
        ResultSet tugasMhsTable = meta.getTables(null, null, "tugas_mahasiswa", new String[]{"TABLE"});
        boolean tugasMhsExists = tugasMhsTable.next();
        tugasMhsTable.close();
        
        System.out.println("Table 'tugas' exists: " + tugasExists);
        System.out.println("Table 'tugas_mahasiswa' exists: " + tugasMhsExists);
        
        return tugasExists && tugasMhsExists;
    }

    private void createTables(Connection conn) throws SQLException {
        System.out.println("Creating missing tables...");
        
        String createTugasTable = """
            CREATE TABLE IF NOT EXISTS tugas (
                id INT AUTO_INCREMENT PRIMARY KEY,
                judul VARCHAR(255) NOT NULL,
                deskripsi TEXT,
                deadline DATE,
                prioritas VARCHAR(50),
                mata_kuliah VARCHAR(100),
                tipe VARCHAR(50),
                status VARCHAR(50) DEFAULT 'Belum Dikerjakan',
                upload_path TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;
        
        String createTugasMahasiswaTable = """
            CREATE TABLE IF NOT EXISTS tugas_mahasiswa (
                id INT AUTO_INCREMENT PRIMARY KEY,
                id_tugas INT,
                id_mahasiswa INT,
                FOREIGN KEY (id_tugas) REFERENCES tugas(id) ON DELETE CASCADE,
                FOREIGN KEY (id_mahasiswa) REFERENCES mahasiswa(id) ON DELETE CASCADE
            )
        """;
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createTugasTable);
            stmt.execute(createTugasMahasiswaTable);
            System.out.println("Tables created successfully");
        }
    }

    private void updateTugasStatus(Tugas tugas) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "UPDATE tugas SET status = ? WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, tugas.getStatus());
            stmt.setInt(2, tugas.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating task status: " + e.getMessage());
            showAlert("Error", "Gagal mengupdate status tugas: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void updateTugasUploadPath(Tugas tugas) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "UPDATE tugas SET upload_path = ? WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, tugas.getUploadPath());
            stmt.setInt(2, tugas.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating upload path: " + e.getMessage());
            showAlert("Error", "Gagal mengupdate file upload: " + e.getMessage(), Alert.AlertType.ERROR);
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

    private void showAlert(String title, String msg, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}