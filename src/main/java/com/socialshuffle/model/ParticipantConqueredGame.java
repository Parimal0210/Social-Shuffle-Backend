package com.socialshuffle.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "participant_conquered_games", indexes = {
    @Index(name = "idx_pcg_participant", columnList = "participant_id"),
    @Index(name = "idx_pcg_library_game", columnList = "library_game_id"),
    @Index(name = "idx_pcg_genre", columnList = "genre"),
    @Index(name = "idx_pcg_source", columnList = "source"),
    @Index(name = "idx_pcg_norm_name", columnList = "normalized_name")
})
public class ParticipantConqueredGame implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "participant_id", nullable = false, length = 64)
    private String participantId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "normalized_name", nullable = false, length = 255)
    private String normalizedName;

    @Column(name = "genre", length = 100)
    private String genre;

    @Column(name = "date_played", length = 50)
    private String datePlayed;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "library_game_id", length = 64)
    private String libraryGameId;

    @Column(name = "source", length = 40)
    private String source;

    @Column(name = "created_at", length = 50)
    private String createdAt;

    @Column(name = "updated_at", length = 50)
    private String updatedAt;

    public ParticipantConqueredGame() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getParticipantId() {
        return participantId;
    }

    public void setParticipantId(String participantId) {
        this.participantId = participantId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNormalizedName() {
        return normalizedName;
    }

    public void setNormalizedName(String normalizedName) {
        this.normalizedName = normalizedName;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getDatePlayed() {
        return datePlayed;
    }

    public void setDatePlayed(String datePlayed) {
        this.datePlayed = datePlayed;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getLibraryGameId() {
        return libraryGameId;
    }

    public void setLibraryGameId(String libraryGameId) {
        this.libraryGameId = libraryGameId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParticipantConqueredGame that = (ParticipantConqueredGame) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
