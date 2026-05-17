package com.zsj.tactimind.catalog.service;

import com.zsj.tactimind.catalog.model.DataLevel;
import com.zsj.tactimind.catalog.model.MatchCatalogItem;
import com.zsj.tactimind.catalog.model.SourceType;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class MatchCatalogService {
    private final List<MatchCatalogItem> matches = List.of(
            new MatchCatalogItem(
                    1L,
                    "world-cup-2022-argentina-france",
                    "Argentina",
                    "France",
                    "argentina.png",
                    "france.png",
                    "World Cup",
                    "2022",
                    LocalDate.of(2022, 12, 18),
                    SourceType.AI_SIMULATED,
                    DataLevel.SIMULATED_EVENT,
                    "../data/events/world-cup-2022-argentina-france-simulated.json",
                    true,
                    "世界杯决赛主题的模拟事件流，用于演示实时事件回放、Agent 分析和防幻觉校验。",
                    List.of("事件流回放", "实时战术分析", "结构化报告", "证据校验", "WebSocket 演练")
            ),
            new MatchCatalogItem(
                    2L,
                    "premier-league-mancity-arsenal-demo",
                    "Manchester City",
                    "Arsenal",
                    "mancity.png",
                    "arsenal.png",
                    "Premier League",
                    "2024/25",
                    LocalDate.of(2025, 3, 30),
                    SourceType.AI_SIMULATED,
                    DataLevel.SIMULATED_EVENT,
                    "../data/events/premier-league-mancity-arsenal-simulated.json",
                    true,
                    "英超强强对话模拟数据，适合演示边路进攻、控球压制和攻防转换分析。",
                    List.of("模拟事件流", "关键指标统计", "战术趋势判断", "Agent Trace")
            ),
            new MatchCatalogItem(
                    3L,
                    "euro-spain-england-basic",
                    "Spain",
                    "England",
                    "spain.png",
                    "england.png",
                    "Euro",
                    "2024",
                    LocalDate.of(2024, 7, 14),
                    SourceType.MANUAL_BUILT,
                    DataLevel.BASIC_STATS,
                    "",
                    true,
                    "基础统计样例，仅包含比分、控球率和射门等摘要数据，适合生成简版赛后总结。",
                    List.of("基础赛后总结", "低置信度战术提示", "数据不足提示")
            ),
            new MatchCatalogItem(
                    4L,
                    "la-liga-real-madrid-barcelona-demo",
                    "Real Madrid",
                    "Barcelona",
                    "realmadrid.png",
                    "barcelona.png",
                    "La Liga",
                    "2024/25",
                    LocalDate.of(2025, 4, 20),
                    SourceType.AI_SIMULATED,
                    DataLevel.SIMULATED_EVENT,
                    "",
                    true,
                    "国家德比模拟演练比赛，用于展示快速反击、肋部推进和高位逼抢分析。",
                    List.of("模拟事件流", "攻防转换分析", "战术建议", "证据校验")
            ),
            new MatchCatalogItem(
                    5L,
                    "bundesliga-bayern-dortmund-demo",
                    "Bayern Munich",
                    "Dortmund",
                    "bayern.png",
                    "dortmund.png",
                    "Bundesliga",
                    "2024/25",
                    LocalDate.of(2025, 3, 8),
                    SourceType.AI_SIMULATED,
                    DataLevel.SIMULATED_EVENT,
                    "",
                    true,
                    "德甲国家德比模拟比赛，适合演示高节奏转换和前场压迫分析。",
                    List.of("模拟事件流", "压迫趋势分析", "关键阶段报告", "Agent Trace")
            ),
            new MatchCatalogItem(
                    6L,
                    "world-cup-2022-argentina-france-open-data",
                    "Argentina",
                    "France",
                    "argentina.png",
                    "france.png",
                    "World Cup",
                    "2022",
                    LocalDate.of(2022, 12, 18),
                    SourceType.STATSBOMB_OPEN_DATA,
                    DataLevel.DEEP_EVENT,
                    "../data/open-data/statsbomb/raw/world-cup-2022-argentina-france-statsbomb.json",
                    true,
                    "StatsBomb Open Data 公开事件数据：2022 世界杯决赛真实事件流。阵容能力标签仍为项目辅助资料，页面和报告需与模拟数据区分。",
                    List.of("真实公开事件流", "实时战术分析", "结构化报告", "证据校验", "WebSocket 演练")
            ),
            new MatchCatalogItem(
                    7L,
                    "world-cup-2022-argentina-croatia-open-data",
                    "Argentina",
                    "Croatia",
                    "argentina.png",
                    "default.png",
                    "World Cup",
                    "2022",
                    LocalDate.of(2022, 12, 13),
                    SourceType.STATSBOMB_OPEN_DATA,
                    DataLevel.DEEP_EVENT,
                    "../data/open-data/statsbomb/raw/world-cup-2022-argentina-croatia-statsbomb.json",
                    true,
                    "StatsBomb Open Data 公开事件数据：2022 世界杯半决赛真实事件流，用于演示真实事件驱动的 Agent 分析。",
                    List.of("真实公开事件流", "关键指标统计", "战术趋势判断", "Agent Trace")
            ),
            new MatchCatalogItem(
                    8L,
                    "world-cup-2022-netherlands-argentina-open-data",
                    "Netherlands",
                    "Argentina",
                    "default.png",
                    "argentina.png",
                    "World Cup",
                    "2022",
                    LocalDate.of(2022, 12, 9),
                    SourceType.STATSBOMB_OPEN_DATA,
                    DataLevel.DEEP_EVENT,
                    "../data/open-data/statsbomb/raw/world-cup-2022-netherlands-argentina-statsbomb.json",
                    true,
                    "StatsBomb Open Data 公开事件数据：2022 世界杯四分之一决赛真实事件流，适合观察阶段性压力和转换机会。",
                    List.of("真实公开事件流", "关键阶段分析", "证据校验", "赛后报告")
            )
    );

    public List<MatchCatalogItem> search(LocalDate date, String team, String competition, DataLevel dataLevel) {
        return matches.stream()
                .filter(match -> date == null || match.matchDate().equals(date))
                .filter(match -> dataLevel == null || match.dataLevel() == dataLevel)
                .filter(match -> containsIgnoreCase(match.competition(), competition)
                        || containsIgnoreCase(competitionDisplayName(match.competition()), competition))
                .filter(match -> team == null || team.isBlank()
                        || containsIgnoreCase(match.homeTeam(), team)
                        || containsIgnoreCase(match.awayTeam(), team)
                        || containsIgnoreCase(teamDisplayName(match.homeTeam()), team)
                        || containsIgnoreCase(teamDisplayName(match.awayTeam()), team))
                .toList();
    }

    public Optional<MatchCatalogItem> findByIdOrCode(String matchId) {
        return matches.stream()
                .filter(match -> String.valueOf(match.id()).equals(matchId) || match.matchCode().equalsIgnoreCase(matchId))
                .findFirst();
    }

    private boolean containsIgnoreCase(String source, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        return source.toLowerCase(Locale.ROOT).contains(keyword.trim().toLowerCase(Locale.ROOT));
    }

    private String teamDisplayName(String team) {
        return switch (team) {
            case "Argentina" -> "阿根廷";
            case "France" -> "法国";
            case "Manchester City" -> "曼城";
            case "Arsenal" -> "阿森纳";
            case "Spain" -> "西班牙";
            case "England" -> "英格兰";
            case "Real Madrid" -> "皇家马德里";
            case "Barcelona" -> "巴塞罗那";
            case "Bayern Munich" -> "拜仁慕尼黑";
            case "Dortmund" -> "多特蒙德";
            case "Open Data Team A" -> "开放数据A队";
            case "Open Data Team B" -> "开放数据B队";
            default -> team;
        };
    }

    private String competitionDisplayName(String competition) {
        return switch (competition) {
            case "World Cup" -> "世界杯";
            case "Premier League" -> "英超";
            case "Euro" -> "欧洲杯";
            case "La Liga" -> "西甲";
            case "Bundesliga" -> "德甲";
            case "Open Data Demo" -> "开放数据演示";
            default -> competition;
        };
    }
}
