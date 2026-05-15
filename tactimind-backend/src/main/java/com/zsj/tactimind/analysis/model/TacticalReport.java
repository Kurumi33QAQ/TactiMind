package com.zsj.tactimind.analysis.model;

import com.zsj.tactimind.agent.model.TacticalAnalysis;

import java.util.List;

public record TacticalReport(
        String reportId,
        String taskId,
        String matchId,
        String summary,
        List<TacticalAnalysis> conclusions,
        List<String> suggestions,
        List<String> risks,
        List<String> dataSources,
        boolean simulatedData
) {
}
