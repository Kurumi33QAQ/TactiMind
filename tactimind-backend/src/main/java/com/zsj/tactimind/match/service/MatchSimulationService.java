package com.zsj.tactimind.match.service;

import com.zsj.tactimind.agent.AgentClient;
import com.zsj.tactimind.agent.model.TacticalAnalysis;
import com.zsj.tactimind.match.cache.MatchRealtimeCacheFacade;
import com.zsj.tactimind.match.model.EventType;
import com.zsj.tactimind.match.model.MatchEvent;
import com.zsj.tactimind.match.model.MatchState;
import com.zsj.tactimind.match.model.SimulationStatus;
import com.zsj.tactimind.persistence.MatchPersistenceFacade;
import com.zsj.tactimind.persistence.MatchReport;
import com.zsj.tactimind.websocket.MatchWebSocketBroadcaster;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
public class MatchSimulationService {
    private static final Logger log = LoggerFactory.getLogger(MatchSimulationService.class);
    private static final Set<Double> SUPPORTED_SPEEDS = Set.of(0.5, 1.0, 2.0, 4.0, 8.0);

    private final MatchEventLoader eventLoader;
    private final MatchStateAggregator aggregator;
    private final AgentClient agentClient;
    private final MatchRealtimeCacheFacade cacheService;
    private final MatchPersistenceFacade persistenceService;
    private final MatchWebSocketBroadcaster broadcaster;
    private final long tickMillis;
    private final int recentWindowSize;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private List<MatchEvent> events = new ArrayList<>();
    private List<MatchEvent> recentEvents = new ArrayList<>();
    private List<MatchEvent> publishedEvents = new ArrayList<>();
    private List<TacticalAnalysis> analysisHistory = new ArrayList<>();
    private MatchState state;
    private ScheduledFuture<?> scheduledTask;
    private int cursor;
    private boolean running;
    private int finalMinute;
    private double speed = 1.0;

    public MatchSimulationService(
            MatchEventLoader eventLoader,
            MatchStateAggregator aggregator,
            AgentClient agentClient,
            MatchRealtimeCacheFacade cacheService,
            MatchPersistenceFacade persistenceService,
            MatchWebSocketBroadcaster broadcaster,
            @Value("${tactimind.match.tick-millis}") long tickMillis,
            @Value("${tactimind.match.recent-window-size}") int recentWindowSize
    ) {
        this.eventLoader = eventLoader;
        this.aggregator = aggregator;
        this.agentClient = agentClient;
        this.cacheService = cacheService;
        this.persistenceService = persistenceService;
        this.broadcaster = broadcaster;
        this.tickMillis = tickMillis;
        this.recentWindowSize = recentWindowSize;
    }

    @PostConstruct
    public synchronized void init() {
        events = eventLoader.loadEvents();
        finalMinute = events.stream()
                .mapToInt(MatchEvent::getMinute)
                .max()
                .orElse(90);
        state = aggregator.newState();
        cacheSnapshot();
        persistMatchInfo();
    }

    public synchronized SimulationStatus start() {
        if (running) {
            return status();
        }
        if (cursor >= events.size() || state.isFinished() || state.getCurrentMinute() >= finalMinute) {
            resetInternal();
            clearPersistedTimeline();
        }
        running = true;
        state.setRunning(true);
        cacheSnapshot();
        persistMatchInfo();
        broadcaster.broadcast("SIMULATION_STATUS", status());
        scheduleClock();
        return status();
    }

    public synchronized SimulationStatus pause() {
        running = false;
        state.setRunning(false);
        cancelTask();
        cacheSnapshot();
        persistMatchInfo();
        broadcaster.broadcast("SIMULATION_STATUS", status());
        return status();
    }

    public synchronized SimulationStatus reset() {
        running = false;
        cancelTask();
        resetInternal();
        clearPersistedTimeline();
        cacheSnapshot();
        persistMatchInfo();
        broadcaster.broadcast("MATCH_STATE", state);
        broadcaster.broadcast("SIMULATION_STATUS", status());
        return status();
    }

    public synchronized List<MatchEvent> events() {
        return events;
    }

    public synchronized MatchState state() {
        return state;
    }

    public synchronized MatchState cachedState() {
        return cacheService.getCachedState(state.getMatchId()).orElse(state);
    }

    public synchronized List<MatchEvent> cachedRecentEvents() {
        return cacheService.getCachedRecentEvents(state.getMatchId());
    }

    public synchronized boolean cacheAvailable() {
        return cacheService.isAvailable();
    }

    public synchronized List<TacticalAnalysis> analyzeNow() {
        List<TacticalAnalysis> analyses = analyzeCurrentState();
        rememberAnalyses(analyses);
        return analyses;
    }

    public synchronized List<TacticalAnalysis> analyses() {
        List<TacticalAnalysis> persistedAnalyses = persistenceService.listAnalyses(state.getMatchId());
        if (!persistedAnalyses.isEmpty()) {
            return persistedAnalyses;
        }
        return analysisHistory;
    }

