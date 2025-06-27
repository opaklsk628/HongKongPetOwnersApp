package com.example.hongkongpetownersapp;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

public class WalkingRecord {
    @DocumentId
    private String id;
    private String petId;
    private String petName;
    private String ownerId;
    private int steps;
    private double distance; // in kilometers
    private long duration; // in milliseconds
    private Timestamp startTime;
    private Timestamp endTime;
    private double avgSpeed; // km/h

    // Empty constructor for Firestore
    public WalkingRecord() {}

    // Constructor
    public WalkingRecord(String petId, String petName, String ownerId) {
        this.petId = petId;
        this.petName = petName;
        this.ownerId = ownerId;
        this.steps = 0;
        this.distance = 0.0;
        this.duration = 0;
        this.startTime = Timestamp.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPetId() { return petId; }
    public void setPetId(String petId) { this.petId = petId; }

    public String getPetName() { return petName; }
    public void setPetName(String petName) { this.petName = petName; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public int getSteps() { return steps; }
    public void setSteps(int steps) { this.steps = steps; }

    public double getDistance() { return distance; }
    public void setDistance(double distance) { this.distance = distance; }

    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }

    public Timestamp getStartTime() { return startTime; }
    public void setStartTime(Timestamp startTime) { this.startTime = startTime; }

    public Timestamp getEndTime() { return endTime; }
    public void setEndTime(Timestamp endTime) { this.endTime = endTime; }

    public double getAvgSpeed() { return avgSpeed; }
    public void setAvgSpeed(double avgSpeed) { this.avgSpeed = avgSpeed; }

    // Helper method to calculate average speed
    public void calculateAvgSpeed() {
        if (duration > 0) {
            double hours = duration / (1000.0 * 60 * 60);
            this.avgSpeed = distance / hours;
        }
    }

    // Helper method to format duration
    public String getFormattedDuration() {
        long seconds = duration / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        if (hours > 0) {
            return String.format("%d h %d min", hours, minutes % 60);
        } else {
            return String.format("%d min", minutes);
        }
    }
}