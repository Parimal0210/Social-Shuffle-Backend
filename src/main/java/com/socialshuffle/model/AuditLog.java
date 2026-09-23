package com.socialshuffle.model;

import jakarta.persistence.*;

@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_timestamp", columnList = "timestamp")
})
public class AuditLog {

    @Id
    @Column(length = 64)
    private String id;

    private String adminName;
    private String action;
    private String target;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 50)
    private String timestamp;

    public AuditLog() {
    }

    public AuditLog(String id, String adminName, String action, String target, String details, String timestamp) {
        this.id = id;
        this.adminName = adminName;
        this.action = action;
        this.target = target;
        this.details = details;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAdminName() {
        return adminName;
    }

    public void setAdminName(String adminName) {
        this.adminName = adminName;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
