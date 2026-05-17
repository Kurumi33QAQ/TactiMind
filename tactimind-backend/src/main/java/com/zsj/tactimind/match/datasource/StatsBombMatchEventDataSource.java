package com.zsj.tactimind.match.datasource;

import com.zsj.tactimind.catalog.model.MatchCatalogItem;
import com.zsj.tactimind.catalog.model.SourceType;
import com.zsj.tactimind.match.model.MatchEvent;
import com.zsj.tactimind.opendata.service.StatsBombLocalEventConverter;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * StatsBomb Open Data 事件数据源。
 * 比赛目录只要标记为 STATSBOMB_OPEN_DATA，就可以直接读取 raw 目录中的真实公开事件 JSON。
 */
@Order(0)
@Component
public class StatsBombMatchEventDataSource implements MatchEventDataSource {
    private final StatsBombLocalEventConverter converter;

    public StatsBombMatchEventDataSource(StatsBombLocalEventConverter converter) {
        this.converter = converter;
    }

    @Override
    public String name() {
        return "STATSBOMB_OPEN_DATA_RAW_JSON";
    }

    @Override
    public boolean supports(MatchCatalogItem match) {
        return match.sourceType() == SourceType.STATSBOMB_OPEN_DATA
                && match.eventFilePath() != null
                && !match.eventFilePath().isBlank();
    }

    @Override
    public List<MatchEvent> loadEvents(MatchCatalogItem match) {
        Path rawEventsFile = Path.of(match.eventFilePath());
        try {
            return converter.convertToEvents(rawEventsFile);
        } catch (IOException e) {
            throw new IllegalStateException("加载 StatsBomb 公开事件数据失败，比赛=" + match.matchCode()
                    + "，文件路径=" + rawEventsFile.toAbsolutePath(), e);
        }
    }
}