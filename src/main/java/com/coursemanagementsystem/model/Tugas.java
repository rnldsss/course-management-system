package com.coursemanagementsystem.model;

public class Tugas {
    private int id;
    private String judul;
    private String deskripsi;
    private String deadline;
    private String prioritas;
    private String mataKuliah;
    private String tipe;
    private String status;
    private String uploadPath;

    // Constructor utama (untuk database)
    public Tugas(int id, String judul, String deskripsi, String deadline, String prioritas, String mataKuliah, String tipe) {
        this.id = id;
        this.judul = judul;
        this.deskripsi = deskripsi;
        this.deadline = deadline;
        this.prioritas = prioritas;
        this.mataKuliah = mataKuliah;
        this.tipe = tipe;
        this.status = "Belum Dikerjakan";
        this.uploadPath = "";
    }

    // Constructor tambahan (untuk subclass, tidak dipakai jika tidak perlu)
    public Tugas(String judul, String deadline, String prioritas, String mataKuliah) {
        this(0, judul, "", deadline, prioritas, mataKuliah, "");
    }

    // Supaya subclass bisa set tipe
    protected void setTipe(String tipe) {
        this.tipe = tipe;
    }

    public int getId() { return id; }
    public String getJudul() { return judul; }
    public String getDeskripsi() { return deskripsi; }
    public String getDeadline() { return deadline; }
    public String getPrioritas() { return prioritas; }
    public String getMataKuliah() { return mataKuliah; }
    public String getTipe() { return tipe; }
    public String getStatus() { return status; }
    public String getUploadPath() { return uploadPath; }

    public void setStatus(String status) { this.status = status; }
    public void setUploadPath(String uploadPath) { this.uploadPath = uploadPath; }

    // Untuk kompatibilitas getNama() (jika di subclass butuh)
    public String getNama() { return judul; }

    // Constructor tambahan sesuai permintaan
    public Tugas(int id, String judul, String deadline, String prioritas, String mataKuliah, String tipe) {
        this.id = id;
        this.judul = judul;
        this.deskripsi = "";
        this.deadline = deadline;
        this.prioritas = prioritas;
        this.mataKuliah = mataKuliah;
        this.tipe = tipe;
        this.status = "Belum Dikerjakan";
        this.uploadPath = "";
    }
}