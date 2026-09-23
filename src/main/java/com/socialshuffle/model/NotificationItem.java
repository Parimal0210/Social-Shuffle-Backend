package com.socialshuffle.model;

import jakarta.persistence.*;

@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notif_timestamp", columnList = "timestamp"),
    @Index(name = "idx_notif_read", columnList = "isRead")
})
public class NotificationItem {

    @Id
    @Column(length = 64)
    private String id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(length = 50)
    private String type; // event, registration, community, system

    @Column(length = 50)
    private String timestamp;

    @Column(name = "is_read")
    private boolean read = false;

    public NotificationItem() {
    }

    public NotificationItem(String id, String title, String message, String type, String timestamp, boolean read) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.type = type;
        this.timestamp = timestamp;
        this.read = read;
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
}
