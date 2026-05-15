package com.zsj.tactimind.agent.model;

import com.zsj.tactimind.match.model.MatchEvent;
import com.zsj.tactimind.match.model.MatchState;

import java.util.List;

/**
 * Java 后端调用 Python Agent 服务时提交的请求体。
 */
public record AgentAnalyzeRequest(
        MatchState matchState,
        List<MatchEvent> recentEvents
) {
}
