package com.socialshuffle.dto;

import java.util.ArrayList;
import java.util.List;

public class BadgeLevelDefinition {
    private int level;
    private String fancyName;
    private String shortTitle;
    private String subtitle;
    private String description;
    private String icon;
    private int minGamePercentage;
    private int minEventsAttended;
    private String rarity;
    private String gradient;
    private String borderColor;
    private String textColor;
    private String badgeTagColor;
    private String glowEffect;
    private List<String> perks = new ArrayList<>();

    public BadgeLevelDefinition() {
    }

    public BadgeLevelDefinition(int level, String shortTitle, String fancyName, String subtitle,
                                String description, String icon, int minGamePercentage,
                                int minEventsAttended, String rarity, String gradient,
                                String borderColor, String textColor, String badgeTagColor,
                                String glowEffect, List<String> perks) {
        this.level = level;
        this.shortTitle = shortTitle;
        this.fancyName = fancyName;
        this.subtitle = subtitle;
        this.description = description;
        this.icon = icon;
        this.minGamePercentage = minGamePercentage;
        this.minEventsAttended = minEventsAttended;
        this.rarity = rarity;
        this.gradient = gradient;
        this.borderColor = borderColor;
        this.textColor = textColor;
        this.badgeTagColor = badgeTagColor;
        this.glowEffect = glowEffect;
        this.perks = perks != null ? perks : new ArrayList<>();
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getFancyName() {
        return fancyName;
    }

    public void setFancyName(String fancyName) {
        this.fancyName = fancyName;
    }

    public String getShortTitle() {
        return shortTitle;
    }

    public void setShortTitle(String shortTitle) {
        this.shortTitle = shortTitle;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public int getMinGamePercentage() {
        return minGamePercentage;
    }

    public void setMinGamePercentage(int minGamePercentage) {
        this.minGamePercentage = minGamePercentage;
    }

    public int getMinEventsAttended() {
        return minEventsAttended;
    }

    public void setMinEventsAttended(int minEventsAttended) {
        this.minEventsAttended = minEventsAttended;
    }

    public String getRarity() {
        return rarity;
    }

    public void setRarity(String rarity) {
        this.rarity = rarity;
    }

    public String getGradient() {
        return gradient;
    }

    public void setGradient(String gradient) {
        this.gradient = gradient;
    }

    public String getBorderColor() {
        return borderColor;
    }

    public void setBorderColor(String borderColor) {
        this.borderColor = borderColor;
    }

    public String getTextColor() {
        return textColor;
    }

    public void setTextColor(String textColor) {
        this.textColor = textColor;
    }

    public String getBadgeTagColor() {
        return badgeTagColor;
    }

    public void setBadgeTagColor(String badgeTagColor) {
        this.badgeTagColor = badgeTagColor;
    }

    public String getGlowEffect() {
        return glowEffect;
    }

    public void setGlowEffect(String glowEffect) {
        this.glowEffect = glowEffect;
    }

    public List<String> getPerks() {
        return perks;
    }

    public void setPerks(List<String> perks) {
        this.perks = perks;
    }
}
