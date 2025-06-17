package com.example.hongkongpetownersapp;

import com.google.firebase.Timestamp;

public class Photo {
    private String id;
    private String url;
    private String petId;
    private Timestamp createdAt;

    // Empty constructor for Firestore
    public Photo() {}

    // Constructor
    public Photo(String url, String petId) {
        this.url = url;
        this.petId = petId;
        this.createdAt = Timestamp.now();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPetId() {
        return petId;
    }

    public void setPetId(String petId) {
        this.petId = petId;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}