package com.zsj.tactimind.analysis.model;

public record CreateAnalysisTaskRequest(
        String matchId,
        String analysisType
) {
}
