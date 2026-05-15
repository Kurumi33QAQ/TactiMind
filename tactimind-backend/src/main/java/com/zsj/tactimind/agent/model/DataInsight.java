package com.zsj.tactimind.agent.model;

import java.util.List;

/**
 * DataAgent 输出的数据洞察。
 * 它只描述可验证的数据趋势，不直接下战术结论。
 */
public record DataInsight(
        int minute,
        String code,
        String subjectTeam,
        String targetTeam,
        String summary,
        List<String> evidence,
        double strength
) {
}