    public synchronized MatchReport report() {
        List<MatchEvent> reportEvents = persistenceService.listEvents(state.getMatchId());
        if (reportEvents.isEmpty()) {
            reportEvents = publishedEvents;
        }

        List<TacticalAnalysis> reportAnalyses = analyses();
        String summary = "本场比赛共记录 " + reportEvents.size()
                + " 条事件，生成 " + reportAnalyses.size()
                + " 条 Agent 战术分析。";
        return new MatchReport(state.getMatchId(), state, reportEvents, reportAnalyses, summary);
    }

    public synchronized SimulationStatus status() {
        return new SimulationStatus(running, state.isFinished(), cursor, events.size(), state.getCurrentMinute(), speed);
    }

    public synchronized SimulationStatus updateSpeed(double speed) {
        if (!SUPPORTED_SPEEDS.contains(speed)) {
            throw new IllegalArgumentException("暂不支持该演练倍速");
        }
        this.speed = speed;
        if (running) {
            cancelTask();
            scheduleClock();
        }
        broadcaster.broadcast("SIMULATION_STATUS", status());
        return status();
    }

    private void advanceOneMinuteSafely() {
        try {
            advanceOneMinute();
        } catch (RuntimeException e) {
            pause();
            broadcaster.broadcast("SIMULATION_ERROR", e.getMessage());
        }
    }

    private synchronized void advanceOneMinute() {
        if (!running) {
            return;
        }
        if (state.getCurrentMinute() >= finalMinute) {
            finishIfNeeded();
            return;
        }

        int nextMinute = state.getCurrentMinute() + 1;
        aggregator.advanceClock(state, nextMinute);

        while (cursor < events.size() && events.get(cursor).getMinute() == nextMinute) {
            publishEventAtCurrentMinute(events.get(cursor));
        }

        cacheSnapshot();
        persistMatchInfo();
        broadcaster.broadcast("MATCH_STATE", state);
        broadcaster.broadcast("SIMULATION_STATUS", status());

        if (state.isFinished() || state.getCurrentMinute() >= finalMinute) {
            finishIfNeeded();
        }
    }

    private void publishEventAtCurrentMinute(MatchEvent event) {
        cursor++;
        aggregator.apply(state, event, cursor);
        rememberRecentEvent(event);
        publishedEvents.add(event);
        persistenceService.saveEvent(state.getMatchId(), cursor, event);
        broadcaster.broadcast("MATCH_EVENT", event);
        analyzeIfNeeded(event);
    }

    private void rememberRecentEvent(MatchEvent event) {
        recentEvents.add(event);
        if (recentEvents.size() > recentWindowSize) {
            recentEvents = new ArrayList<>(recentEvents.subList(recentEvents.size() - recentWindowSize, recentEvents.size()));
        }
    }

    private void analyzeIfNeeded(MatchEvent event) {
        if (!isTacticalEvent(event)) {
            return;
        }

        List<TacticalAnalysis> analyses = analyzeCurrentState();
        rememberAnalyses(analyses);
        for (TacticalAnalysis analysis : analyses) {
            broadcaster.broadcast("TACTICAL_ANALYSIS", analysis);
        }
    }

    private void rememberAnalyses(List<TacticalAnalysis> analyses) {
        for (TacticalAnalysis analysis : analyses) {
            analysisHistory.add(analysis);
            persistenceService.saveAnalysis(state.getMatchId(), analysis);
        }
    }

    private List<TacticalAnalysis> analyzeCurrentState() {
        List<TacticalAnalysis> analyses = agentClient.analyze(state, List.copyOf(recentEvents));
        if (analyses.isEmpty()) {
            log.warn("Agent returned no analyses, minute={}, recentEvents={}", state.getCurrentMinute(), recentEvents.size());
        }
        return analyses;
    }

    private boolean isTacticalEvent(MatchEvent event) {
        EventType type = event.getType();
        return type == EventType.GOAL
                || type == EventType.SHOT
                || type == EventType.SHOT_ON_TARGET
                || type == EventType.POSSESSION_CHANGE
                || type == EventType.CORNER
                || type == EventType.DANGEROUS_ATTACK;
    }

    private void finishIfNeeded() {
        running = false;
        state.setRunning(false);
        state.setFinished(true);
        cancelTask();
        cacheSnapshot();
        persistMatchInfo();
        broadcaster.broadcast("SIMULATION_STATUS", status());
    }

    private void resetInternal() {
        cursor = 0;
        recentEvents = new ArrayList<>();
        publishedEvents = new ArrayList<>();
        analysisHistory = new ArrayList<>();
        state = aggregator.newState();
        state.setRunning(false);
        state.setFinished(false);
    }

    private void cacheSnapshot() {
        cacheService.cacheSnapshot(state, List.copyOf(recentEvents));
    }

    private void persistMatchInfo() {
        persistenceService.saveMatchInfo(state);
    }

    private void clearPersistedTimeline() {
        persistenceService.clearMatchData(state.getMatchId());
    }

    private void cancelTask() {
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
            scheduledTask = null;
        }
    }

    private void scheduleClock() {
        scheduledTask = executor.scheduleAtFixedRate(
                this::advanceOneMinuteSafely,
                0,
                currentTickMillis(),
                TimeUnit.MILLISECONDS
        );
    }

    private long currentTickMillis() {
        return Math.max(100L, Math.round(tickMillis / speed));
    }
}
