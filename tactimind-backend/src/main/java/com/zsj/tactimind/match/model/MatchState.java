package com.zsj.tactimind.match.model;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class MatchState {
    private String matchId = "demo-match-001";
    private int currentMinute;
    private boolean running;
    private boolean finished;
    private int eventCursor;
    private Map<String, TeamStats> teams = new LinkedHashMap<>();
    private Instant updatedAt = Instant.now();

    public String getMatchId() {
        return matchId;
    }

    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }

    public int getCurrentMinute() {
        return currentMinute;
    }

    public void setCurrentMinute(int currentMinute) {
        this.currentMinute = currentMinute;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public int getEventCursor() {
        return eventCursor;
    }

    public void setEventCursor(int eventCursor) {
        this.eventCursor = eventCursor;
    }

    public Map<String, TeamStats> getTeams() {
        return teams;
    }

    public void setTeams(Map<String, TeamStats> teams) {
        this.teams = teams == null ? new LinkedHashMap<>() : teams;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
