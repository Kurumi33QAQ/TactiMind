package com.zsj.tactimind.agent.model;

import java.util.List;

/**
 * Agent 最终返回给后端和前端展示的战术分析结果。
 */
public record TacticalAnalysis(
        int minute,
        String conclusion,
        List<String> evidence,
        double confidence,
        String riskLevel
) {
}
