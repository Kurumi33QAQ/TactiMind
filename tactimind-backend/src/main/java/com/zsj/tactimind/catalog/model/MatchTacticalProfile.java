package com.zsj.tactimind.catalog.model;

import java.util.List;

/**
 * 比赛战术资料。
 * 用来补充事件流以外的阵型、阵容、球员能力和数据可信度说明。
 */
public record MatchTacticalProfile(
        String matchCode,
        TeamTacticalProfile home,
        TeamTacticalProfile away,
        List<String> keyFactors,
        List<String> dataNotes
) {
}
