package com.zsj.tactimind.analysis.model;

import java.time.Instant;

public record AgentTraceLog(
        String stepName,
        String agentName,
        String toolName,
        String status,
        String inputSummary,
        String outputSummary,
        long costTimeMs,
        String errorMessage,
        Instant createdAt
) {
}
