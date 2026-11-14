package com.socialshuffle.model;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "community_stats")
public class CommunityStats implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 64)
    private String id = "community-stats-main";

    @Column(name = "total_members")
    private int totalMembers = 0;

    @Column(name = "total_events")
    private int totalEvents = 0;

    @Column(name = "total_games")
    private int totalGames = 0;

    @Column(name = "total_checkins")
    private int totalCheckins = 0;

    @Column(name = "active_areas", columnDefinition = "TEXT")
    private String activeAreas = "[]";

    @Column(name = "payload_json", columnDefinition = "TEXT")
    private String payloadJson;

    @Column(name = "updated_by", length = 150)
    private String updatedBy;

    @Column(name = "updated_at", length = 50)
    private String updatedAt;

    public CommunityStats() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public int getTotalMembers() { return totalMembers; }
    public void setTotalMembers(int totalMembers) { this.totalMembers = totalMembers; }

    public int getTotalEvents() { return totalEvents; }
    public void setTotalEvents(int totalEvents) { this.totalEvents = totalEvents; }

    public int getTotalGames() { return totalGames; }
    public void setTotalGames(int totalGames) { this.totalGames = totalGames; }

    public int getTotalCheckins() { return totalCheckins; }
    public void setTotalCheckins(int totalCheckins) { this.totalCheckins = totalCheckins; }

    public String getActiveAreas() { return activeAreas; }
    public void setActiveAreas(String activeAreas) { this.activeAreas = activeAreas; }

    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
