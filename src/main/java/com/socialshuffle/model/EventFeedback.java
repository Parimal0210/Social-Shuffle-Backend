package com.socialshuffle.model;

import jakarta.persistence.*;

@Entity
@Table(name = "event_feedback", indexes = {
    @Index(name = "idx_feedback_event", columnList = "eventId"),
    @Index(name = "idx_feedback_participant", columnList = "participantId")
})
public class EventFeedback {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 64)
    private String eventId;

    private String eventTitle;

    @Column(length = 64)
    private String participantId;

    private String participantName;

    private boolean anonymous = false;

    private int overallRating; // 1-5
    private int venueRating;
    private int gameRating;
    private int hostRating;

    @Column(columnDefinition = "TEXT")
    private String suggestions;

    @Column(length = 50)
    private String createdAt;

    public EventFeedback() {
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

    public boolean isAnonymous() {
        return anonymous;
    }

    public void setAnonymous(boolean anonymous) {
        this.anonymous = anonymous;
    }

    public int getOverallRating() {
        return overallRating;
    }

    public void setOverallRating(int overallRating) {
        this.overallRating = overallRating;
    }

    public int getVenueRating() {
        return venueRating;
    }

    public void setVenueRating(int venueRating) {
        this.venueRating = venueRating;
    }

    public int getGameRating() {
        return gameRating;
    }

    public void setGameRating(int gameRating) {
        this.gameRating = gameRating;
    }

    public int getHostRating() {
        return hostRating;
    }

    public void setHostRating(int hostRating) {
        this.hostRating = hostRating;
    }

    public String getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(String suggestions) {
        this.suggestions = suggestions;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
