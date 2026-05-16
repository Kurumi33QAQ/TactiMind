package com.zsj.tactimind.catalog.web;

import com.zsj.tactimind.catalog.model.DataLevel;
import com.zsj.tactimind.catalog.model.MatchCatalogItem;
import com.zsj.tactimind.catalog.model.MatchEventSummary;
import com.zsj.tactimind.catalog.model.MatchTacticalProfile;
import com.zsj.tactimind.catalog.service.MatchCatalogService;
import com.zsj.tactimind.catalog.service.MatchTacticalProfileService;
import com.zsj.tactimind.match.model.MatchEvent;
import com.zsj.tactimind.match.service.MatchEventLoader;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@CrossOrigin
@RestController
@RequestMapping("/api/matches")
public class MatchCatalogController {
    private final MatchCatalogService matchCatalogService;
    private final MatchTacticalProfileService profileService;
    private final MatchEventLoader matchEventLoader;

    public MatchCatalogController(
            MatchCatalogService matchCatalogService,
            MatchTacticalProfileService profileService,
            MatchEventLoader matchEventLoader
    ) {
        this.matchCatalogService = matchCatalogService;
        this.profileService = profileService;
        this.matchEventLoader = matchEventLoader;
    }

    @GetMapping("/search")
    public List<MatchCatalogItem> search(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String team,
            @RequestParam(required = false) String competition,
            @RequestParam(required = false) DataLevel dataLevel
    ) {
        return matchCatalogService.search(date, team, competition, dataLevel);
    }

    @GetMapping("/{matchId}")
    public MatchCatalogItem detail(@PathVariable String matchId) {
        return matchCatalogService.findByIdOrCode(matchId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "未找到对应比赛"));
    }


    @GetMapping("/{matchId}/events/summary")
    public MatchEventSummary eventSummary(@PathVariable String matchId) {
        MatchCatalogItem match = matchCatalogService.findByIdOrCode(matchId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "未找到对应比赛"));
        if (match.eventFilePath() == null || match.eventFilePath().isBlank()) {
            return new MatchEventSummary(match.matchCode(), "UNSUPPORTED", "", 0, 0, 0, Map.of());
        }

        List<MatchEvent> events = matchEventLoader.loadEvents(match);
        int firstMinute = events.stream().mapToInt(MatchEvent::getMinute).min().orElse(0);
        int lastMinute = events.stream().mapToInt(MatchEvent::getMinute).max().orElse(0);
        Map<String, Long> eventTypeCounts = events.stream()
                .collect(Collectors.groupingBy(event -> event.getType().name(), Collectors.counting()));
        return new MatchEventSummary(
                match.matchCode(),
                matchEventLoader.dataSourceName(match),
                match.eventFilePath(),
                events.size(),
                firstMinute,
                lastMinute,
                eventTypeCounts
        );
    }
    @GetMapping("/{matchId}/profile")
    public MatchTacticalProfile profile(@PathVariable String matchId) {
        MatchCatalogItem match = matchCatalogService.findByIdOrCode(matchId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "未找到对应比赛"));
        return profileService.findByMatchCode(match.matchCode())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "该比赛暂无战术资料"));
    }
}
