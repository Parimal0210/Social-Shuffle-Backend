package com.socialshuffle.model;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "participant_badges", indexes = {
    @Index(name = "idx_part_badge_participant", columnList = "participant_id"),
    @Index(name = "idx_part_badge_level", columnList = "badge_level")
})
public class ParticipantBadge implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "participant_id", nullable = false, length = 64)
    private String participantId;

    @Column(name = "badge_level", nullable = false)
    private int badgeLevel;

    @Column(name = "badge_name", length = 100)
    private String badgeName;

    @Column(name = "badge_icon", length = 20)
    private String badgeIcon;

    @Column(name = "earned_at", length = 50)
    private String earnedAt;

    @Column(name = "is_current")
    private boolean current;

    public ParticipantBadge() {
    }

    public ParticipantBadge(String id, String participantId, int badgeLevel, String badgeName, String badgeIcon, String earnedAt, boolean current) {
        this.id = id;
        this.participantId = participantId;
        this.badgeLevel = badgeLevel;
        this.badgeName = badgeName;
        this.badgeIcon = badgeIcon;
        this.earnedAt = earnedAt;
        this.current = current;
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

    public int getBadgeLevel() {
        return badgeLevel;
    }

    public void setBadgeLevel(int badgeLevel) {
        this.badgeLevel = badgeLevel;
    }

    public String getBadgeName() {
        return badgeName;
    }

    public void setBadgeName(String badgeName) {
        this.badgeName = badgeName;
    }

    public String getBadgeIcon() {
        return badgeIcon;
    }

    public void setBadgeIcon(String badgeIcon) {
        this.badgeIcon = badgeIcon;
    }

    public String getEarnedAt() {
        return earnedAt;
    }

    public void setEarnedAt(String earnedAt) {
        this.earnedAt = earnedAt;
    }

    public boolean isCurrent() {
        return current;
    }

    public void setCurrent(boolean current) {
        this.current = current;
    }
}
