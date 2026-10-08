package com.socialshuffle.dto;

import java.util.ArrayList;
import java.util.List;

public class BadgeCalculationResult {
    private int currentLevel;
    private BadgeLevelDefinition currentBadge;
    private BadgeLevelDefinition nextBadge;
    private int gamesPlayedCount;
    private int totalGamesCount;
    private int gamePercentage;
    private int eventsAttendedCount;
    private int overallScore;
    private LevelProgress levelProgress;
    private List<LevelStatus> levels = new ArrayList<>();

    public static class LevelProgress {
        private int percentageToNextLevel;
        private int gamesNeededForNextLevel;
        private int eventsNeededForNextLevel;

        public LevelProgress() {
        }

        public LevelProgress(int percentageToNextLevel, int gamesNeededForNextLevel, int eventsNeededForNextLevel) {
            this.percentageToNextLevel = percentageToNextLevel;
            this.gamesNeededForNextLevel = gamesNeededForNextLevel;
            this.eventsNeededForNextLevel = eventsNeededForNextLevel;
        }

        public int getPercentageToNextLevel() {
            return percentageToNextLevel;
        }

        public void setPercentageToNextLevel(int percentageToNextLevel) {
            this.percentageToNextLevel = percentageToNextLevel;
        }

        public int getGamesNeededForNextLevel() {
            return gamesNeededForNextLevel;
        }

        public void setGamesNeededForNextLevel(int gamesNeededForNextLevel) {
            this.gamesNeededForNextLevel = gamesNeededForNextLevel;
        }

        public int getEventsNeededForNextLevel() {
            return eventsNeededForNextLevel;
        }

        public void setEventsNeededForNextLevel(int eventsNeededForNextLevel) {
            this.eventsNeededForNextLevel = eventsNeededForNextLevel;
        }
    }

    public static class LevelStatus {
        private int level;
        private BadgeLevelDefinition definition;
        private boolean isUnlocked;
        private boolean isCurrent;
        private int progressPercentage;

        public LevelStatus() {
        }

        public LevelStatus(int level, BadgeLevelDefinition definition, boolean isUnlocked, boolean isCurrent, int progressPercentage) {
            this.level = level;
            this.definition = definition;
            this.isUnlocked = isUnlocked;
            this.isCurrent = isCurrent;
            this.progressPercentage = progressPercentage;
        }

        public int getLevel() {
            return level;
        }

        public void setLevel(int level) {
            this.level = level;
        }

        public BadgeLevelDefinition getDefinition() {
            return definition;
        }

        public void setDefinition(BadgeLevelDefinition definition) {
            this.definition = definition;
        }

        public boolean isUnlocked() {
            return isUnlocked;
        }

        public void setUnlocked(boolean unlocked) {
            isUnlocked = unlocked;
        }

        public boolean isCurrent() {
            return isCurrent;
        }

        public void setCurrent(boolean current) {
            isCurrent = current;
        }

        public int getProgressPercentage() {
            return progressPercentage;
        }

        public void setProgressPercentage(int progressPercentage) {
            this.progressPercentage = progressPercentage;
        }
    }

    public BadgeCalculationResult() {
    }

    public int getCurrentLevel() {
        return currentLevel;
    }

    public void setCurrentLevel(int currentLevel) {
        this.currentLevel = currentLevel;
    }

    public BadgeLevelDefinition getCurrentBadge() {
        return currentBadge;
    }

    public void setCurrentBadge(BadgeLevelDefinition currentBadge) {
        this.currentBadge = currentBadge;
    }

    public BadgeLevelDefinition getNextBadge() {
        return nextBadge;
    }

    public void setNextBadge(BadgeLevelDefinition nextBadge) {
        this.nextBadge = nextBadge;
    }

    public int getGamesPlayedCount() {
        return gamesPlayedCount;
    }

    public void setGamesPlayedCount(int gamesPlayedCount) {
        this.gamesPlayedCount = gamesPlayedCount;
    }

    public int getTotalGamesCount() {
        return totalGamesCount;
    }

    public void setTotalGamesCount(int totalGamesCount) {
        this.totalGamesCount = totalGamesCount;
    }

    public int getGamePercentage() {
        return gamePercentage;
    }

    public void setGamePercentage(int gamePercentage) {
        this.gamePercentage = gamePercentage;
    }

    public int getEventsAttendedCount() {
        return eventsAttendedCount;
    }

    public void setEventsAttendedCount(int eventsAttendedCount) {
        this.eventsAttendedCount = eventsAttendedCount;
    }

    public int getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(int overallScore) {
        this.overallScore = overallScore;
    }

    public LevelProgress getLevelProgress() {
        return levelProgress;
    }

    public void setLevelProgress(LevelProgress levelProgress) {
        this.levelProgress = levelProgress;
    }

    public List<LevelStatus> getLevels() {
        return levels;
    }

    public void setLevels(List<LevelStatus> levels) {
        this.levels = levels;
    }
}
