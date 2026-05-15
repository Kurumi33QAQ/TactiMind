package com.zsj.tactimind.match.web;

import com.zsj.tactimind.agent.model.TacticalAnalysis;
import com.zsj.tactimind.match.model.MatchEvent;
import com.zsj.tactimind.match.model.MatchState;
import com.zsj.tactimind.match.model.SimulationStatus;
import com.zsj.tactimind.match.service.MatchSimulationService;
import com.zsj.tactimind.persistence.MatchPersistenceFacade;
import com.zsj.tactimind.persistence.MatchReport;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/match")
public class MatchSimulationController {
    private final MatchSimulationService simulationService;
    private final MatchPersistenceFacade persistenceService;

    public MatchSimulationController(
            MatchSimulationService simulationService,
            MatchPersistenceFacade persistenceService
    ) {
        this.simulationService = simulationService;
        this.persistenceService = persistenceService;
    }

    @PostMapping("/simulate/start")
    public SimulationStatus start() {
        return simulationService.start();
    }

    @PostMapping("/simulate/pause")
    public SimulationStatus pause() {
        return simulationService.pause();
    }

    @PostMapping("/simulate/reset")
    public SimulationStatus reset() {
        return simulationService.reset();
    }

    @GetMapping("/simulate/status")
    public SimulationStatus status() {
        return simulationService.status();
    }

    @PostMapping("/simulate/speed")
    public SimulationStatus updateSpeed(@RequestParam double speed) {
        return simulationService.updateSpeed(speed);
    }

    @GetMapping("/events")
    public List<MatchEvent> events() {
        return simulationService.events();
    }

    @GetMapping("/state")
    public MatchState state() {
        return simulationService.state();
    }

    @GetMapping("/cache/state")
    public MatchState cachedState() {
        return simulationService.cachedState();
    }

    @GetMapping("/cache/recent-events")
    public List<MatchEvent> cachedRecentEvents() {
        return simulationService.cachedRecentEvents();
    }

    @GetMapping("/cache/status")
    public Map<String, Object> cacheStatus() {
        return Map.of(
                "available", simulationService.cacheAvailable(),
                "stateKey", "tactimind:match:demo-match-001:state",
                "recentEventsKey", "tactimind:match:demo-match-001:recent-events"
        );
    }

    @PostMapping("/agent/analyze-now")
    public List<TacticalAnalysis> analyzeNow() {
        return simulationService.analyzeNow();
    }

    @GetMapping("/analysis")
    public List<TacticalAnalysis> analyses() {
        return simulationService.analyses();
    }

    @GetMapping("/report")
    public MatchReport report() {
        return simulationService.report();
    }

    @GetMapping("/persistence/status")
    public Map<String, Object> persistenceStatus() {
        return Map.of(
                "enabled", persistenceService.isEnabled(),
                "mode", persistenceService.isEnabled() ? "mysql" : "memory"
        );
    }
}
