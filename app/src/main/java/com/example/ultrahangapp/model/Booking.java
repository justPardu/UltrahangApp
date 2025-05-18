package com.example.ultrahangapp.model;

import com.google.firebase.firestore.Exclude;

public class Booking {
    private String id; // Firestore dokumentum ID
    private String patientName;
    private String date;
    private String time;

    // Üres konstruktor Firestore számára
    public Booking() {}

    public Booking(String patientName, String date, String time) {
        this.patientName = patientName;
        this.date = date;
        this.time = time;
    }

    @Exclude
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}
