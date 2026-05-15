package com.zsj.tactimind.match.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zsj.tactimind.match.model.MatchEvent;
import com.zsj.tactimind.match.model.MatchState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Service
@ConditionalOnProperty(name = "tactimind.redis.enabled", havingValue = "true", matchIfMissing = true)
public class MatchRealtimeCacheService extends MatchRealtimeCacheFacade {
    private static final Logger log = LoggerFactory.getLogger(MatchRealtimeCacheService.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration ttl;
    private boolean redisWarningPrinted;

    public MatchRealtimeCacheService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${tactimind.redis.ttl-seconds}") long ttlSeconds
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    @Override
    public void cacheSnapshot(MatchState state, List<MatchEvent> recentEvents) {
        cacheState(state);
        cacheRecentEvents(state.getMatchId(), recentEvents);
    }

    public void cacheState(MatchState state) {
        writeJson(stateKey(state.getMatchId()), state);
    }

    public void cacheRecentEvents(String matchId, List<MatchEvent> recentEvents) {
        writeJson(recentEventsKey(matchId), recentEvents);
    }

    @Override
    public Optional<MatchState> getCachedState(String matchId) {
        return readJson(stateKey(matchId), MatchState.class);
    }

    @Override
    public List<MatchEvent> getCachedRecentEvents(String matchId) {
        return readJson(recentEventsKey(matchId), new TypeReference<List<MatchEvent>>() {
        }).orElse(List.of());
    }

    @Override
    public boolean isAvailable() {
        try {
            RedisCallback<Boolean> pingCallback = connection -> "PONG".equalsIgnoreCase(connection.ping());
            return Boolean.TRUE.equals(redisTemplate.execute(pingCallback));
        } catch (RuntimeException e) {
            return false;
        }
    }

    private void writeJson(String key, Object value) {
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, json, ttl);
        } catch (RedisConnectionFailureException e) {
            printRedisWarning(e);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize Redis value, key={}, reason={}", key, e.getMessage());
        }
    }

    private <T> Optional<T> readJson(String key, Class<T> valueType) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null || json.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, valueType));
        } catch (RedisConnectionFailureException e) {
            printRedisWarning(e);
            return Optional.empty();
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse Redis value, key={}, reason={}", key, e.getMessage());
            return Optional.empty();
        }
    }

    private <T> Optional<T> readJson(String key, TypeReference<T> valueType) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null || json.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, valueType));
        } catch (RedisConnectionFailureException e) {
            printRedisWarning(e);
            return Optional.empty();
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse Redis value, key={}, reason={}", key, e.getMessage());
            return Optional.empty();
        }
    }

    private void printRedisWarning(RuntimeException e) {
        if (!redisWarningPrinted) {
            redisWarningPrinted = true;
            log.warn("Redis is unavailable. Realtime cache is skipped. reason={}", e.getMessage());
        }
    }

    private String stateKey(String matchId) {
        return "tactimind:match:" + matchId + ":state";
    }

    private String recentEventsKey(String matchId) {
        return "tactimind:match:" + matchId + ":recent-events";
    }
}
