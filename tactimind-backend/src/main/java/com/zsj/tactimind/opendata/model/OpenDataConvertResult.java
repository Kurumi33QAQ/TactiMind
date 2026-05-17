package com.zsj.tactimind.opendata.model;

import java.util.List;
import java.util.Map;

/**
 * 本地公开数据转换结果。
 * eventTypeCounts 用来快速确认原始事件被转换成了哪些 TactiMind 统一事件类型。
 */
public record OpenDataConvertResult(
        String matchCode,
        String sourceType,
        String rawEventFile,
        String outputFile,
        int rawEventCount,
        int convertedEventCount,
        Map<String, Integer> eventTypeCounts,
        List<String> warnings
) {
}