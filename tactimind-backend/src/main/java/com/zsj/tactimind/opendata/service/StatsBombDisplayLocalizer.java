package com.zsj.tactimind.opendata.service;

import com.zsj.tactimind.match.model.EventType;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * StatsBomb 公开数据展示本地化工具。
 *
 * 重要原则：不修改真实原始字段，只为页面、报告和 Agent 证据生成中文展示值。
 */
@Component
public class StatsBombDisplayLocalizer {
    private static final Map<String, String> TEAM_NAMES = Map.ofEntries(
            Map.entry("Argentina", "阿根廷"),
            Map.entry("France", "法国"),
            Map.entry("Croatia", "克罗地亚"),
            Map.entry("Netherlands", "荷兰"),
            Map.entry("Australia", "澳大利亚"),
            Map.entry("Poland", "波兰"),
            Map.entry("Brazil", "巴西"),
            Map.entry("England", "英格兰"),
            Map.entry("Morocco", "摩洛哥")
    );

    private static final Map<String, String> PLAYER_NAMES = Map.ofEntries(
            Map.entry("Lionel Messi", "梅西"),
            Map.entry("Ángel Di María", "迪马利亚"),
            Map.entry("Angel Di María", "迪马利亚"),
            Map.entry("Julián Álvarez", "阿尔瓦雷斯"),
            Map.entry("Julian Alvarez", "阿尔瓦雷斯"),
            Map.entry("Emiliano Martínez", "马丁内斯"),
            Map.entry("Rodrigo De Paul", "德保罗"),
            Map.entry("Enzo Fernández", "恩佐·费尔南德斯"),
            Map.entry("Alexis Mac Allister", "麦卡利斯特"),
            Map.entry("Kylian Mbappé", "姆巴佩"),
            Map.entry("Kylian Mbappe", "姆巴佩"),
            Map.entry("Antoine Griezmann", "格列兹曼"),
            Map.entry("Olivier Giroud", "吉鲁"),
            Map.entry("Ousmane Dembélé", "登贝莱"),
            Map.entry("Ousmane Dembele", "登贝莱"),
            Map.entry("Adrien Rabiot", "拉比奥"),
            Map.entry("Aurélien Tchouaméni", "楚阿梅尼"),
            Map.entry("Aurelien Tchouameni", "楚阿梅尼"),
            Map.entry("Luka Modrić", "莫德里奇"),
            Map.entry("Luka Modric", "莫德里奇"),
            Map.entry("Ivan Perišić", "佩里西奇"),
            Map.entry("Ivan Perisic", "佩里西奇"),
            Map.entry("Andrej Kramarić", "克拉马里奇"),
            Map.entry("Andrej Kramaric", "克拉马里奇"),
            Map.entry("Cody Gakpo", "加克波"),
            Map.entry("Memphis Depay", "德佩"),
            Map.entry("Virgil van Dijk", "范戴克"),
            Map.entry("Frenkie de Jong", "弗朗基·德容"),
            Map.entry("Wout Weghorst", "韦霍斯特")
    );

    private static final Map<String, String> PLAY_PATTERN_NAMES = Map.ofEntries(
            Map.entry("Regular Play", "常规进攻"),
            Map.entry("From Counter", "反击"),
            Map.entry("From Corner", "角球进攻"),
            Map.entry("From Free Kick", "任意球进攻"),
            Map.entry("From Throw In", "界外球进攻"),
            Map.entry("From Goal Kick", "门球发起"),
            Map.entry("From Kick Off", "开球阶段"),
            Map.entry("Other", "其他阶段")
    );

    private static final Map<String, String> RESULT_NAMES = Map.ofEntries(
            Map.entry("goal", "进球"),
            Map.entry("Saved", "被扑救"),
            Map.entry("Saved to Post", "扑救后击中门框"),
            Map.entry("Off T", "偏出球门"),
            Map.entry("Blocked", "被封堵"),
            Map.entry("Wayward", "严重偏出"),
            Map.entry("Post", "击中门框"),
            Map.entry("created_chance", "制造机会"),
            Map.entry("lost_possession", "丢失球权"),
            Map.entry("recorded", "已记录"),
            Map.entry("unknown", "未知结果")
    );

    public String team(String rawTeam) {
        return TEAM_NAMES.getOrDefault(rawTeam, rawTeam);
    }

    public String player(String rawPlayer) {
        return PLAYER_NAMES.getOrDefault(rawPlayer, rawPlayer);
    }

    public String eventType(EventType type) {
        return switch (type) {
            case GOAL -> "进球";
            case SHOT_ON_TARGET -> "射正";
            case SHOT -> "射门";
            case KEY_PASS -> "关键传球";
            case DANGEROUS_ATTACK -> "危险进攻";
            case TACKLE -> "防守动作";
            case TURNOVER -> "丢失球权";
            case SUBSTITUTION -> "换人";
            default -> type.name();
        };
    }

    public String playPattern(String rawPlayPattern) {
        return PLAY_PATTERN_NAMES.getOrDefault(rawPlayPattern, rawPlayPattern);
    }

    public String result(String rawResult) {
        return RESULT_NAMES.getOrDefault(rawResult, rawResult);
    }

    public String zone(String zone) {
        return switch (zone) {
            case "box" -> "禁区内";
            case "left" -> "左路";
            case "right" -> "右路";
            case "middle" -> "中路";
            default -> "未知区域";
        };
    }

    public String phase(String phase) {
        return switch (phase) {
            case "transition" -> "攻防转换";
            case "set_piece" -> "定位球";
            case "build_up" -> "组织推进";
            default -> "未知阶段";
        };
    }
}