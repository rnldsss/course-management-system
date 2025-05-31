package com.coursemanagementsystem.model;

import javafx.beans.property.*;

public class Tugas {
    private final IntegerProperty id;
    private final StringProperty judul;
    private final StringProperty deskripsi;
    private final StringProperty deadline;
    private final StringProperty prioritas;
    private final StringProperty mataKuliah;
    private final StringProperty tipe;
    private final StringProperty status;

    public Tugas(int id, String judul, String deskripsi, String deadline, String prioritas, String mataKuliah, String tipe, String status) {
        this.id = new SimpleIntegerProperty(id);
        this.judul = new SimpleStringProperty(judul);
        this.deskripsi = new SimpleStringProperty(deskripsi);
        this.deadline = new SimpleStringProperty(deadline);
        this.prioritas = new SimpleStringProperty(prioritas);
        this.mataKuliah = new SimpleStringProperty(mataKuliah);
        this.tipe = new SimpleStringProperty(tipe);
        this.status = new SimpleStringProperty(status);
    }

    // Getter
    public int getId() { return id.get(); }
    public String getJudul() { return judul.get(); }
    public String getDeskripsi() { return deskripsi.get(); }
    public String getDeadline() { return deadline.get(); }
    public String getPrioritas() { return prioritas.get(); }
    public String getMataKuliah() { return mataKuliah.get(); }
    public String getTipe() { return tipe.get(); }
    public String getStatus() { return status.get(); }

    // Property
    public IntegerProperty idProperty() { return id; }
    public StringProperty judulProperty() { return judul; }
    public StringProperty deskripsiProperty() { return deskripsi; }
    public StringProperty deadlineProperty() { return deadline; }
    public StringProperty prioritasProperty() { return prioritas; }
    public StringProperty mataKuliahProperty() { return mataKuliah; }
    public StringProperty tipeProperty() { return tipe; }
    public StringProperty statusProperty() { return status; }

    // Setter
    public void setId(int id) { this.id.set(id); }
    public void setJudul(String judul) { this.judul.set(judul); }
    public void setDeskripsi(String deskripsi) { this.deskripsi.set(deskripsi); }
    public void setDeadline(String deadline) { this.deadline.set(deadline); }
    public void setPrioritas(String prioritas) { this.prioritas.set(prioritas); }
    public void setMataKuliah(String mataKuliah) { this.mataKuliah.set(mataKuliah); }
    public void setTipe(String tipe) { this.tipe.set(tipe); }
    public void setStatus(String status) { this.status.set(status); }
}