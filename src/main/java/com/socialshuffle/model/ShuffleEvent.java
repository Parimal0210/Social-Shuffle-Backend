package com.socialshuffle.model;

import com.socialshuffle.converter.StringListConverter;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "events", indexes = {
    @Index(name = "idx_event_status", columnList = "status"),
    @Index(name = "idx_event_date", columnList = "date")
})
public class ShuffleEvent {

    @Id
    @Column(length = 64)
    private String id;

    private int number;
    private String title;

    @Column(length = 50)
    private String date; // YYYY-MM-DD

    @Column(length = 50)
    private String time;

    private String venue;

    @Column(columnDefinition = "TEXT")
    private String address;

    private String area;
    private String category;
    private int capacity;
    private double ticketPrice;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<String> plannedGames = new ArrayList<>();

    @Column(length = 50)
    private String status; // "upcoming", "active", "completed", "archived"

    @Column(columnDefinition = "TEXT")
    private String coverImage;

    public ShuffleEvent() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public double getTicketPrice() {
        return ticketPrice;
    }

    public void setTicketPrice(double ticketPrice) {
        this.ticketPrice = ticketPrice;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<String> getPlannedGames() {
        return plannedGames;
    }

    public void setPlannedGames(List<String> plannedGames) {
        this.plannedGames = plannedGames != null ? plannedGames : new ArrayList<>();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCoverImage() {
        return coverImage;
    }

    public void setCoverImage(String coverImage) {
        this.coverImage = coverImage;
    }
}
