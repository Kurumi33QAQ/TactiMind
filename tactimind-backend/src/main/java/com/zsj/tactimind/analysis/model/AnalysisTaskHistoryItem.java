package com.zsj.tactimind.analysis.model;

import java.time.Instant;

public record AnalysisTaskHistoryItem(
        String taskId,
        String matchId,
        String analysisType,
        AnalysisTaskStatus status,
        String currentStep,
        int progress,
        String reportId,
        Instant createdAt
) {
}
