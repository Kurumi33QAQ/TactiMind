package com.zsj.tactimind.catalog.model;

import java.util.List;

/**
 * 球员战术资料。
 * 第一版先使用手工构造数据，后续可迁移到球员画像表或公开数据源。
 */
public record PlayerProfile(
        int number,
        String name,
        String position,
        String role,
        String status,
        int pitchX,
        int pitchY,
        List<String> abilityTags
) {
}
