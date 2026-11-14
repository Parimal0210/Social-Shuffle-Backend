package com.socialshuffle.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notif_timestamp", columnList = "\"timestamp\""),
    @Index(name = "idx_notif_read", columnList = "is_read")
})
public class NotificationItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "title")
    private String title;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "type", length = 50)
    private String type; // event, registration, community, system

    @Column(name = "\"timestamp\"", length = 50)
    private String timestamp;

    @JsonProperty("read")
    @JsonAlias({"read", "isRead"})
    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "recipient_email", length = 150)
    private String recipientEmail;

    @Column(name = "recipient_phone", length = 50)
    private String recipientPhone;

    @Column(name = "channel", length = 50)
    private String channel = "dual";

    @Column(name = "event_id", length = 64)
    private String eventId;

    @Column(name = "user_id", length = 64)
    private String userId;

    @Column(name = "registration_id", length = 64)
    private String registrationId;

    @Column(name = "delivery_status", length = 50)
    private String deliveryStatus = "Sent";

    @Column(name = "whatsapp_status", length = 100)
    private String whatsappStatus;

    @Column(name = "email_html", columnDefinition = "TEXT")
    private String emailHtml;

    @Column(name = "action_url", columnDefinition = "TEXT")
    private String actionUrl;

    @Column(name = "created_at", length = 50)
    private String createdAt;

    public NotificationItem() {
    }

    public NotificationItem(String id, String title, String message, String type, String timestamp, boolean read) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.type = type;
        this.timestamp = timestamp;
        this.read = read;
        this.deliveryStatus = "Sent";
    }

    public NotificationItem(String id, String title, String message, String timestamp, boolean read, String eventId) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.type = "registration";
        this.timestamp = timestamp;
        this.read = read;
        this.eventId = eventId;
        this.deliveryStatus = "Sent";
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getDeliveryStatus() {
        return deliveryStatus;
    }

    public void setDeliveryStatus(String deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public String getRecipientPhone() { return recipientPhone; }
    public void setRecipientPhone(String recipientPhone) { this.recipientPhone = recipientPhone; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getRegistrationId() { return registrationId; }
    public void setRegistrationId(String registrationId) { this.registrationId = registrationId; }

    public String getWhatsappStatus() { return whatsappStatus; }
    public void setWhatsappStatus(String whatsappStatus) { this.whatsappStatus = whatsappStatus; }

    public String getEmailHtml() { return emailHtml; }
    public void setEmailHtml(String emailHtml) { this.emailHtml = emailHtml; }

    public String getActionUrl() { return actionUrl; }
    public void setActionUrl(String actionUrl) { this.actionUrl = actionUrl; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NotificationItem that = (NotificationItem) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "NotificationItem{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", type='" + type + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", read=" + read +
                '}';
    }
}
