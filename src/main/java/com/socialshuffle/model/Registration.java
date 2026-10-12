package com.socialshuffle.model;

import com.socialshuffle.converter.GuestListConverter;
import com.socialshuffle.converter.StringListConverter;
import jakarta.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "registrations", indexes = {
    @Index(name = "idx_reg_event", columnList = "event_id"),
    @Index(name = "idx_reg_participant", columnList = "participant_id"),
    @Index(name = "idx_reg_event_part", columnList = "event_id, participant_id"),
    @Index(name = "idx_reg_attendance", columnList = "attendance_status"),
    @Index(name = "idx_reg_payment", columnList = "payment_status"),
    @Index(name = "idx_reg_qr_token", columnList = "qr_code_token"),
    @Index(name = "idx_reg_ticket_code", columnList = "ticket_code"),
    @Index(name = "idx_reg_razorpay", columnList = "razorpay_payment_id")
})
public class Registration implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "event_id", nullable = false, length = 64)
    private String eventId;

    @Column(name = "participant_id", nullable = false, length = 64)
    private String participantId;

    @Column(name = "participant_name")
    private String participantName;

    @Column(name = "participant_email")
    private String participantEmail;

    @Column(name = "participant_phone")
    private String participantPhone;

    @Column(name = "participant_area")
    private String participantArea;

    @Column(name = "pax_count")
    private int paxCount = 1;

    @Convert(converter = GuestListConverter.class)
    @Column(name = "guests", columnDefinition = "TEXT")
    private List<GuestInfo> guests = new ArrayList<>();

    @Column(name = "payment_status", length = 30)
    private String paymentStatus = "Pending"; // Confirmed, Pending, Waived

    @Column(name = "attendance_status", length = 30)
    private String attendanceStatus = "Pending"; // Checked In, Pending, Cancelled, No Show

    @Column(name = "check_in_time", length = 50)
    private String checkInTime;

    @Column(name = "registered_at", length = 50)
    private String registeredAt;

    @Column(name = "razorpay_payment_id", length = 64)
    private String razorpayPaymentId;

    @Column(name = "razorpay_order_id", length = 64)
    private String razorpayOrderId;

    @Column(name = "amount_paid")
    private Double amountPaid;

    @Column(name = "qr_code_token", length = 64)
    private String qrCodeToken;

    @Column(name = "qr_code_url", columnDefinition = "TEXT")
    private String qrCodeUrl;

    @Column(name = "email_sent")
    private Boolean emailSent = false;

    @Column(name = "email_sent_at", length = 50)
    private String emailSentAt;

    @Convert(converter = StringListConverter.class)
    @Column(name = "games_played", columnDefinition = "TEXT")
    private List<String> gamesPlayed = new ArrayList<>();

    @Column(name = "ticket_code", length = 64)
    private String ticketCode;

    public Registration() {
    }

    public Registration(String id, String eventId, String participantId, String participantName,
                        String participantEmail, String participantPhone, int paxCount,
                        String paymentStatus, String razorpayPaymentId, Double amountPaid,
                        String attendanceStatus, String registeredAt) {
        this.id = id;
        this.eventId = eventId;
        this.participantId = participantId;
        this.participantName = participantName;
        this.participantEmail = participantEmail;
        this.participantPhone = participantPhone;
        this.paxCount = paxCount;
        this.paymentStatus = paymentStatus;
        this.razorpayPaymentId = razorpayPaymentId;
        this.amountPaid = amountPaid;
        this.attendanceStatus = attendanceStatus;
        this.registeredAt = registeredAt;
    }

    public String getTicketCode() {
        return ticketCode != null ? ticketCode : qrCodeToken;
    }

    public void setTicketCode(String ticketCode) {
        this.ticketCode = ticketCode;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Registration that = (Registration) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Registration{" +
                "id='" + id + '\'' +
                ", eventId='" + eventId + '\'' +
                ", participantId='" + participantId + '\'' +
                ", participantName='" + participantName + '\'' +
                ", paymentStatus='" + paymentStatus + '\'' +
                ", attendanceStatus='" + attendanceStatus + '\'' +
                '}';
    }
}
