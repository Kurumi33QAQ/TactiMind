package com.zsj.tactimind.match.datasource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zsj.tactimind.catalog.model.MatchCatalogItem;
import com.zsj.tactimind.match.model.MatchEvent;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

/**
 * 本地 JSON 事件流数据源。
 * 当前 0 成本演示数据和后续导入后的公开数据，都可以先落成统一 JSON 再通过这里加载。
 */
@Component
public class LocalJsonMatchEventDataSource implements MatchEventDataSource {
    private final ObjectMapper objectMapper;

    public LocalJsonMatchEventDataSource(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "LOCAL_JSON_EVENT_FILE";
    }

    @Override
    public boolean supports(MatchCatalogItem match) {
        return match.eventFilePath() != null && !match.eventFilePath().isBlank();
    }

    @Override
    public List<MatchEvent> loadEvents(MatchCatalogItem match) {
        Path eventsFile = Path.of(match.eventFilePath());
        try {
            List<MatchEvent> events = objectMapper.readValue(
                    eventsFile.toFile(),
                    new TypeReference<>() {
                    }
            );
            return events.stream()
                    .sorted(Comparator.comparingInt(MatchEvent::getMinute))
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("加载比赛事件流失败，比赛=" + match.matchCode()
                    + "，文件路径=" + eventsFile.toAbsolutePath(), e);
        }
    }
}
