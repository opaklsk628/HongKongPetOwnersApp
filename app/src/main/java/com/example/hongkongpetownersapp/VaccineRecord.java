package com.example.hongkongpetownersapp;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

public class VaccineRecord {
    @DocumentId
    private String id;
    private String petId;
    private String vaccineName;
    private String veterinarian;
    private String clinic;
    private Timestamp vaccinationDate;
    private Timestamp nextDueDate;
    private String notes;
    private Timestamp createdAt;

    // Empty constructor for Firestore
    public VaccineRecord() {}

    // Constructor
    public VaccineRecord(String petId, String vaccineName, String veterinarian,
                         String clinic, Timestamp vaccinationDate, Timestamp nextDueDate,
                         String notes) {
        this.petId = petId;
        this.vaccineName = vaccineName;
        this.veterinarian = veterinarian;
        this.clinic = clinic;
        this.vaccinationDate = vaccinationDate;
        this.nextDueDate = nextDueDate;
        this.notes = notes;
        this.createdAt = Timestamp.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPetId() { return petId; }
    public void setPetId(String petId) { this.petId = petId; }

    public String getVaccineName() { return vaccineName; }
    public void setVaccineName(String vaccineName) { this.vaccineName = vaccineName; }

    public String getVeterinarian() { return veterinarian; }
    public void setVeterinarian(String veterinarian) { this.veterinarian = veterinarian; }

    public String getClinic() { return clinic; }
    public void setClinic(String clinic) { this.clinic = clinic; }

    public Timestamp getVaccinationDate() { return vaccinationDate; }
    public void setVaccinationDate(Timestamp vaccinationDate) { this.vaccinationDate = vaccinationDate; }

    public Timestamp getNextDueDate() { return nextDueDate; }
    public void setNextDueDate(Timestamp nextDueDate) { this.nextDueDate = nextDueDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}