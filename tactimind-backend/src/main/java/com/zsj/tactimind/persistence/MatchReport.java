package com.zsj.tactimind.persistence;

import com.zsj.tactimind.agent.model.TacticalAnalysis;
import com.zsj.tactimind.match.model.MatchEvent;
import com.zsj.tactimind.match.model.MatchState;

import java.util.List;

/**
 * 赛后报告对象：聚合当前比赛状态、历史事件和 Agent 分析结果。
 */
public record MatchReport(
        String matchId,
        MatchState currentState,
        List<MatchEvent> events,
        List<TacticalAnalysis> analyses,
        String summary
) {
}
