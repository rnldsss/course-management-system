package com.coursemanagementsystem.model;

import java.util.ArrayList;
import java.util.List;

public class TugasKelompok extends Tugas implements Notifikasi {
    private List<Mahasiswa> anggota;

    public TugasKelompok(String nama, String deadline, String prioritas, String mataKuliah) {
        super(nama, deadline, prioritas, mataKuliah);
        setTipe("Kelompok");
        this.anggota = new ArrayList<>();
    }

    public TugasKelompok(String nama, String deadline, String prioritas, String mataKuliah, List<Mahasiswa> anggota) {
        super(nama, deadline, prioritas, mataKuliah);
        setTipe("Kelompok");
        this.anggota = anggota;
    }

    public List<Mahasiswa> getAnggota() {
        return anggota;
    }

    public void setAnggota(List<Mahasiswa> anggota) {
        this.anggota = anggota;
    }

    public void tambahAnggota(Mahasiswa mahasiswa) {
        this.anggota.add(mahasiswa);
    }

    @Override
    public void kirimPengingat(Tugas tugas) {
        System.out.println("Pengingat: Tugas Kelompok " + tugas.getNama() + " deadline: " + tugas.getDeadline());
        for (Mahasiswa m : anggota) {
            System.out.println("Notifikasi ke: " + m.getNama());
        }
    }
}
// No changes needed here; fix required in Tugas.java (add constructor and getters).