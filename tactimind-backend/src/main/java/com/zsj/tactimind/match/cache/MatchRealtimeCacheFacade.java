package com.zsj.tactimind.match.cache;

import com.zsj.tactimind.match.model.MatchEvent;
import com.zsj.tactimind.match.model.MatchState;

import java.util.List;
import java.util.Optional;

public abstract class MatchRealtimeCacheFacade {
    public abstract void cacheSnapshot(MatchState state, List<MatchEvent> recentEvents);

    public abstract Optional<MatchState> getCachedState(String matchId);

    public abstract List<MatchEvent> getCachedRecentEvents(String matchId);

    public abstract boolean isAvailable();
}
