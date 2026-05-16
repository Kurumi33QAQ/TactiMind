package com.zsj.tactimind.catalog.model;

import java.util.List;

/**
 * 单支球队在某场比赛中的战术上下文。
 */
public record TeamTacticalProfile(
        String team,
        String coach,
        String formation,
        String style,
        String pressingStyle,
        String buildUpFocus,
        List<PlayerProfile> startingLineup,
        List<PlayerProfile> substitutes
) {
}
