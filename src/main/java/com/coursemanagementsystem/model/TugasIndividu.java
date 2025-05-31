package com.coursemanagementsystem.model;

public class TugasIndividu extends Tugas implements Notifikasi {
    public TugasIndividu(String nama, String deadline, String prioritas, String mataKuliah) {
        super(nama, deadline, prioritas);
        setMataKuliah(mataKuliah);
        setTipe("Individu");
    }

    @Override
    public void kirimPengingat(Tugas tugas) {
        System.out.println("Pengingat: Tugas Individu " + tugas.getNama() + " deadline: " + tugas.getDeadline());
    }
}