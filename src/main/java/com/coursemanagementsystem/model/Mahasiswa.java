package com.coursemanagementsystem.model;

public class Mahasiswa {
    private int id;
    private String nama;
    private String nim;
    private String email;
    private String password;

    public Mahasiswa(int id, String nama, String nim, String email, String password) {
        this.id = id;
        this.nama = nama;
        this.nim = nim;
        this.email = email;
        this.password = password;
    }

    public int getId() { return id; }
    public String getNama() { return nama; }
    public String getNim() { return nim; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
}