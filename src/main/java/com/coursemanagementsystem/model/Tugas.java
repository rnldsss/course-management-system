package com.coursemanagementsystem.model;

public class Tugas {
    private int id;
    private String judul;
    private String deskripsi;
    private String deadline;
    private String prioritas;
    private String mataKuliah;
    private String tipe;

    // Constructor utama (untuk database)
    public Tugas(int id, String judul, String deskripsi, String deadline, String prioritas, String mataKuliah, String tipe) {
        this.id = id;
        this.judul = judul;
        this.deskripsi = deskripsi;
        this.deadline = deadline;
        this.prioritas = prioritas;
        this.mataKuliah = mataKuliah;
        this.tipe = tipe;
    }

    // Constructor tambahan (untuk subclass)
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
    // Untuk kompatibilitas getNama() (jika di subclass butuh)
    public String getNama() { return judul; }
}