package com.example.hongkongpetownersapp;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

public class HealthReminder {
    @DocumentId
    private String id;
    private String petId;
    private String petName;
    private String type; // feeding, medication, exercise, checkup, grooming, other
    private String title;
    private String description;
    private String frequency; // daily, weekly, monthly, custom
    private int hour; // 0-23
    private int minute; // 0-59
    private boolean isActive;
    private Timestamp createdAt;
    private Timestamp nextReminder;
    private int intervalDays; // for custom frequency
    private String[] daysOfWeek; // for weekly frequency (Mon, Tue, etc.)
    private int dayOfMonth; // for monthly frequency (1-31)

    // Empty constructor for Firestore
    public HealthReminder() {}

    // Constructor
    public HealthReminder(String petId, String petName, String type, String title,
                          String description, String frequency, int hour, int minute) {
        this.petId = petId;
        this.petName = petName;
        this.type = type;
        this.title = title;
        this.description = description;
        this.frequency = frequency;
        this.hour = hour;
        this.minute = minute;
        this.isActive = true;
        this.createdAt = Timestamp.now();
        this.intervalDays = 1; // default for daily
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPetId() { return petId; }
    public void setPetId(String petId) { this.petId = petId; }

    public String getPetName() { return petName; }
    public void setPetName(String petName) { this.petName = petName; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }

    public int getHour() { return hour; }
    public void setHour(int hour) { this.hour = hour; }

    public int getMinute() { return minute; }
    public void setMinute(int minute) { this.minute = minute; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getNextReminder() { return nextReminder; }
    public void setNextReminder(Timestamp nextReminder) { this.nextReminder = nextReminder; }

    public int getIntervalDays() { return intervalDays; }
    public void setIntervalDays(int intervalDays) { this.intervalDays = intervalDays; }

    public String[] getDaysOfWeek() { return daysOfWeek; }
    public void setDaysOfWeek(String[] daysOfWeek) { this.daysOfWeek = daysOfWeek; }

    public int getDayOfMonth() { return dayOfMonth; }
    public void setDayOfMonth(int dayOfMonth) { this.dayOfMonth = dayOfMonth; }

    // Helper method to get icon based on type
    public String getTypeIcon() {
        switch (type) {
            case "feeding": return "🍽️";
            case "medication": return "💊";
            case "exercise": return "🏃";
            case "checkup": return "🏥";
            case "grooming": return "✂️";
            default: return "⏰";
        }
    }
}