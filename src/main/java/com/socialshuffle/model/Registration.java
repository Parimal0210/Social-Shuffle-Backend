package com.socialshuffle.model;

import com.socialshuffle.converter.GuestListConverter;
import com.socialshuffle.converter.StringListConverter;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "registrations", indexes = {
    @Index(name = "idx_reg_event", columnList = "eventId"),
    @Index(name = "idx_reg_participant", columnList = "participantId"),
    @Index(name = "idx_reg_event_part", columnList = "eventId, participantId"),
    @Index(name = "idx_reg_attendance", columnList = "attendanceStatus"),
    @Index(name = "idx_reg_payment", columnList = "paymentStatus"),
    @Index(name = "idx_reg_qr_token", columnList = "qrCodeToken"),
    @Index(name = "idx_reg_razorpay", columnList = "razorpayPaymentId")
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

    @Column(length = 64)
    private String razorpayPaymentId;

    @Column(length = 64)
    private String razorpayOrderId;

    private Double amountPaid;

    @Column(length = 64)
    private String qrCodeToken;

    @Column(columnDefinition = "TEXT")
    private String qrCodeUrl;

    private Boolean emailSent = false;

    @Column(length = 50)
    private String emailSentAt;

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<String> gamesPlayed = new ArrayList<>();

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

    public String getRazorpayPaymentId() {
        return razorpayPaymentId;
    }

    public void setRazorpayPaymentId(String razorpayPaymentId) {
        this.razorpayPaymentId = razorpayPaymentId;
    }

    public String getRazorpayOrderId() {
        return razorpayOrderId;
    }

    public void setRazorpayOrderId(String razorpayOrderId) {
        this.razorpayOrderId = razorpayOrderId;
    }

    public Double getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(Double amountPaid) {
        this.amountPaid = amountPaid;
    }

    public String getQrCodeToken() {
        return qrCodeToken;
    }

    public void setQrCodeToken(String qrCodeToken) {
        this.qrCodeToken = qrCodeToken;
    }

    public String getQrCodeUrl() {
        return qrCodeUrl;
    }

    public void setQrCodeUrl(String qrCodeUrl) {
        this.qrCodeUrl = qrCodeUrl;
    }

    public Boolean getEmailSent() {
        return emailSent != null ? emailSent : false;
    }

    public void setEmailSent(Boolean emailSent) {
        this.emailSent = emailSent;
    }

    public String getEmailSentAt() {
        return emailSentAt;
    }

    public void setEmailSentAt(String emailSentAt) {
        this.emailSentAt = emailSentAt;
    }

    public List<String> getGamesPlayed() {
        return gamesPlayed != null ? gamesPlayed : new ArrayList<>();
    }

    public void setGamesPlayed(List<String> gamesPlayed) {
        this.gamesPlayed = gamesPlayed != null ? gamesPlayed : new ArrayList<>();
    }
}
