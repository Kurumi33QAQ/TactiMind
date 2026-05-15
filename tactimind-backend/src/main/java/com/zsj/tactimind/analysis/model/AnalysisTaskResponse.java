package com.zsj.tactimind.analysis.model;

public record AnalysisTaskResponse(
        String taskId,
        AnalysisTaskStatus status,
        String currentStep,
        int progress,
        String reportId
) {
}
