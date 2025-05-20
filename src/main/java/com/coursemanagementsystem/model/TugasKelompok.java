package com.coursemanagementsystem.model;

import java.util.List;

public class TugasKelompok extends Tugas implements Notifikasi {
    private List<Mahasiswa> anggota;

    public TugasKelompok(String nama, String deadline, String prioritas, String mataKuliah, List<Mahasiswa> anggota) {
        super(nama, deadline, prioritas, mataKuliah);
        this.anggota = anggota;
    }

    public List<Mahasiswa> getAnggota() {
        return anggota;
    }

    public void setAnggota(List<Mahasiswa> anggota) {
        this.anggota = anggota;
    }

    @Override
    public void kirimPengingat(Tugas tugas) {
        System.out.println("Pengingat: Tugas Kelompok " + tugas.getNama() + " deadline: " + tugas.getDeadline());
        for (Mahasiswa m : anggota) {
            System.out.println("Notifikasi ke: " + m.getNama());
        }
    }
}