package com.socialshuffle.model;

import com.socialshuffle.converter.StringListConverter;
import jakarta.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "participants", indexes = {
    @Index(name = "idx_participant_email", columnList = "email"),
    @Index(name = "idx_participant_phone", columnList = "phone"),
    @Index(name = "idx_participant_area", columnList = "area")
})
public class Participant implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "name")
    private String name;

    @Column(name = "email", unique = true, nullable = false, length = 150)
    private String email;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "area")
    private String area;

    @Column(name = "avatar", columnDefinition = "TEXT")
    private String avatar;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "joined_date", length = 50)
    private String joinedDate;

    @Column(name = "total_events_attended")
    private int totalEventsAttended;

    @Column(name = "total_registrations")
    private int totalRegistrations;

    @Convert(converter = StringListConverter.class)
    @Column(name = "games_played_ids", columnDefinition = "TEXT")
    private List<String> gamesPlayedIds = new ArrayList<>();

    @Convert(converter = StringListConverter.class)
    @Column(name = "venues_visited", columnDefinition = "TEXT")
    private List<String> venuesVisited = new ArrayList<>();

    @Column(name = "total_pax_brought")
    private int totalPaxBrought;

    @Column(name = "last_attended_event_id", length = 64)
    private String lastAttendedEventId;

    @Column(name = "last_attended_event_title")
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Participant that = (Participant) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Participant{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", area='" + area + '\'' +
                ", totalEventsAttended=" + totalEventsAttended +
                '}';
    }
}
