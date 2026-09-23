package com.socialshuffle.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "registrations")
@CompoundIndex(def = "{'eventId': 1, 'participantId': 1}")
public class Registration {

    @Id
    private String id;

    @Indexed
    private String eventId;

    @Indexed
    private String participantId;

    private String participantName;
    private String participantEmail;
    private String participantPhone;
    private String participantArea;

    private int paxCount = 1;
    private List<GuestInfo> guests = new ArrayList<>();

    private String paymentStatus = "Pending"; // Confirmed, Pending, Waived
    private String attendanceStatus = "Pending"; // Checked In, Pending, Cancelled, No Show
    private String checkInTime;

    private List<String> gamesPlayed = new ArrayList<>();
    private String notes;
    private String registeredAt;

    public Registration() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getParticipantId() {
        return participantId;
    }

    public void setParticipantId(String participantId) {
        this.participantId = participantId;
    }

    public String getParticipantName() {
        return participantName;
    }

    public void setParticipantName(String participantName) {
        this.participantName = participantName;
    }

    public String getParticipantEmail() {
        return participantEmail;
    }

    public void setParticipantEmail(String participantEmail) {
        this.participantEmail = participantEmail;
    }

    public String getParticipantPhone() {
        return participantPhone;
    }

    public void setParticipantPhone(String participantPhone) {
        this.participantPhone = participantPhone;
    }

    public String getParticipantArea() {
        return participantArea;
    }

    public void setParticipantArea(String participantArea) {
        this.participantArea = participantArea;
    }

    public int getPaxCount() {
        return paxCount;
    }

    public void setPaxCount(int paxCount) {
        this.paxCount = paxCount;
    }

    public List<GuestInfo> getGuests() {
        return guests;
    }

    public void setGuests(List<GuestInfo> guests) {
        this.guests = guests != null ? guests : new ArrayList<>();
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getAttendanceStatus() {
        return attendanceStatus;
    }

    public void setAttendanceStatus(String attendanceStatus) {
        this.attendanceStatus = attendanceStatus;
    }

    public String getCheckInTime() {
        return checkInTime;
    }

    public void setCheckInTime(String checkInTime) {
        this.checkInTime = checkInTime;
    }

    public List<String> getGamesPlayed() {
        return gamesPlayed;
    }

    public void setGamesPlayed(List<String> gamesPlayed) {
        this.gamesPlayed = gamesPlayed != null ? gamesPlayed : new ArrayList<>();
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(String registeredAt) {
        this.registeredAt = registeredAt;
    }
}
