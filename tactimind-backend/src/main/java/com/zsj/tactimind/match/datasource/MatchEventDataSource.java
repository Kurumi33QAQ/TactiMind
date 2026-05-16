package com.zsj.tactimind.match.datasource;

import com.zsj.tactimind.catalog.model.MatchCatalogItem;
import com.zsj.tactimind.match.model.MatchEvent;

import java.util.List;

/**
 * 比赛事件数据源适配器。
 * 不同来源的数据先转换成统一 MatchEvent，再交给模拟器和 Agent 使用。
 */
public interface MatchEventDataSource {
    String name();

    boolean supports(MatchCatalogItem match);

    List<MatchEvent> loadEvents(MatchCatalogItem match);
}
