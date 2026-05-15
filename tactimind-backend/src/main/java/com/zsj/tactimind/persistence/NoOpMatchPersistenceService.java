package com.zsj.tactimind.persistence;

import com.zsj.tactimind.agent.model.TacticalAnalysis;
import com.zsj.tactimind.match.model.MatchEvent;
import com.zsj.tactimind.match.model.MatchState;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ConditionalOnProperty(name = "tactimind.mysql.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpMatchPersistenceService extends MatchPersistenceFacade {
    @Override
    public void saveMatchInfo(MatchState state) {
    }

    @Override
    public void clearMatchData(String matchId) {
    }

    @Override
    public void saveEvent(String matchId, int eventIndex, MatchEvent event) {
    }

    @Override
    public void saveAnalysis(String matchId, TacticalAnalysis analysis) {
    }

    @Override
    public List<MatchEvent> listEvents(String matchId) {
        return List.of();
    }

    @Override
    public List<TacticalAnalysis> listAnalyses(String matchId) {
        return List.of();
    }

    @Override
    public boolean isEnabled() {
        return false;
    }
}
