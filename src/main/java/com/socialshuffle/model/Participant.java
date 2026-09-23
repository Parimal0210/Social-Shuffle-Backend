package com.socialshuffle.model;

import com.socialshuffle.converter.StringListConverter;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "participants", indexes = {
    @Index(name = "idx_participant_email", columnList = "email"),
    @Index(name = "idx_participant_phone", columnList = "phone"),
    @Index(name = "idx_participant_area", columnList = "area")
})
public class Participant {

    @Id
    @Column(length = 64)
    private String id;

    private String name;

    @Column(unique = true, nullable = false, length = 150)
    private String email;

    @Column(length = 50)
    private String phone;

    private String area;

    @Column(columnDefinition = "TEXT")
    private String avatar;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(length = 50)
    private String joinedDate;

    private int totalEventsAttended;
    private int totalRegistrations;

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<String> gamesPlayedIds = new ArrayList<>();

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<String> venuesVisited = new ArrayList<>();

    private int totalPaxBrought;

    @Column(length = 64)
    private String lastAttendedEventId;

    private String lastAttendedEventTitle;

    public Participant() {
    }

    public Participant(String id, String name, String email, String phone, String area) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.area = area;
        this.joinedDate = "2024-01-01";
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getJoinedDate() {
        return joinedDate;
    }

    public void setJoinedDate(String joinedDate) {
        this.joinedDate = joinedDate;
    }

    public int getTotalEventsAttended() {
        return totalEventsAttended;
    }

    public void setTotalEventsAttended(int totalEventsAttended) {
        this.totalEventsAttended = totalEventsAttended;
    }

    public int getTotalRegistrations() {
        return totalRegistrations;
    }

    public void setTotalRegistrations(int totalRegistrations) {
        this.totalRegistrations = totalRegistrations;
    }

    public List<String> getGamesPlayedIds() {
        return gamesPlayedIds;
    }

    public void setGamesPlayedIds(List<String> gamesPlayedIds) {
        this.gamesPlayedIds = gamesPlayedIds != null ? gamesPlayedIds : new ArrayList<>();
    }

    public List<String> getVenuesVisited() {
        return venuesVisited;
    }

    public void setVenuesVisited(List<String> venuesVisited) {
        this.venuesVisited = venuesVisited != null ? venuesVisited : new ArrayList<>();
    }

    public int getTotalPaxBrought() {
        return totalPaxBrought;
    }

    public void setTotalPaxBrought(int totalPaxBrought) {
        this.totalPaxBrought = totalPaxBrought;
    }

    public String getLastAttendedEventId() {
        return lastAttendedEventId;
    }

    public void setLastAttendedEventId(String lastAttendedEventId) {
        this.lastAttendedEventId = lastAttendedEventId;
    }

    public String getLastAttendedEventTitle() {
        return lastAttendedEventTitle;
    }

    public void setLastAttendedEventTitle(String lastAttendedEventTitle) {
        this.lastAttendedEventTitle = lastAttendedEventTitle;
    }
}
