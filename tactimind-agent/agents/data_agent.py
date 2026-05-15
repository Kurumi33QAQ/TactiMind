from collections import Counter, defaultdict
from typing import List

from schemas.analysis_schema import AnalyzeRequest, DataInsight, MatchEvent


class DataAgent:
    """数据分析 Agent：只从比赛状态和近期事件中提取可验证的数据趋势。"""

    def analyze(self, request: AnalyzeRequest) -> List[DataInsight]:
        """汇总多个规则检测结果，形成 DataInsight 列表。"""
        insights: List[DataInsight] = []
        recent_events = request.recentEvents
        if not recent_events:
            return insights

        insights.extend(self._detect_repeated_pressure(recent_events))
        insights.extend(self._detect_shot_pressure(recent_events))
        insights.extend(self._detect_possession_gap(request))
        return insights

    def _detect_repeated_pressure(self, events: List[MatchEvent]) -> List[DataInsight]:
        """检测某队是否在同一区域连续制造进攻压力。"""
        attack_events_by_team_zone: dict[tuple[str, str], list[MatchEvent]] = defaultdict(list)
        for event in events:
            if event.type not in {"DANGEROUS_ATTACK", "CORNER", "SHOT", "SHOT_ON_TARGET", "GOAL"}:
                continue
            zone = str(event.data.get("zone", "unknown"))
            attack_events_by_team_zone[(event.team, zone)].append(event)

        insights: List[DataInsight] = []
        for (team, zone), zone_events in attack_events_by_team_zone.items():
            if len(zone_events) < 3 or zone == "unknown":
                continue
            start_minute = min(event.minute for event in zone_events)
            end_minute = max(event.minute for event in zone_events)
            team_label = self._team_name(team)
            evidence = [
                f"第{start_minute}到{end_minute}分钟，{team_label}在{self._zone_name(zone)}累计制造{len(zone_events)}次进攻相关事件"
            ]
            insights.append(DataInsight(
                code="REPEATED_ZONE_PRESSURE",
                subjectTeam=team,
                summary=f"{team_label}在{self._zone_name(zone)}形成连续压力",
                evidence=evidence,
                strength=min(1.0, 0.45 + len(zone_events) * 0.12),
            ))
        return insights

    def _detect_shot_pressure(self, events: List[MatchEvent]) -> List[DataInsight]:
        """检测近期射门或进球事件是否集中出现。"""
        shot_counter = Counter(
            event.team for event in events if event.type in {"SHOT", "SHOT_ON_TARGET", "GOAL"}
        )
        insights: List[DataInsight] = []
        for team, shots in shot_counter.items():
            if shots < 2:
                continue
            minutes = [event.minute for event in events if event.team == team and event.type in {"SHOT", "SHOT_ON_TARGET", "GOAL"}]
            team_label = self._team_name(team)
            evidence = [
                f"第{min(minutes)}到{max(minutes)}分钟，{team_label}完成{shots}次射门或进球事件"
            ]
            insights.append(DataInsight(
                code="SHOT_PRESSURE",
                subjectTeam=team,
                summary=f"{team_label}近期射门压力上升",
                evidence=evidence,
                strength=min(1.0, 0.4 + shots * 0.15),
            ))
        return insights

    def _detect_possession_gap(self, request: AnalyzeRequest) -> List[DataInsight]:
        """检测双方控球率是否已经形成明显差距。"""
        teams = request.matchState.teams
        if len(teams) < 2:
            return []

        sorted_teams = list(teams.items())
        first_team, first_stats = sorted_teams[0]
        second_team, second_stats = sorted_teams[1]
        gap = abs(first_stats.possessionRate - second_stats.possessionRate)
        if gap < 10:
            return []

        leading_team = first_team if first_stats.possessionRate > second_stats.possessionRate else second_team
        leading_rate = teams[leading_team].possessionRate
        leading_team_label = self._team_name(leading_team)
        evidence = [
            f"当前{leading_team_label}控球率为{leading_rate}%，双方控球率差距达到{gap}个百分点"
        ]
        return [DataInsight(
            code="POSSESSION_GAP",
            subjectTeam=leading_team,
            summary=f"{leading_team_label}在控球上占据明显优势",
            evidence=evidence,
            strength=min(1.0, 0.5 + gap / 100),
        )]

    def _zone_name(self, zone: str) -> str:
        """把事件里的英文区域值转换成中文展示。"""
        names = {
            "left": "左路",
            "right": "右路",
            "middle": "中路",
        }
        return names.get(zone, zone)

    def _team_name(self, team: str) -> str:
        """把内部球队名转换成中文展示名。"""
        names = {
            "Team A": "A队",
            "Team B": "B队",
        }
        return names.get(team, team)
