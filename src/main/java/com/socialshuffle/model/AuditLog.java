package com.socialshuffle.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_timestamp", columnList = "\"timestamp\"")
})
public class AuditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "admin_name")
    private String adminName;

    @Column(name = "action")
    private String action;

    @Column(name = "target")
    private String target;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "\"timestamp\"", length = 50)
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

    public AuditLog(String id, String adminName, String action, String details, String timestamp) {
        this(id, adminName, action, "System", details, timestamp);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuditLog auditLog = (AuditLog) o;
        return Objects.equals(id, auditLog.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "AuditLog{" +
                "id='" + id + '\'' +
                ", adminName='" + adminName + '\'' +
                ", action='" + action + '\'' +
                ", target='" + target + '\'' +
                ", timestamp='" + timestamp + '\'' +
                '}';
    }
}
