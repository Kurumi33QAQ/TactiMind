package com.zsj.tactimind.opendata.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.zsj.tactimind.match.model.EventType;
import com.zsj.tactimind.match.model.MatchEvent;
import com.zsj.tactimind.opendata.model.OpenDataConvertRequest;
import com.zsj.tactimind.opendata.model.OpenDataConvertResult;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * StatsBomb Open Data 本地转换器。
 *
 * 设计原则：
 * 1. 只读取本地 raw 目录中的 JSON，保证 0 成本且不依赖运行时外网。
 * 2. 只转换 TactiMind 第一版 Agent 真正会使用的高价值事件，普通传球等噪声事件先跳过。
 * 3. 转换后的事件必须带 sourceType/originalType 等字段，方便前端和 Agent 区分真实公开数据与模拟数据。
 */
@Service
public class StatsBombLocalEventConverter {
    private static final String SOURCE_TYPE = "STATSBOMB_OPEN_DATA";

    private final ObjectMapper objectMapper;
    private final StatsBombDisplayLocalizer localizer;
    private final Path rawDirectory;
    private final Path convertedDirectory;

    public StatsBombLocalEventConverter(ObjectMapper objectMapper, StatsBombDisplayLocalizer localizer) {
        this.objectMapper = objectMapper.copy().enable(SerializationFeature.INDENT_OUTPUT);
        this.localizer = localizer;
        this.rawDirectory = Path.of("..", "data", "open-data", "statsbomb", "raw").normalize();
        this.convertedDirectory = Path.of("..", "data", "open-data", "statsbomb", "converted").normalize();
    }

    public OpenDataConvertResult convert(OpenDataConvertRequest request) throws IOException {
        String matchCode = requireText(request.matchCode(), "matchCode 不能为空");
        String rawEventFile = requireText(request.rawEventFile(), "rawEventFile 不能为空");
        String outputFile = request.outputFile() == null || request.outputFile().isBlank()
                ? matchCode + ".json"
                : request.outputFile();

        Path rawPath = resolveInside(rawDirectory, rawEventFile);
        Path outputPath = resolveInside(convertedDirectory, outputFile);
        Files.createDirectories(outputPath.getParent());

        JsonNode root = readStatsBombArray(rawPath);
        List<MatchEvent> convertedEvents = convertToEvents(root);
        Map<String, Integer> eventTypeCounts = countByType(convertedEvents);
        List<String> warnings = warnings(root.size(), convertedEvents.size());

        objectMapper.writeValue(outputPath.toFile(), convertedEvents);
        return new OpenDataConvertResult(
                matchCode,
                SOURCE_TYPE,
                rawEventFile,
                convertedDirectory.resolve(outputFile).normalize().toString().replace('\\', '/'),
                root.size(),
                convertedEvents.size(),
                eventTypeCounts,
                warnings
        );
    }

    /**
     * 供 MatchEventDataSource 直接复用：比赛库可以直接指向 raw 目录中的 StatsBomb 原始事件文件。
     */
    public List<MatchEvent> convertToEvents(Path rawPath) throws IOException {
        return convertToEvents(readStatsBombArray(rawPath));
    }

    private JsonNode readStatsBombArray(Path rawPath) throws IOException {
        JsonNode root = objectMapper.readTree(rawPath.toFile());
        if (!root.isArray()) {
            throw new IllegalArgumentException("StatsBomb 事件文件必须是 JSON 数组");
        }
        return root;
    }

    private List<MatchEvent> convertToEvents(JsonNode root) {
        List<MatchEvent> convertedEvents = new ArrayList<>();
        for (JsonNode rawEvent : root) {
            Optional<MatchEvent> converted = convertOne(rawEvent);
            converted.ifPresent(convertedEvents::add);
        }
        return convertedEvents.stream()
                .sorted(Comparator.comparingInt(MatchEvent::getMinute))
                .toList();
    }

    private Map<String, Integer> countByType(List<MatchEvent> events) {
        Map<String, Integer> eventTypeCounts = new LinkedHashMap<>();
        for (MatchEvent event : events) {
            eventTypeCounts.merge(event.getType().name(), 1, Integer::sum);
        }
        return eventTypeCounts;
    }

