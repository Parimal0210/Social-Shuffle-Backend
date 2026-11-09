package com.socialshuffle.model;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "venues", indexes = {
    @Index(name = "idx_venues_area", columnList = "area"),
    @Index(name = "idx_venues_active", columnList = "active")
})
public class Venue implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "area", length = 100)
    private String area;

    @Column(name = "capacity")
    private int capacity;

    @Column(name = "contact_phone", length = 50)
    private String contactPhone;

    @Column(name = "maps_url", columnDefinition = "TEXT")
    private String mapsUrl;

    @Column(name = "image", columnDefinition = "TEXT")
    private String image;

    @Column(name = "active")
    private boolean active = true;

    public Venue() {
    }

    public Venue(String id, String name, String address, String area, int capacity, String contactPhone, String mapsUrl, String image, boolean active) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.area = area;
        this.capacity = capacity;
        this.contactPhone = contactPhone;
        this.mapsUrl = mapsUrl;
        this.image = image;
        this.active = active;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getMapsUrl() {
        return mapsUrl;
    }

    public void setMapsUrl(String mapsUrl) {
        this.mapsUrl = mapsUrl;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
