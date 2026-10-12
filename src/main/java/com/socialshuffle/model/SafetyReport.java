package com.socialshuffle.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "safety_reports", indexes = {
    @Index(name = "idx_safety_status", columnList = "status"),
    @Index(name = "idx_safety_event", columnList = "event_id")
})
public class SafetyReport implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "event_id", length = 64)
    private String eventId;

    @Column(name = "event_title")
    private String eventTitle;

    @Column(name = "type", length = 64)
    private String type; // Event issue, Venue issue, Community concern, Uncomfortable interaction, Harassment, Other

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "date_time", length = 50)
    private String dateTime;

    @Column(name = "person_involved")
    private String personInvolved;

    @Column(name = "anonymous", nullable = false)
    private boolean anonymous = false;

    @Column(name = "reporter_name")
    private String reporterName;

    @Column(name = "reporter_contact", length = 100)
    private String reporterContact;

    @Column(name = "status", length = 30)
    private String status = "New"; // New, Investigating, Resolved

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "created_at", length = 50)
    private String createdAt;

    public SafetyReport() {
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

    public String getEventTitle() {
        return eventTitle;
    }

    public void setEventTitle(String eventTitle) {
        this.eventTitle = eventTitle;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public String getPersonInvolved() {
        return personInvolved;
    }

    public void setPersonInvolved(String personInvolved) {
        this.personInvolved = personInvolved;
    }

    public boolean isAnonymous() {
        return anonymous;
    }

    public void setAnonymous(boolean anonymous) {
        this.anonymous = anonymous;
    }

    public String getReporterName() {
        return reporterName;
    }

    public void setReporterName(String reporterName) {
        this.reporterName = reporterName;
    }

    public String getReporterContact() {
        return reporterContact;
    }

    public void setReporterContact(String reporterContact) {
        this.reporterContact = reporterContact;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SafetyReport that = (SafetyReport) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "SafetyReport{" +
                "id='" + id + '\'' +
                ", eventId='" + eventId + '\'' +
                ", type='" + type + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
