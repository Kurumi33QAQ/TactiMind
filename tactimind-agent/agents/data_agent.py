from collections import Counter, defaultdict
from typing import List

from schemas.analysis_schema import AnalyzeRequest, DataInsight, MatchEvent, TeamTacticalProfile


class DataAgent:
    """数据分析 Agent：只从比赛状态和近期事件中提取可验证的数据趋势。"""

    def analyze(self, request: AnalyzeRequest) -> List[DataInsight]:
        """汇总多个规则检测结果，形成 DataInsight 列表。"""
        insights: List[DataInsight] = []
        recent_events = request.recentEvents
        if not recent_events:
            return insights

        insights.extend(self._detect_repeated_pressure(request, recent_events))
        insights.extend(self._detect_shot_pressure(request, recent_events))
        insights.extend(self._detect_possession_gap(request))
        insights.extend(self._detect_profile_context(request, recent_events))
        return insights

    def _detect_profile_context(self, request: AnalyzeRequest, events: List[MatchEvent]) -> List[DataInsight]:
        """结合阵型、教练和关键球员能力，解释近期事件背后的上下文。"""
        profile = request.tacticalProfile
        if not profile:
            return []

        active_counter = Counter(event.team for event in events if event.type in {"SHOT", "SHOT_ON_TARGET", "GOAL", "DANGEROUS_ATTACK", "CORNER"})
        insights: List[DataInsight] = []

        for team, event_count in active_counter.items():
            team_profile = self._profile_team_for_event_team(team, profile)
            if event_count < 2 or not team_profile:
                continue
            key_player = self._select_key_player(team_profile)
            team_label = self._team_name(team_profile.team)
            evidence = [
                f"{team_label}本场阵型为{team_profile.formation}，主教练为{team_profile.coach}",
                f"近段时间{team_label}出现{event_count}次射门、角球、进球或危险进攻事件"
            ]
            if key_player:
                evidence.append(
                    f"{key_player.name}的角色是{key_player.role}，能力标签包括{'、'.join(key_player.abilityTags[:3])}"
                )
            if profile.dataNotes:
                evidence.append(profile.dataNotes[0])

            insights.append(DataInsight(
                code="PROFILE_CONTEXT_SUPPORT",
                subjectTeam=team_profile.team,
                summary=f"{team_label}的阵型和关键球员特点为近期趋势提供了上下文依据",
                evidence=evidence,
                strength=0.68 if key_player else 0.6,
            ))
        return insights

    def _select_key_player(self, team_profile: TeamTacticalProfile):
        """优先选择标注为关键球员或角色偏核心的球员。"""
        for player in team_profile.startingLineup:
            text = f"{player.status} {player.role}"
            if "关键" in text or "核心" in text or "爆点" in text:
                return player
        return team_profile.startingLineup[0] if team_profile.startingLineup else None

    def _detect_repeated_pressure(self, request: AnalyzeRequest, events: List[MatchEvent]) -> List[DataInsight]:
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
            subject_team = self._subject_team(team, request)
            team_label = self._team_name(subject_team)
            evidence = [
                f"第{start_minute}到{end_minute}分钟，{team_label}在{self._zone_name(zone)}累计制造{len(zone_events)}次进攻相关事件"
            ]
            insights.append(DataInsight(
                code="REPEATED_ZONE_PRESSURE",
                subjectTeam=subject_team,
                summary=f"{team_label}在{self._zone_name(zone)}形成连续压力",
                evidence=evidence,
                strength=min(1.0, 0.45 + len(zone_events) * 0.12),
            ))
        return insights

    def _detect_shot_pressure(self, request: AnalyzeRequest, events: List[MatchEvent]) -> List[DataInsight]:
        """检测近期射门或进球事件是否集中出现。"""
        shot_counter = Counter(
            event.team for event in events if event.type in {"SHOT", "SHOT_ON_TARGET", "GOAL"}
        )
        insights: List[DataInsight] = []
        for team, shots in shot_counter.items():
            if shots < 2:
                continue
            minutes = [event.minute for event in events if event.team == team and event.type in {"SHOT", "SHOT_ON_TARGET", "GOAL"}]
            subject_team = self._subject_team(team, request)
            team_label = self._team_name(subject_team)
            evidence = [
                f"第{min(minutes)}到{max(minutes)}分钟，{team_label}完成{shots}次射门或进球事件"
            ]
            insights.append(DataInsight(
                code="SHOT_PRESSURE",
                subjectTeam=subject_team,
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
        subject_team = self._subject_team(leading_team, request)
        leading_rate = teams[leading_team].possessionRate
        leading_team_label = self._team_name(subject_team)
        evidence = [
            f"当前{leading_team_label}控球率为{leading_rate}%，双方控球率差距达到{gap}个百分点"
        ]
        return [DataInsight(
            code="POSSESSION_GAP",
            subjectTeam=subject_team,
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

    def _subject_team(self, team: str, request: AnalyzeRequest) -> str:
        """把事件流里的 Team A/B 映射成 Profile 里的真实球队名。"""
        profile = request.tacticalProfile
        if not profile:
            return team
        mapped_team = self._profile_team_for_event_team(team, profile)
        return mapped_team.team if mapped_team else team

    def _profile_team_for_event_team(self, team: str, profile):
        """兼容 demo 事件流 Team A/B 和真实队名两种写法。"""
        if team == profile.home.team:
            return profile.home
        if team == profile.away.team:
            return profile.away
        if team == "Team A":
            return profile.home
        if team == "Team B":
            return profile.away
        return None

    def _team_name(self, team: str) -> str:
        """把内部球队名转换成中文展示名。"""
        names = {
            "Team A": "A队",
            "Team B": "B队",
            "Argentina": "阿根廷",
            "France": "法国",
        }
        return names.get(team, team)
