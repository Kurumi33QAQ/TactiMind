package com.zsj.tactimind.persistence;

import com.zsj.tactimind.agent.model.TacticalAnalysis;
import com.zsj.tactimind.match.model.MatchEvent;
import com.zsj.tactimind.match.model.MatchState;

import java.util.List;

public abstract class MatchPersistenceFacade {
    public abstract void saveMatchInfo(MatchState state);

    public abstract void clearMatchData(String matchId);

    public abstract void saveEvent(String matchId, int eventIndex, MatchEvent event);

    public abstract void saveAnalysis(String matchId, TacticalAnalysis analysis);

    public abstract List<MatchEvent> listEvents(String matchId);

    public abstract List<TacticalAnalysis> listAnalyses(String matchId);

    public abstract boolean isEnabled();
}
