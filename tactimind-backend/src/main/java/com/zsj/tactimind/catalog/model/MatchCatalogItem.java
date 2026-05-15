package com.zsj.tactimind.catalog.model;

import java.time.LocalDate;
import java.util.List;

/**
 * 可演练比赛目录项。
 * 当前阶段使用内置数据，后续可以平滑迁移到 match_catalog 表。
 */
public record MatchCatalogItem(
        Long id,
        String matchCode,
        String homeTeam,
        String awayTeam,
        String homeTeamLogo,
        String awayTeamLogo,
        String competition,
        String season,
        LocalDate matchDate,
        SourceType sourceType,
        DataLevel dataLevel,
        String eventFilePath,
        boolean playable,
        String description,
        List<String> capabilities
) {
    public boolean simulated() {
        return sourceType == SourceType.AI_SIMULATED || dataLevel == DataLevel.SIMULATED_EVENT;
    }
}
