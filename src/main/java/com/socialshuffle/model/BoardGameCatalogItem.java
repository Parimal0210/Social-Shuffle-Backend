package com.socialshuffle.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "board_game_catalog", indexes = {
    @Index(name = "idx_bgc_norm_name", columnList = "normalized_name"),
    @Index(name = "idx_bgc_external_id", columnList = "external_id"),
    @Index(name = "idx_bgc_rank", columnList = "bgg_rank"),
    @Index(name = "idx_bgc_snapshot_date", columnList = "snapshot_date")
})
public class BoardGameCatalogItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "external_id", length = 64)
    private String externalId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "normalized_name", nullable = false, length = 255)
    private String normalizedName;

    @Column(name = "genre", length = 100)
    private String genre;

    @Column(name = "players", length = 64)
    private String players;

    @Column(name = "duration", length = 64)
    private String duration;

    @Column(name = "bgg_rank")
    private int bggRank = 99999;

    @Column(name = "has_verified_specs")
    private boolean hasVerifiedSpecs = false;

    @Column(name = "snapshot_date", length = 32)
    private String snapshotDate;

    @Column(name = "updated_at", length = 50)
    private String updatedAt;

    public BoardGameCatalogItem() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
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

    public String getPlayers() {
        return players;
    }

    public void setPlayers(String players) {
        this.players = players;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public int getBggRank() {
        return bggRank;
    }

    public void setBggRank(int bggRank) {
        this.bggRank = bggRank;
    }

    public boolean isHasVerifiedSpecs() {
        return hasVerifiedSpecs;
    }

    public void setHasVerifiedSpecs(boolean hasVerifiedSpecs) {
        this.hasVerifiedSpecs = hasVerifiedSpecs;
    }

    public String getSnapshotDate() {
        return snapshotDate;
    }

    public void setSnapshotDate(String snapshotDate) {
        this.snapshotDate = snapshotDate;
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
        BoardGameCatalogItem that = (BoardGameCatalogItem) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
