package com.example.hongkongpetownersapp;

import com.google.firebase.Timestamp;

public class Pet {
    private String id;
    private String name;
    private String type;
    private String ownerId;
    private Timestamp createdAt;

    public Pet() {}

    public Pet(String name, String type, String ownerId) {
        this.name = name;
        this.type = type;
        this.ownerId = ownerId;
        this.createdAt = Timestamp.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}