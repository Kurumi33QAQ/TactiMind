package com.zsj.tactimind.catalog.service;

import com.zsj.tactimind.catalog.model.MatchTacticalProfile;
import com.zsj.tactimind.catalog.model.PlayerProfile;
import com.zsj.tactimind.catalog.model.TeamTacticalProfile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 比赛战术资料服务。
 * 当前使用内置演练数据，后续可以替换为 MySQL 表或公开数据源同步。
 */
@Service
public class MatchTacticalProfileService {
    private final Map<String, MatchTacticalProfile> profiles = Map.of(
            "world-cup-2022-argentina-france",
            new MatchTacticalProfile(
                    "world-cup-2022-argentina-france",
                    new TeamTacticalProfile(
                            "Argentina",
                            "Lionel Scaloni",
                            "4-3-3",
                            "中路组织 + 左侧突破",
                            "中等强度压迫，丢球后优先延缓反击",
                            "依靠前腰和边锋在肋部区域制造推进",
                            List.of(
                                    player(23, "Martinez", "门将", "出球门将", "稳定", 50, 92, "扑救", "长传"),
                                    player(26, "Molina", "右后卫", "边路往返", "状态良好", 82, 72, "速度", "回追", "传中"),
                                    player(13, "Romero", "中卫", "对抗中卫", "稳定", 62, 76, "对抗", "拦截", "防空"),
                                    player(19, "Otamendi", "中卫", "防线指挥", "经验丰富", 38, 76, "对抗", "站位", "指挥"),
                                    player(3, "Tagliafico", "左后卫", "防守型边后卫", "稳定", 18, 72, "回追", "抢断", "覆盖"),
                                    player(7, "De Paul", "中场", "覆盖型中场", "体能充沛", 70, 52, "跑动", "压迫", "接应"),
                                    player(24, "Fernandez", "后腰", "节奏调度", "状态良好", 50, 56, "传球", "拦截", "转移"),
                                    player(20, "Mac Allister", "中场", "连接中场", "稳定", 30, 52, "传球", "跑动", "前插"),
                                    player(10, "Messi", "右路内收", "核心组织者", "关键球员", 68, 30, "传球", "持球推进", "终结"),
                                    player(9, "Alvarez", "中锋", "前场压迫点", "体能充沛", 50, 24, "压迫", "跑动", "终结"),
                                    player(11, "Di Maria", "左边锋", "左路突破点", "状态良好", 24, 30, "速度", "突破", "传中")
                            ),
                            List.of(
                                    player(21, "Dybala", "前腰", "替补创造力", "待命", 0, 0, "传球", "远射"),
                                    player(22, "Lautaro", "中锋", "替补冲击点", "待命", 0, 0, "终结", "对抗"),
                                    player(5, "Paredes", "后腰", "控球调度", "待命", 0, 0, "传球", "对抗"),
                                    player(6, "Pezzella", "中卫", "防守替补", "待命", 0, 0, "防空", "对抗"),
                                    player(2, "Foyth", "后卫", "边中卫替补", "待命", 0, 0, "站位", "对抗")
                            )
                    ),
                    new TeamTacticalProfile(
                            "France",
                            "Didier Deschamps",
                            "4-2-3-1",
                            "快速反击 + 右路冲击",
                            "阶段性高压，更多依靠中前场个人能力",
                            "右路速度冲击和前场转换推进",
                            List.of(
                                    player(1, "Lloris", "门将", "防线指挥", "稳定", 50, 92, "扑救", "指挥"),
                                    player(5, "Kounde", "右后卫", "防守型边后卫", "稳定", 82, 72, "回追", "对抗", "站位"),
                                    player(4, "Varane", "中卫", "防线核心", "稳定", 62, 76, "防空", "对抗", "出球"),
                                    player(18, "Upamecano", "中卫", "身体型中卫", "对抗优势", 38, 76, "速度", "对抗", "拦截"),
                                    player(22, "Theo Hernandez", "左后卫", "前插边后卫", "状态良好", 18, 72, "速度", "前插", "传中"),
                                    player(8, "Tchouameni", "后腰", "防守屏障", "稳定", 42, 56, "拦截", "对抗", "长传"),
                                    player(14, "Rabiot", "中场", "覆盖型中场", "状态良好", 58, 54, "跑动", "前插", "对抗"),
                                    player(11, "Dembele", "右边锋", "边路爆点", "需要观察", 78, 34, "速度", "突破", "传中"),
                                    player(7, "Griezmann", "前腰", "串联核心", "状态良好", 50, 38, "传球", "跑动", "防守覆盖"),
                                    player(10, "Mbappe", "左边锋", "反击爆点", "关键球员", 22, 34, "速度", "突破", "终结"),
                                    player(9, "Giroud", "中锋", "支点中锋", "对抗优势", 50, 22, "对抗", "头球", "做球")
                            ),
                            List.of(
                                    player(20, "Coman", "边锋", "替补速度点", "待命", 0, 0, "速度", "突破"),
                                    player(12, "Kolo Muani", "前锋", "替补冲击点", "待命", 0, 0, "跑动", "终结"),
                                    player(25, "Camavinga", "中场", "多面手", "待命", 0, 0, "覆盖", "推进"),
                                    player(24, "Konate", "中卫", "防守替补", "待命", 0, 0, "防空", "对抗"),
                                    player(19, "Fofana", "中场", "冲击型中场", "待命", 0, 0, "跑动", "对抗")
                            )
                    ),
                    List.of(
                            "阿根廷需要保护中路组织核心，避免被法国反击直接打身后。",
                            "法国的速度型边锋会影响阿根廷边后卫站位和回追压力。",
                            "该资料为项目演练用战术上下文，不能替代真实赛前报告。"
                    ),
                    List.of(
                            "阵容和能力标签为手工构造的演练数据。",
                            "后续 Agent 只能把这些资料作为辅助依据，不能把模拟资料当成真实事实。"
                    )
            )
    );

    public Optional<MatchTacticalProfile> findByMatchCode(String matchCode) {
        return Optional.ofNullable(profiles.get(matchCode));
    }

    private static PlayerProfile player(
            int number,
            String name,
            String position,
            String role,
            String status,
            int pitchX,
            int pitchY,
            String... abilityTags
    ) {
        return new PlayerProfile(number, name, position, role, status, pitchX, pitchY, List.of(abilityTags));
    }
}
