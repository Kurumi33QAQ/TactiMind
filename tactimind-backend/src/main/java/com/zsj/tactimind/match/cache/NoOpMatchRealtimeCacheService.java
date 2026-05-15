package com.zsj.tactimind.match.cache;

import com.zsj.tactimind.match.model.MatchEvent;
import com.zsj.tactimind.match.model.MatchState;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@ConditionalOnProperty(name = "tactimind.redis.enabled", havingValue = "false")
public class NoOpMatchRealtimeCacheService extends MatchRealtimeCacheFacade {
    @Override
    public void cacheSnapshot(MatchState state, List<MatchEvent> recentEvents) {
    }

    @Override
    public Optional<MatchState> getCachedState(String matchId) {
        return Optional.empty();
    }

    @Override
    public List<MatchEvent> getCachedRecentEvents(String matchId) {
        return List.of();
    }

    @Override
    public boolean isAvailable() {
        return false;
    }
}
