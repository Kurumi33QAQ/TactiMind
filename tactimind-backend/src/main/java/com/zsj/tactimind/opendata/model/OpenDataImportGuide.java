package com.zsj.tactimind.opendata.model;

import java.util.List;

public record OpenDataImportGuide(
        String sourceType,
        String sourceName,
        boolean zeroCost,
        boolean supportedNow,
        String rawDataDirectory,
        String convertedEventDirectory,
        List<String> importSteps,
        List<String> targetEventSchema,
        List<String> notes
) {
}
