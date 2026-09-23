package com.socialshuffle.model;

import jakarta.persistence.*;

@Entity
@Table(name = "safety_reports", indexes = {
    @Index(name = "idx_safety_status", columnList = "status"),
    @Index(name = "idx_safety_event", columnList = "eventId")
})
public class SafetyReport {

    @Id
    @Column(length = 64)
    private String id;

    @Column(length = 64)
    private String eventId;

    private String eventTitle;

    @Column(length = 64)
    private String type; // Event issue, Venue issue, Community concern, Uncomfortable interaction, Harassment, Other

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 50)
    private String dateTime;

    private String personInvolved;
    private boolean anonymous = false;
    private String reporterName;

    @Column(length = 100)
    private String reporterContact;

    @Column(length = 30)
    private String status = "New"; // New, Investigating, Resolved

    @Column(columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(length = 50)
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
}
