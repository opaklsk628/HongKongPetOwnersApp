package com.example.hongkongpetownersapp;

import com.google.android.gms.maps.model.LatLng;

public class PetPark {
    private String name;
    private String district;
    private String area;
    private LatLng location;
    private boolean hasDesignatedArea; // ⭐
    private boolean hasWasteBin;       // 💩
    private boolean hasToilet;         // 🚻

    public PetPark(String name, String district, String area, LatLng location,
                   boolean hasDesignatedArea, boolean hasWasteBin, boolean hasToilet) {
        this.name = name;
        this.district = district;
        this.area = area;
        this.location = location;
        this.hasDesignatedArea = hasDesignatedArea;
        this.hasWasteBin = hasWasteBin;
        this.hasToilet = hasToilet;
    }

    // Getters
    public String getName() { return name; }
    public String getDistrict() { return district; }
    public String getArea() { return area; }
    public LatLng getLocation() { return location; }
    public boolean hasDesignatedArea() { return hasDesignatedArea; }
    public boolean hasWasteBin() { return hasWasteBin; }
    public boolean hasToilet() { return hasToilet; }

    // Get facilities string for map snippet in English format
    public String getFacilitiesString() {
        StringBuilder facilities = new StringBuilder();
        facilities.append("Facilities: ");

        boolean hasAnyFacility = false;

        if (hasDesignatedArea) {
            facilities.append("⭐ Designated pet area only ");
            hasAnyFacility = true;
        }
        if (hasWasteBin) {
            facilities.append("💩 Feces collection box ");
            hasAnyFacility = true;
        }
        if (hasToilet) {
            facilities.append("🚻 Toilet");
            hasAnyFacility = true;
        }

        if (!hasAnyFacility) {
            return "Facilities: None specified";
        }

        return facilities.toString().trim();
    }
}