package com.zsj.tactimind.catalog.model;

import java.util.Map;

public record MatchEventSummary(
        String matchCode,
        String dataSource,
        String eventFilePath,
        int totalEvents,
        int firstMinute,
        int lastMinute,
        Map<String, Long> eventTypeCounts
) {
}
