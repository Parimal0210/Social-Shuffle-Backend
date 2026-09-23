package com.socialshuffle.model;

import com.socialshuffle.converter.StringListConverter;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "games", indexes = {
    @Index(name = "idx_game_category", columnList = "category"),
    @Index(name = "idx_game_difficulty", columnList = "difficulty"),
    @Index(name = "idx_game_active", columnList = "active")
})
public class Game {

    @Id
    @Column(length = 64)
    private String id;

    private String name;
    private String category;
    private String difficulty;
    private String players;
    private String duration;

    @Column(columnDefinition = "TEXT")
    private String description;

    private boolean active = true;
    private int playsCount = 0;

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<String> tags = new ArrayList<>();

    private Double rating;

    @Column(columnDefinition = "TEXT")
    private String image;

    public Game() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getPlaysCount() {
        return playsCount;
    }

    public void setPlaysCount(int playsCount) {
        this.playsCount = playsCount;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags != null ? tags : new ArrayList<>();
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}