    private List<String> warnings(int rawEventCount, int convertedEventCount) {
        List<String> warnings = new ArrayList<>();
        if (convertedEventCount == 0) {
            warnings.add("没有转换出可用于 TactiMind 演练的关键事件，请检查原始事件类型是否包含 Shot、Pass、Duel、Interception 等。");
        }
        if (convertedEventCount < rawEventCount) {
            warnings.add("已跳过普通传球、无球移动、点球大战等暂不适合第一版演练的事件，避免事件流过密或比分统计失真。");
        }
        return warnings;
    }

    private Optional<MatchEvent> convertOne(JsonNode rawEvent) {
        int period = rawEvent.path("period").asInt(1);
        if (period > 4) {
            return Optional.empty();
        }

        String originalType = text(rawEvent, "type", "name");
        if (originalType == null || originalType.isBlank()) {
            return Optional.empty();
        }

        EventType targetType = mapEventType(originalType, rawEvent);
        if (targetType == null) {
            return Optional.empty();
        }

        int minute = rawEvent.path("minute").asInt(0);
        String team = defaultText(text(rawEvent, "team", "name"), "未知球队");
        String player = defaultText(text(rawEvent, "player", "name"), "未知球员");
        String playPattern = defaultText(text(rawEvent, "play_pattern", "name"), "未知进攻方式");
        String zone = resolveZone(rawEvent.path("location"));
        String direction = resolveDirection(rawEvent.path("location"));
        String phase = resolvePhase(playPattern);
        String result = resolveResult(targetType, rawEvent);

        String displayTeam = localizer.team(team);
        String displayPlayer = localizer.player(player);
        String displayType = localizer.eventType(targetType);
        String displayZone = localizer.zone(zone);
        String displayPhase = localizer.phase(phase);
        String displayPlayPattern = localizer.playPattern(playPattern);
        String displayResult = localizer.result(result);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sourceType", SOURCE_TYPE);
        data.put("originalType", originalType);
        data.put("displayType", displayType);
        data.put("period", period);
        data.put("playPattern", playPattern);
        data.put("playPatternDisplay", displayPlayPattern);
        data.put("zone", zone);
        data.put("zoneDisplay", displayZone);
        data.put("direction", direction);
        data.put("directionDisplay", localizer.zone(direction));
        data.put("phase", phase);
        data.put("phaseDisplay", displayPhase);
        data.put("result", result);
        data.put("resultDisplay", displayResult);
        data.put("displayTeam", displayTeam);
        data.put("displayPlayer", displayPlayer);
        if (targetType == EventType.SHOT || targetType == EventType.SHOT_ON_TARGET || targetType == EventType.GOAL) {
            data.put("shot_on_target", targetType == EventType.SHOT_ON_TARGET || targetType == EventType.GOAL);
        }

        MatchEvent event = new MatchEvent();
        event.setMinute(minute);
        event.setTeam(team);
        event.setType(targetType);
        event.setPlayer(player);
        event.setDescription(buildChineseDescription(minute, displayTeam, displayPlayer, targetType, displayZone, displayResult));
        event.setData(data);
        return Optional.of(event);
    }

    private EventType mapEventType(String originalType, JsonNode rawEvent) {
        String normalized = originalType.toLowerCase(Locale.ROOT);
        if (normalized.equals("shot")) {
            String outcome = defaultText(text(rawEvent, "shot", "outcome", "name"), "");
            if ("Goal".equalsIgnoreCase(outcome)) {
                return EventType.GOAL;
            }
            return isShotOnTarget(outcome) ? EventType.SHOT_ON_TARGET : EventType.SHOT;
        }
        if (normalized.equals("pass")) {
            JsonNode pass = rawEvent.path("pass");
            if (pass.path("goal_assist").asBoolean(false) || pass.path("shot_assist").asBoolean(false)) {
                return EventType.KEY_PASS;
            }
            return null;
        }
        if (normalized.equals("dribble") || normalized.equals("carry")) {
            return isAdvancedLocation(rawEvent.path("location")) ? EventType.DANGEROUS_ATTACK : null;
        }
        if (normalized.equals("duel") || normalized.equals("interception") || normalized.equals("ball recovery") || normalized.equals("block")) {
            return EventType.TACKLE;
        }
        if (normalized.equals("dispossessed") || normalized.equals("miscontrol")) {
            return EventType.TURNOVER;
        }
        if (normalized.equals("substitution")) {
            return EventType.SUBSTITUTION;
        }
        return null;
    }

