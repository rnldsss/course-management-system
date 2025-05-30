package com.coursemanagementsystem.model;

public class Tugas {
    protected String nama;
    protected String deadline;
    protected String prioritas;
    protected String mataKuliah;
    protected String tipe; 
    protected String status; 
    protected String uploadPath; 

    public Tugas(String nama, String deadline, String prioritas, String mataKuliah) {
        this.nama = nama;
        this.deadline = deadline;
        this.prioritas = prioritas;
        this.mataKuliah = mataKuliah;
        this.tipe = "Individu"; // default
        this.status = "Belum Dikerjakan"; // default
        this.uploadPath = null; // default belum upload
    }

    // Getter dan setter untuk status
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // Getter dan setter untuk uploadPath
    public String getUploadPath() {
        return uploadPath;
    }

    public void setUploadPath(String uploadPath) {
        this.uploadPath = uploadPath;
    }

    // Getter dan setter lainnya
    public String getNama() {
        return nama;
    }

    public void setNama(String nama) {
        this.nama = nama;
    }

    public String getDeadline() {
        return deadline;
    }

    public void setDeadline(String deadline) {
        this.deadline = deadline;
    }

    public String getPrioritas() {
        return prioritas;
    }

    public void setPrioritas(String prioritas) {
        this.prioritas = prioritas;
    }

    public String getMataKuliah() {
        return mataKuliah;
    }

    public void setMataKuliah(String mataKuliah) {
        this.mataKuliah = mataKuliah;
    }

    public String getTipe() {
        return tipe;
    }

    public void setTipe(String tipe) {
        this.tipe = tipe;
    }
}
