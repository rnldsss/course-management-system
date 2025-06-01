package com.coursemanagementsystem.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import com.coursemanagementsystem.database.DatabaseConnection;
import com.coursemanagementsystem.model.Mahasiswa;
import com.coursemanagementsystem.model.Tugas;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class TambahTugasController {

    @FXML private TextField txtJudul;
    @FXML private TextField txtMataKuliah;
    @FXML private TextArea txtDeskripsi;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> cbJam;
    @FXML private ComboBox<String> cbPrioritas;
    @FXML private ComboBox<String> cbTipe;
    @FXML private ComboBox<String> cbStatus;
    @FXML private VBox vboxAnggotaKelompok;
    @FXML private TextField txtNamaAnggota;
    @FXML private TextField txtNimAnggota;
    @FXML private Button btnTambahAnggota;
    @FXML private ListView<String> listViewAnggota;
    @FXML private Button btnSimpan;
    @FXML private Button btnBatal;
    @FXML private ComboBox<String> cbMenit;

    private ObservableList<Mahasiswa> anggotaKelompok = FXCollections.observableArrayList();
    private ObservableList<String> anggotaDisplayList = FXCollections.observableArrayList();
    private Tugas tugasToEdit = null;
    private Runnable onTugasAdded;
    private boolean isInitialized = false;

    public TambahTugasController() {}

    @FXML
    public void initialize() {
        // Gunakan Platform.runLater untuk memastikan semua komponen sudah ter-inject
        Platform.runLater(() -> {
            try {
                initializeComponents();
                isInitialized = true;
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Error", "Gagal inisialisasi form: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    private void initializeComponents() {
        // Initialize ComboBox items - dengan null check
        if (cbPrioritas != null) {
            cbPrioritas.getItems().clear();
            cbPrioritas.getItems().addAll("Rendah", "Menengah", "Tinggi");
            cbPrioritas.setValue("Rendah"); // Set default
        }
        
        if (cbTipe != null) {
            cbTipe.getItems().clear();
            cbTipe.getItems().addAll("Individu", "Kelompok");
            cbTipe.setValue("Individu"); // Set default
        }
        
        if (cbStatus != null) {
            cbStatus.getItems().clear();
            cbStatus.getItems().addAll("Belum Dikerjakan", "Sedang Dikerjakan", "Selesai");
            cbStatus.setValue("Belum Dikerjakan"); // Set default
        }

        // Initialize time ComboBoxes
        initializeTimeComboBoxes();
        
        // Initialize DatePicker
        initializeDatePicker();
        
        // Initialize ListView
        if (listViewAnggota != null) {
            listViewAnggota.setItems(anggotaDisplayList);
            setupListView();
        }
        
        // Set button actions
        setupButtonActions();

        // Setup type change listener
        setupTypeChangeListener();
        
        // Initially hide group members section
        handleTipeChange("Individu");
    }

    private void initializeTimeComboBoxes() {
        // Jam: 00-23
        if (cbJam != null) {
            cbJam.getItems().clear();
            for (int i = 0; i < 24; i++) {
                cbJam.getItems().add(String.format("%02d", i));
            }
            cbJam.setValue("00"); // Default value
        }
        
        // Menit: kelipatan 5
        if (cbMenit != null) {
            cbMenit.getItems().clear();
            for (int i = 0; i < 60; i += 5) {
                cbMenit.getItems().add(String.format("%02d", i));
            }
            cbMenit.setValue("00"); // Default value
        }
    }

    private void initializeDatePicker() {
        if (datePicker != null) {
            // Set date format
            datePicker.setConverter(new javafx.util.StringConverter<LocalDate>() {
                private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                
                @Override
                public String toString(LocalDate date) {
                    if (date != null) {
                        return dateFormatter.format(date);
                    }
                    return "";
                }
                
                @Override
                public LocalDate fromString(String string) {
                    if (string != null && !string.isEmpty()) {
                        try {
                            return LocalDate.parse(string, dateFormatter);
                        } catch (Exception e) {
                            return null;
                        }
                    }
                    return null;
                }
            });
            
            // Restrict to today and future dates
            datePicker.setDayCellFactory(picker -> new DateCell() {
                @Override
                public void updateItem(LocalDate date, boolean empty) {
                    super.updateItem(date, empty);
                    LocalDate today = LocalDate.now();
                    setDisable(empty || date.isBefore(today));
                }
            });
            
            // Set default to today
            datePicker.setValue(LocalDate.now());
        }
    }

    private void setupButtonActions() {
        if (btnTambahAnggota != null) {
            btnTambahAnggota.setOnAction(e -> tambahAnggotaKelompok());
        }
        if (btnSimpan != null) {
            btnSimpan.setOnAction(e -> simpanTugas());
        }
        if (btnBatal != null) {
            btnBatal.setOnAction(e -> close());
        }
    }

    private void setupTypeChangeListener() {
        if (cbTipe != null) {
            cbTipe.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    handleTipeChange(newVal);
                }
            });
        }
    }

    private void handleTipeChange(String newType) {
        boolean isKelompok = "Kelompok".equals(newType);
        if (vboxAnggotaKelompok != null) {
            vboxAnggotaKelompok.setVisible(isKelompok);
            vboxAnggotaKelompok.setManaged(isKelompok);
        }
        
        if (!isKelompok) {
            anggotaKelompok.clear();
            anggotaDisplayList.clear();
        }
    }

    private void setupListView() {
        if (listViewAnggota != null) {
            listViewAnggota.setCellFactory(listView -> {
                ListCell<String> cell = new ListCell<String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty || item == null ? null : item);
                    }
                };

                ContextMenu contextMenu = new ContextMenu();
                MenuItem deleteItem = new MenuItem("🗑 Hapus");
                deleteItem.setOnAction(e -> {
                    int index = cell.getIndex();
                    if (index >= 0 && index < anggotaKelompok.size()) {
                        String nama = anggotaKelompok.get(index).getNama();
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                        alert.setTitle("Hapus Anggota");
                        alert.setHeaderText(null);
                        alert.setContentText("Yakin hapus " + nama + "?");
                        Optional<ButtonType> result = alert.showAndWait();
                        if (result.isPresent() && result.get() == ButtonType.OK) {
                            anggotaKelompok.remove(index);
                            anggotaDisplayList.remove(index);
                        }
                    }
                });

                contextMenu.getItems().add(deleteItem);
                cell.emptyProperty().addListener((obs, wasEmpty, isNowEmpty) -> {
                    cell.setContextMenu(isNowEmpty ? null : contextMenu);
                });

                return cell;
            });
        }
    }

    private void tambahAnggotaKelompok() {
        String nama = (txtNamaAnggota != null) ? txtNamaAnggota.getText().trim() : "";
        String nim = (txtNimAnggota != null) ? txtNimAnggota.getText().trim() : "";

        if (nama.isEmpty() || nim.isEmpty()) {
            showAlert("Validasi", "Nama dan NIM harus diisi!", Alert.AlertType.WARNING);
            return;
        }

        if (anggotaKelompok.stream().anyMatch(a -> a.getNim().equals(nim))) {
            showAlert("Validasi", "NIM sudah ada di kelompok!", Alert.AlertType.WARNING);
            return;
        }

        anggotaKelompok.add(new Mahasiswa(0, nama, nim, "", ""));
        anggotaDisplayList.add("👤 " + nama + " (" + nim + ")");
        
        if (txtNamaAnggota != null) txtNamaAnggota.clear();
        if (txtNimAnggota != null) txtNimAnggota.clear();
    }

    private void simpanTugas() {
        // Validate input
        String judul = (txtJudul != null) ? txtJudul.getText().trim() : "";
        String deskripsi = (txtDeskripsi != null) ? txtDeskripsi.getText().trim() : "";
        LocalDate tanggal = (datePicker != null) ? datePicker.getValue() : null;
        String jam = (cbJam != null) ? cbJam.getValue() : null;
        String menit = (cbMenit != null) ? cbMenit.getValue() : null;
        String prioritas = (cbPrioritas != null) ? cbPrioritas.getValue() : null;
        String matkul = (txtMataKuliah != null) ? txtMataKuliah.getText().trim() : "";
        String tipe = (cbTipe != null) ? cbTipe.getValue() : null;
        String status = (cbStatus != null) ? cbStatus.getValue() : null;

        if (judul.isEmpty() || tanggal == null || jam == null || menit == null
                || prioritas == null || matkul.isEmpty() || tipe == null || status == null) {
            showAlert("Validasi", "Semua field wajib diisi!", Alert.AlertType.WARNING);
            return;
        }

        if ("Kelompok".equals(tipe) && anggotaKelompok.isEmpty()) {
            showAlert("Validasi", "Tugas kelompok butuh minimal 1 anggota.", Alert.AlertType.WARNING);
            return;
        }

        try {
            Timestamp deadline = Timestamp.valueOf(tanggal + " " + jam + ":" + menit + ":00");

            try (Connection conn = DatabaseConnection.getConnection()) {
                conn.setAutoCommit(false);

                if (tugasToEdit == null) {
                    // INSERT new task
                    insertNewTask(conn, judul, deskripsi, deadline, prioritas, matkul, tipe, status);
                } else {
                    // UPDATE existing task
                    updateExistingTask(conn, judul, deskripsi, deadline, prioritas, matkul, tipe, status);
                }

                conn.commit();
                if (onTugasAdded != null) {
                    onTugasAdded.run();
                }
                close();

            } catch (SQLException e) {
                e.printStackTrace();
                showAlert("Error", "Gagal simpan tugas: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Gagal menyimpan tugas: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void insertNewTask(Connection conn, String judul, String deskripsi, Timestamp deadline, 
                              String prioritas, String matkul, String tipe, String status) throws SQLException {
        String sql = "INSERT INTO tugas (judul, deskripsi, deadline, prioritas, mata_kuliah, tipe, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement st = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            st.setString(1, judul);
            st.setString(2, deskripsi);
            st.setTimestamp(3, deadline);
            st.setString(4, prioritas);
            st.setString(5, matkul);
            st.setString(6, tipe);
            st.setString(7, status);

            st.executeUpdate();

            try (ResultSet rs = st.getGeneratedKeys()) {
                int tugasId = rs.next() ? rs.getInt(1) : 0;

                if ("Kelompok".equals(tipe) && !anggotaKelompok.isEmpty()) {
                    insertGroupMembers(conn, tugasId);
                }
            }
        }
        showAlert("Sukses", "Tugas berhasil ditambahkan!", Alert.AlertType.INFORMATION);
    }

    private void updateExistingTask(Connection conn, String judul, String deskripsi, Timestamp deadline, 
                                   String prioritas, String matkul, String tipe, String status) throws SQLException {
        String sql = "UPDATE tugas SET judul=?, deskripsi=?, deadline=?, prioritas=?, mata_kuliah=?, tipe=?, status=? WHERE id=?";
        try (PreparedStatement st = conn.prepareStatement(sql)) {
            st.setString(1, judul);
            st.setString(2, deskripsi);
            st.setTimestamp(3, deadline);
            st.setString(4, prioritas);
            st.setString(5, matkul);
            st.setString(6, tipe);
            st.setString(7, status);
            st.setInt(8, tugasToEdit.getId());
            st.executeUpdate();
        }

        // Delete and re-insert group members
        try (PreparedStatement del = conn.prepareStatement("DELETE FROM anggota_kelompok WHERE tugas_id=?")) {
            del.setInt(1, tugasToEdit.getId());
            del.executeUpdate();
        }

        if ("Kelompok".equals(tipe) && !anggotaKelompok.isEmpty()) {
            insertGroupMembers(conn, tugasToEdit.getId());
        }

        showAlert("Sukses", "Tugas berhasil diperbarui!", Alert.AlertType.INFORMATION);
    }

    private void insertGroupMembers(Connection conn, int tugasId) throws SQLException {
        String sql = "INSERT INTO anggota_kelompok (tugas_id, nama, nim) VALUES (?, ?, ?)";
        try (PreparedStatement stAnggota = conn.prepareStatement(sql)) {
            for (Mahasiswa m : anggotaKelompok) {
                stAnggota.setInt(1, tugasId);
                stAnggota.setString(2, m.getNama());
                stAnggota.setString(3, m.getNim());
                stAnggota.addBatch();
            }
            stAnggota.executeBatch();
        }
    }

    public void setTugasToEdit(Tugas tugas) {
        this.tugasToEdit = tugas;
        
        // Jika komponen belum initialized, tunggu dulu
        if (!isInitialized) {
            Platform.runLater(() -> setTugasToEdit(tugas));
            return;
        }
        
        if (tugas != null) {
            if (btnSimpan != null) {
                btnSimpan.setText("Update");
            }
            
            if (txtJudul != null) txtJudul.setText(tugas.getJudul());
            if (txtDeskripsi != null) txtDeskripsi.setText(tugas.getDeskripsi());
            if (txtMataKuliah != null) txtMataKuliah.setText(tugas.getMataKuliah());
            if (cbPrioritas != null) cbPrioritas.setValue(tugas.getPrioritas());
            if (cbTipe != null) cbTipe.setValue(tugas.getTipe());
            if (cbStatus != null) cbStatus.setValue(tugas.getStatus());

            // Parse deadline
            parseAndSetDeadline(tugas.getDeadline());

            if ("Kelompok".equals(tugas.getTipe())) {
                handleTipeChange("Kelompok");
                loadAnggotaKelompok(tugas.getId());
            }
        }
    }

    private void parseAndSetDeadline(String deadlineStr) {
        try {
            if (deadlineStr != null && !deadlineStr.isEmpty()) {
                Timestamp deadline = Timestamp.valueOf(deadlineStr);
                LocalDate tanggal = deadline.toLocalDateTime().toLocalDate();
                String jam = String.format("%02d", deadline.toLocalDateTime().getHour());
                String menit = String.format("%02d", deadline.toLocalDateTime().getMinute());

                if (datePicker != null) datePicker.setValue(tanggal);
                if (cbJam != null) cbJam.setValue(jam);
                if (cbMenit != null) cbMenit.setValue(menit);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error parsing deadline: " + e.getMessage());
        }
    }

    private void loadAnggotaKelompok(int tugasId) {
        anggotaKelompok.clear();
        anggotaDisplayList.clear();

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT nama, nim FROM anggota_kelompok WHERE tugas_id=?";
            try (PreparedStatement st = conn.prepareStatement(sql)) {
                st.setInt(1, tugasId);
                try (ResultSet rs = st.executeQuery()) {
                    while (rs.next()) {
                        Mahasiswa m = new Mahasiswa(0, rs.getString("nama"), rs.getString("nim"), "", "");
                        anggotaKelompok.add(m);
                        anggotaDisplayList.add("👤 " + m.getNama() + " (" + m.getNim() + ")");
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Gagal load anggota: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    public void setOnTugasAdded(Runnable callback) {
        this.onTugasAdded = callback;
    }

    private void close() {
        try {
            if (btnBatal != null && btnBatal.getScene() != null && btnBatal.getScene().getWindow() != null) {
                ((Stage) btnBatal.getScene().getWindow()).close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String msg, Alert.AlertType type) {
        try {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(msg);
            alert.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to show alert: " + e.getMessage());
        }
    }
}