    private boolean isShotOnTarget(String outcome) {
        return "Saved".equalsIgnoreCase(outcome)
                || "Saved to Post".equalsIgnoreCase(outcome)
                || "Goal".equalsIgnoreCase(outcome);
    }

    private boolean isAdvancedLocation(JsonNode location) {
        return location.isArray() && location.size() >= 1 && location.get(0).asDouble(0) >= 80D;
    }

    private String resolveZone(JsonNode location) {
        if (!location.isArray() || location.size() < 2) {
            return "unknown";
        }
        double x = location.get(0).asDouble();
        double y = location.get(1).asDouble();
        if (x >= 102D) {
            return "box";
        }
        if (y < 26.6D) {
            return "left";
        }
        if (y > 53.3D) {
            return "right";
        }
        return "middle";
    }

    private String resolveDirection(JsonNode location) {
        String zone = resolveZone(location);
        if ("box".equals(zone)) {
            return "middle";
        }
        return zone;
    }

    private String resolvePhase(String playPattern) {
        String value = playPattern.toLowerCase(Locale.ROOT);
        if (value.contains("counter")) {
            return "transition";
        }
        if (value.contains("corner") || value.contains("free kick") || value.contains("throw-in") || value.contains("kick off")) {
            return "set_piece";
        }
        return "build_up";
    }

    private String resolveResult(EventType targetType, JsonNode rawEvent) {
        if (targetType == EventType.GOAL) {
            return "goal";
        }
        if (targetType == EventType.SHOT || targetType == EventType.SHOT_ON_TARGET) {
            return defaultText(text(rawEvent, "shot", "outcome", "name"), "unknown");
        }
        if (targetType == EventType.KEY_PASS) {
            return "created_chance";
        }
        if (targetType == EventType.TURNOVER) {
            return "lost_possession";
        }
        return "recorded";
    }

    private String buildChineseDescription(int minute, String team, String player, EventType type, String zoneText, String result) {
        return switch (type) {
            case GOAL -> "第" + minute + "分钟，" + team + "由" + player + "在" + zoneText + "完成进球。";
            case SHOT_ON_TARGET -> "第" + minute + "分钟，" + team + "由" + player + "在" + zoneText + "完成射正，结果：" + result + "。";
            case SHOT -> "第" + minute + "分钟，" + team + "由" + player + "在" + zoneText + "完成射门，结果：" + result + "。";
            case KEY_PASS -> "第" + minute + "分钟，" + team + "由" + player + "送出关键传球并制造机会。";
            case DANGEROUS_ATTACK -> "第" + minute + "分钟，" + team + "通过" + zoneText + "推进形成危险进攻。";
            case TACKLE -> "第" + minute + "分钟，" + team + "由" + player + "完成防守动作。";
            case TURNOVER -> "第" + minute + "分钟，" + team + "由" + player + "出现丢失球权。";
            case SUBSTITUTION -> "第" + minute + "分钟，" + team + "进行换人调整。";
            default -> "第" + minute + "分钟，" + team + "发生" + type.name() + "事件。";
        };
    }

    private Path resolveInside(Path baseDirectory, String fileName) {
        Path candidate = baseDirectory.resolve(fileName).normalize();
        if (!candidate.startsWith(baseDirectory.normalize())) {
            throw new IllegalArgumentException("文件路径不能跳出公开数据目录：" + fileName);
        }
        return candidate;
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String text(JsonNode node, String... path) {
        JsonNode current = node;
        for (String key : path) {
            current = current.path(key);
            if (current.isMissingNode() || current.isNull()) {
                return null;
            }
        }
        return current.asText(null);
    }
}