package com.socialshuffle.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "event_feedback", indexes = {
    @Index(name = "idx_feedback_event", columnList = "event_id"),
    @Index(name = "idx_feedback_participant", columnList = "participant_id")
})
public class EventFeedback implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "event_id", nullable = false, length = 64)
    private String eventId;

    @Column(name = "event_title")
    private String eventTitle;

    @Column(name = "participant_id", length = 64)
    private String participantId;

    @Column(name = "participant_name")
    private String participantName;

    @Column(name = "anonymous", nullable = false)
    private boolean anonymous = false;

    @Column(name = "overall_rating")
    private int overallRating; // 1-5

    @Column(name = "venue_rating")
    private int venueRating;

    @Column(name = "game_rating")
    private int gameRating;

    @Column(name = "host_rating")
    private int hostRating;

    @Column(name = "suggestions", columnDefinition = "TEXT")
    private String suggestions;

    @Column(name = "created_at", length = 50)
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventFeedback that = (EventFeedback) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "EventFeedback{" +
                "id='" + id + '\'' +
                ", eventId='" + eventId + '\'' +
                ", participantName='" + participantName + '\'' +
                ", overallRating=" + overallRating +
                '}';
    }
}
