package com.zsj.tactimind.match.service;

import com.zsj.tactimind.agent.AgentClient;
import com.zsj.tactimind.agent.model.AgentAnalyzeResponse;
import com.zsj.tactimind.agent.model.DataInsight;
import com.zsj.tactimind.agent.model.TacticalAnalysis;
import com.zsj.tactimind.analysis.model.AgentTraceLog;
import com.zsj.tactimind.catalog.model.MatchCatalogItem;
import com.zsj.tactimind.catalog.model.MatchTacticalProfile;
import com.zsj.tactimind.catalog.service.MatchCatalogService;
import com.zsj.tactimind.catalog.service.MatchTacticalProfileService;
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

import java.time.Instant;
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
    private static final String DEFAULT_MATCH_CODE = "world-cup-2022-argentina-france";

    private final MatchEventLoader eventLoader;
    private final MatchStateAggregator aggregator;
    private final AgentClient agentClient;
    private final MatchRealtimeCacheFacade cacheService;
    private final MatchPersistenceFacade persistenceService;
    private final MatchWebSocketBroadcaster broadcaster;
    private final MatchCatalogService matchCatalogService;
    private final MatchTacticalProfileService profileService;
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
    private String currentMatchCode = DEFAULT_MATCH_CODE;
    private String currentEventFilePath = "";

    public MatchSimulationService(
            MatchEventLoader eventLoader,
            MatchStateAggregator aggregator,
            AgentClient agentClient,
            MatchRealtimeCacheFacade cacheService,
            MatchPersistenceFacade persistenceService,
            MatchWebSocketBroadcaster broadcaster,
            MatchCatalogService matchCatalogService,
            MatchTacticalProfileService profileService,
            @Value("${tactimind.match.tick-millis}") long tickMillis,
            @Value("${tactimind.match.recent-window-size}") int recentWindowSize
    ) {
        this.eventLoader = eventLoader;
        this.aggregator = aggregator;
        this.agentClient = agentClient;
        this.cacheService = cacheService;
        this.persistenceService = persistenceService;
        this.broadcaster = broadcaster;
        this.matchCatalogService = matchCatalogService;
        this.profileService = profileService;
        this.tickMillis = tickMillis;
        this.recentWindowSize = recentWindowSize;
    }

    @PostConstruct
    public synchronized void init() {
        loadCurrentMatchEvents();
        state = newStateForCurrentMatch();
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

    public synchronized SimulationStatus selectMatch(String matchId) {
        MatchCatalogItem match = matchCatalogService.findByIdOrCode(matchId)
                .orElseThrow(() -> new IllegalArgumentException("未找到可演练比赛：" + matchId));
        if (match.eventFilePath() == null || match.eventFilePath().isBlank()) {
            throw new IllegalArgumentException("该比赛暂无可演练事件流文件，请选择深度演练或模拟演练比赛");
        }

        running = false;
        cancelTask();
        currentMatchCode = match.matchCode();
        currentEventFilePath = match.eventFilePath();
        loadCurrentMatchEvents();
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
        AgentAnalyzeResponse response = analyzeCurrentState();
        broadcastDataInsights(response.dataInsights());
        List<TacticalAnalysis> analyses = response.analyses();
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
        return new SimulationStatus(
                running,
                state.isFinished(),
                cursor,
                events.size(),
                state.getCurrentMinute(),
                speed,
                finalMinute
        );
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

    public synchronized SimulationStatus seek(int minute) {
        if (minute < 0 || minute > finalMinute) {
            throw new IllegalArgumentException("跳转分钟必须位于 0 到 " + finalMinute + " 之间");
        }

        running = false;
        cancelTask();
        resetInternal();
        clearPersistedTimeline();

        for (int nextMinute = 1; nextMinute <= minute; nextMinute++) {
            aggregator.advanceClock(state, nextMinute);
            while (cursor < events.size() && events.get(cursor).getMinute() == nextMinute) {
                replayEventAtCurrentMinute(events.get(cursor));
            }
        }

        state.setRunning(false);
        cacheSnapshot();
        persistMatchInfo();
        broadcaster.broadcast("MATCH_STATE", state);
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

    private void replayEventAtCurrentMinute(MatchEvent event) {
        cursor++;
        aggregator.apply(state, event, cursor);
        rememberRecentEvent(event);
        publishedEvents.add(event);
        persistenceService.saveEvent(state.getMatchId(), cursor, event);
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

        AgentAnalyzeResponse response = analyzeCurrentState();
        broadcastDataInsights(response.dataInsights());
        List<TacticalAnalysis> analyses = response.analyses();
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

    private AgentAnalyzeResponse analyzeCurrentState() {
        AgentAnalyzeResponse response = agentClient.analyze(state, List.copyOf(recentEvents), currentTacticalProfile());
        if (response.analyses().isEmpty()) {
            log.warn("Agent returned no analyses, minute={}, recentEvents={}", state.getCurrentMinute(), recentEvents.size());
        }
        return response;
    }

    private MatchTacticalProfile currentTacticalProfile() {
        // 当前实时演练引擎还固定使用世界杯决赛 demo 事件流。
        // 后续按比赛编号加载事件文件时，这里会改成 state.matchId -> matchCode 的映射。
        return profileService.findByMatchCode(currentMatchCode).orElse(null);
    }

    private void broadcastDataInsights(List<DataInsight> dataInsights) {
        if (dataInsights == null || dataInsights.isEmpty()) {
            return;
        }
        for (DataInsight insight : dataInsights) {
            broadcaster.broadcast("DATA_INSIGHT", insight);
            broadcaster.broadcast("AGENT_TRACE", toRealtimeTrace(insight));
        }
    }

    private AgentTraceLog toRealtimeTrace(DataInsight insight) {
        return new AgentTraceLog(
                "实时数据洞察",
                "DataAgent",
                toolNameOf(insight.code()),
                "成功",
                "第 " + insight.minute() + " 分钟，最近事件数 " + recentEvents.size(),
                insight.summary() + "；依据：" + String.join("；", insight.evidence()),
                0,
                "",
                Instant.now()
        );
    }

    private String toolNameOf(String insightCode) {
        return switch (insightCode) {
            case "REPEATED_ZONE_PRESSURE" -> "detect_repeated_pressure";
            case "SHOT_PRESSURE" -> "detect_shot_pressure";
            case "POSSESSION_GAP" -> "detect_possession_gap";
            case "PROFILE_CONTEXT_SUPPORT" -> "merge_tactical_profile";
            case "KEY_PASS_CREATION" -> "detect_key_pass_creation";
            case "TURNOVER_RISK" -> "detect_turnover_risk";
            case "TRANSITION_THREAT" -> "detect_transition_threat";
            default -> "detect_realtime_trend";
        };
    }

    private boolean isTacticalEvent(MatchEvent event) {
        EventType type = event.getType();
        return type == EventType.GOAL
                || type == EventType.SHOT
                || type == EventType.SHOT_ON_TARGET
                || type == EventType.POSSESSION_CHANGE
                || type == EventType.CORNER
                || type == EventType.DANGEROUS_ATTACK
                || type == EventType.KEY_PASS
                || type == EventType.TACKLE
                || type == EventType.TURNOVER
                || type == EventType.TRANSITION;
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
        state = newStateForCurrentMatch();
        state.setRunning(false);
        state.setFinished(false);
    }

    private void loadCurrentMatchEvents() {
        MatchCatalogItem match = currentCatalogItem();
        currentEventFilePath = match.eventFilePath();
        events = eventLoader.loadEvents(match);
        finalMinute = events.stream()
                .mapToInt(MatchEvent::getMinute)
                .max()
                .orElse(90);
    }

    private MatchState newStateForCurrentMatch() {
        MatchState newState = aggregator.newState();
        newState.setMatchId(currentMatchCode);
        return newState;
    }

    private MatchCatalogItem currentCatalogItem() {
        return matchCatalogService.findByIdOrCode(currentMatchCode)
                .orElseThrow(() -> new IllegalStateException("当前比赛不在比赛目录中：" + currentMatchCode));
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


