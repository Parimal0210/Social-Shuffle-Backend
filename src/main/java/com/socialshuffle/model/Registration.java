package com.socialshuffle.model;

import com.socialshuffle.converter.GuestListConverter;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "registrations", indexes = {
    @Index(name = "idx_reg_event", columnList = "eventId"),
    @Index(name = "idx_reg_participant", columnList = "participantId"),
    @Index(name = "idx_reg_event_part", columnList = "eventId, participantId"),
    @Index(name = "idx_reg_attendance", columnList = "attendanceStatus")
})
public class Registration {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 64)
    private String eventId;

    @Column(nullable = false, length = 64)
    private String participantId;

    private String participantName;
    private String participantEmail;
    private String participantPhone;
    private String participantArea;

    private int paxCount = 1;

    @Convert(converter = GuestListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<GuestInfo> guests = new ArrayList<>();

    @Column(length = 30)
    private String paymentStatus = "Pending"; // Confirmed, Pending, Waived

    @Column(length = 30)
    private String attendanceStatus = "Pending"; // Checked In, Pending, Cancelled, No Show

    @Column(length = 50)
    private String checkInTime;

    @Column(length = 50)
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

    public String getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(String registeredAt) {
        this.registeredAt = registeredAt;
    }
}
