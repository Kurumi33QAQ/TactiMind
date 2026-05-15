package com.zsj.tactimind.match.service;

import com.zsj.tactimind.match.model.EventType;
import com.zsj.tactimind.match.model.MatchEvent;
import com.zsj.tactimind.match.model.MatchState;
import com.zsj.tactimind.match.model.TeamStats;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
public class MatchStateAggregator {
    public MatchState newState() {
        MatchState state = new MatchState();
        ensureTeam(state, "Team A");
        ensureTeam(state, "Team B");
        return state;
    }

    public void advanceClock(MatchState state, int minute) {
        state.setCurrentMinute(minute);
        state.setUpdatedAt(Instant.now());
    }

    public void apply(MatchState state, MatchEvent event, int cursor) {
        state.setEventCursor(cursor);
        state.setUpdatedAt(Instant.now());
        ensureTeam(state, event.getTeam());

        TeamStats stats = state.getTeams().get(event.getTeam());
        EventType type = event.getType();
        if (type == null) {
            return;
        }

        switch (type) {
            case MATCH_START -> state.setFinished(false);
            case GOAL -> stats.setGoals(stats.getGoals() + 1);
            case SHOT -> {
                stats.setShots(stats.getShots() + 1);
                if (asBoolean(event.getData().get("shot_on_target"))) {
                    stats.setShotsOnTarget(stats.getShotsOnTarget() + 1);
                }
            }
            case SHOT_ON_TARGET -> {
                stats.setShots(stats.getShots() + 1);
                stats.setShotsOnTarget(stats.getShotsOnTarget() + 1);
            }
            case YELLOW_CARD -> stats.setYellowCards(stats.getYellowCards() + 1);
            case CORNER -> stats.setCorners(stats.getCorners() + 1);
            case DANGEROUS_ATTACK -> stats.setDangerousAttacks(stats.getDangerousAttacks() + 1);
            case POSSESSION_CHANGE -> updatePossession(state, event.getData());
            case MATCH_END -> state.setFinished(true);
            case SUBSTITUTION -> {
                // Substitution changes context but does not alter the first-version counters.
            }
        }
    }

    private void ensureTeam(MatchState state, String team) {
        if (team != null && !team.isBlank()) {
            state.getTeams().computeIfAbsent(team, ignored -> new TeamStats());
        }
    }

    private void updatePossession(MatchState state, Map<String, Object> data) {
        setPossession(state, "Team A", data.get("team_a"));
        setPossession(state, "Team B", data.get("team_b"));
    }

    private void setPossession(MatchState state, String team, Object value) {
        if (value instanceof Number number) {
            ensureTeam(state, team);
            state.getTeams().get(team).setPossessionRate(number.intValue());
        }
    }

    private boolean asBoolean(Object value) {
        return value instanceof Boolean bool && bool;
    }
}
