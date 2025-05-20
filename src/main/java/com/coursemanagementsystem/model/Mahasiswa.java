package com.coursemanagementsystem.model;

import java.util.ArrayList;
import java.util.List;

public class Mahasiswa {
    private String nama;
    private List<Tugas> daftarTugas = new ArrayList<>();

    public Mahasiswa(String nama) {
        this.nama = nama;
    }

    public String getNama() { return nama; }
    public List<Tugas> getDaftarTugas() { return daftarTugas; }

    public void tambahTugas(Tugas tugas) {
        daftarTugas.add(tugas);
    }

    public void hapusTugas(Tugas tugas) {
        daftarTugas.remove(tugas);
    }
}