package com.socialshuffle.dto;

public class BadgeCalculationRequest {
    private int gamesPlayedCount;
    private int totalGamesCount = 24;
    private int eventsAttendedCount;
    private String participantId;

    public BadgeCalculationRequest() {
    }

    public BadgeCalculationRequest(int gamesPlayedCount, int totalGamesCount, int eventsAttendedCount) {
        this.gamesPlayedCount = gamesPlayedCount;
        this.totalGamesCount = totalGamesCount > 0 ? totalGamesCount : 24;
        this.eventsAttendedCount = eventsAttendedCount;
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

    public int getEventsAttendedCount() {
        return eventsAttendedCount;
    }

    public void setEventsAttendedCount(int eventsAttendedCount) {
        this.eventsAttendedCount = eventsAttendedCount;
    }

    public String getParticipantId() {
        return participantId;
    }

    public void setParticipantId(String participantId) {
        this.participantId = participantId;
    }
}
