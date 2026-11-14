package com.socialshuffle.model;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "google_users", indexes = {
    @Index(name = "idx_google_participant", columnList = "participant_id")
})
public class GoogleUser implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "google_id", length = 150)
    private String googleId;

    @Column(name = "participant_id", length = 64)
    private String participantId;

    @Column(name = "user_json", nullable = false, columnDefinition = "TEXT")
    private String userJson;

    @Column(name = "registered_at", nullable = false, length = 50)
    private String registeredAt;

    public GoogleUser() {
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getGoogleId() { return googleId; }
    public void setGoogleId(String googleId) { this.googleId = googleId; }

    public String getParticipantId() { return participantId; }
    public void setParticipantId(String participantId) { this.participantId = participantId; }

    public String getUserJson() { return userJson; }
    public void setUserJson(String userJson) { this.userJson = userJson; }

    public String getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(String registeredAt) { this.registeredAt = registeredAt; }
}
