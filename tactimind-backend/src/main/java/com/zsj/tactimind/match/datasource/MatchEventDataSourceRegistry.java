package com.zsj.tactimind.match.datasource;

import com.zsj.tactimind.catalog.model.MatchCatalogItem;
import com.zsj.tactimind.match.model.MatchEvent;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 事件数据源注册表。
 * 模拟器只依赖这里，后续新增 StatsBomb/Open Data 适配器时不需要改模拟器主流程。
 */
@Service
public class MatchEventDataSourceRegistry {
    private final List<MatchEventDataSource> dataSources;

    public MatchEventDataSourceRegistry(List<MatchEventDataSource> dataSources) {
        this.dataSources = dataSources;
    }

    public List<MatchEvent> loadEvents(MatchCatalogItem match) {
        return dataSources.stream()
                .filter(dataSource -> dataSource.supports(match))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("该比赛暂无可用事件数据源：" + match.matchCode()))
                .loadEvents(match);
    }

    public String resolveDataSourceName(MatchCatalogItem match) {
        return dataSources.stream()
                .filter(dataSource -> dataSource.supports(match))
                .findFirst()
                .map(MatchEventDataSource::name)
                .orElse("UNSUPPORTED");
    }

    public List<String> availableDataSourceNames() {
        return dataSources.stream()
                .map(MatchEventDataSource::name)
                .toList();
    }
}
