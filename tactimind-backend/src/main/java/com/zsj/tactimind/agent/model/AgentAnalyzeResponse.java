package com.zsj.tactimind.agent.model;

import java.util.List;

/**
 * Python Agent 服务 /analyze 接口的响应结构。
 */
public record AgentAnalyzeResponse(
        int minute,
        List<TacticalAnalysis> analyses
) {
}
