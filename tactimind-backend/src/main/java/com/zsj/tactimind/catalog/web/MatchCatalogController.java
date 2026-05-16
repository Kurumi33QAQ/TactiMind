package com.zsj.tactimind.catalog.web;

import com.zsj.tactimind.catalog.model.DataLevel;
import com.zsj.tactimind.catalog.model.MatchCatalogItem;
import com.zsj.tactimind.catalog.model.MatchTacticalProfile;
import com.zsj.tactimind.catalog.service.MatchCatalogService;
import com.zsj.tactimind.catalog.service.MatchTacticalProfileService;
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

import static org.springframework.http.HttpStatus.NOT_FOUND;

@CrossOrigin
@RestController
@RequestMapping("/api/matches")
public class MatchCatalogController {
    private final MatchCatalogService matchCatalogService;
    private final MatchTacticalProfileService profileService;

    public MatchCatalogController(
            MatchCatalogService matchCatalogService,
            MatchTacticalProfileService profileService
    ) {
        this.matchCatalogService = matchCatalogService;
        this.profileService = profileService;
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

    @GetMapping("/{matchId}/profile")
    public MatchTacticalProfile profile(@PathVariable String matchId) {
        MatchCatalogItem match = matchCatalogService.findByIdOrCode(matchId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "未找到对应比赛"));
        return profileService.findByMatchCode(match.matchCode())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "该比赛暂无战术资料"));
    }
}
