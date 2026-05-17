from collections import Counter, defaultdict
from typing import List

from schemas.analysis_schema import AnalyzeRequest, DataInsight, MatchEvent, TeamTacticalProfile


class DataAgent:
    """数据分析 Agent：只从比赛状态、事件字段和战术资料中提取可验证的数据趋势。"""

    ATTACK_EVENT_TYPES = {"DANGEROUS_ATTACK", "CORNER", "SHOT", "SHOT_ON_TARGET", "GOAL", "KEY_PASS", "TRANSITION"}

    def analyze(self, request: AnalyzeRequest) -> List[DataInsight]:
        """汇总多个规则检测结果，形成 DataInsight 列表。"""
        insights: List[DataInsight] = []
        recent_events = request.recentEvents
        if not recent_events:
            return insights

        insights.extend(self._detect_repeated_pressure(request, recent_events))
        insights.extend(self._detect_key_pass_creation(request, recent_events))
        insights.extend(self._detect_turnover_risk(request, recent_events))
        insights.extend(self._detect_transition_threat(request, recent_events))
        insights.extend(self._detect_shot_pressure(request, recent_events))
        insights.extend(self._detect_possession_gap(request))
        insights.extend(self._detect_profile_context(request, recent_events))
        return insights

    def _detect_profile_context(self, request: AnalyzeRequest, events: List[MatchEvent]) -> List[DataInsight]:
        """结合阵型、教练和关键球员能力，解释近期事件背后的上下文。"""
        profile = request.tacticalProfile
        if not profile:
            return []

        active_counter = Counter(event.team for event in events if event.type in self.ATTACK_EVENT_TYPES)
        insights: List[DataInsight] = []

        for team, event_count in active_counter.items():
            team_profile = self._profile_team_for_event_team(team, profile)
            if event_count < 2 or not team_profile:
                continue
            key_player = self._select_key_player(team_profile)
            team_label = self._team_name(team_profile.team)
            evidence = [
                f"{team_label}本场阵型为{team_profile.formation}，主教练为{team_profile.coach}",
                f"近段时间{team_label}出现{event_count}次射门、关键传球、角球、进球或危险进攻事件"
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
            if event.type not in self.ATTACK_EVENT_TYPES:
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

    def _detect_key_pass_creation(self, request: AnalyzeRequest, events: List[MatchEvent]) -> List[DataInsight]:
        """检测关键传球是否开始带来稳定机会来源。"""
        key_passes_by_team: dict[str, list[MatchEvent]] = defaultdict(list)
        for event in events:
            if event.type == "KEY_PASS" or event.data.get("is_key_pass") is True:
                key_passes_by_team[event.team].append(event)

        insights: List[DataInsight] = []
        for team, key_passes in key_passes_by_team.items():
            subject_team = self._subject_team(team, request)
            team_label = self._team_name(subject_team)
            evidence = [self._format_event_context(event, f"{team_label}完成关键传球") for event in key_passes]
            insights.append(DataInsight(
                code="KEY_PASS_CREATION",
                subjectTeam=subject_team,
                summary=f"{team_label}开始通过关键传球制造机会",
                evidence=evidence,
                strength=min(0.86, 0.54 + len(key_passes) * 0.13),
            ))
        return insights

    def _detect_turnover_risk(self, request: AnalyzeRequest, events: List[MatchEvent]) -> List[DataInsight]:
        """检测推进阶段的丢失球权风险。"""
        turnovers_by_team: dict[str, list[MatchEvent]] = defaultdict(list)
        for event in events:
            if event.type == "TURNOVER" or event.data.get("result") == "lost":
                turnovers_by_team[event.team].append(event)

        insights: List[DataInsight] = []
        for team, turnovers in turnovers_by_team.items():
            subject_team = self._subject_team(team, request)
            team_label = self._team_name(subject_team)
            evidence = [self._format_event_context(event, f"{team_label}丢失球权") for event in turnovers]
            insights.append(DataInsight(
                code="TURNOVER_RISK",
                subjectTeam=subject_team,
                summary=f"{team_label}推进阶段出现丢失球权风险",
                evidence=evidence,
                strength=min(0.82, 0.5 + len(turnovers) * 0.14),
            ))
        return insights

    def _detect_transition_threat(self, request: AnalyzeRequest, events: List[MatchEvent]) -> List[DataInsight]:
        """检测攻防转换阶段是否持续制造威胁。"""
        transition_events_by_team: dict[str, list[MatchEvent]] = defaultdict(list)
        for event in events:
            if event.type == "TRANSITION" or event.data.get("phase") == "transition" or event.data.get("transition") is True:
                transition_events_by_team[event.team].append(event)

        insights: List[DataInsight] = []
        for team, transition_events in transition_events_by_team.items():
            if len(transition_events) < 2:
                continue
            subject_team = self._subject_team(team, request)
            team_label = self._team_name(subject_team)
            start_minute = min(event.minute for event in transition_events)
            end_minute = max(event.minute for event in transition_events)
            evidence = [
                f"第{start_minute}到{end_minute}分钟，{team_label}出现{len(transition_events)}次攻防转换相关事件"
            ]
            evidence.extend(self._format_event_context(event, "转换事件") for event in transition_events[:3])
            insights.append(DataInsight(
                code="TRANSITION_THREAT",
                subjectTeam=subject_team,
                summary=f"{team_label}在攻防转换阶段的威胁上升",
                evidence=evidence,
                strength=min(0.9, 0.5 + len(transition_events) * 0.1),
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

    def _format_event_context(self, event: MatchEvent, prefix: str) -> str:
        """把扩展事件字段组织成中文证据，避免 Agent 只输出空泛判断。"""
        zone = str(event.data.get("zoneDisplay") or self._zone_name(str(event.data.get("zone", "unknown"))))
        direction = str(event.data.get("directionDisplay") or self._direction_name(str(event.data.get("direction", "unknown"))))
        phase = str(event.data.get("phaseDisplay") or self._phase_name(str(event.data.get("phase", "unknown"))))
        result = str(event.data.get("resultDisplay") or self._result_name(str(event.data.get("result", "unknown"))))
        return f"第{event.minute}分钟，{prefix}：区域={zone}，方向={direction}，阶段={phase}，结果={result}"

    def _zone_name(self, zone: str) -> str:
        """把事件里的英文区域值转换成中文展示。"""
        names = {
            "left": "左路",
            "right": "右路",
            "middle": "中路",
            "left_half_space": "左肋部",
            "right_half_space": "右肋部",
            "box": "禁区",
            "unknown": "未知区域",
        }
        return names.get(zone, zone)

    def _direction_name(self, direction: str) -> str:
        """把推进方向转换成中文展示。"""
        names = {
            "left": "左路",
            "right": "右路",
            "middle": "中路",
            "unknown": "未知方向",
        }
        return names.get(direction, direction)

    def _phase_name(self, phase: str) -> str:
        """把比赛阶段转换成中文展示。"""
        names = {
            "build_up": "组织推进",
            "transition": "攻防转换",
            "set_piece": "定位球",
            "defense": "防守阶段",
            "adjustment": "人员调整",
            "unknown": "未知阶段",
        }
        return names.get(phase, phase)

    def _result_name(self, result: str) -> str:
        """把事件结果转换成中文展示。"""
        names = {
            "success": "成功",
            "lost": "丢失球权",
            "threat": "形成威胁",
            "goal": "进球",
            "on_target": "射正",
            "off_target": "偏出",
            "corner_won": "赢得角球",
            "yellow_card": "黄牌",
            "substitution": "换人",
            "Saved": "被扑救",
            "Saved to Post": "扑救后击中门框",
            "Off T": "偏出球门",
            "Blocked": "被封堵",
            "Wayward": "严重偏出",
            "Post": "击中门框",
            "created_chance": "制造机会",
            "lost_possession": "丢失球权",
            "unknown": "未知结果",
        }
        return names.get(result, result)

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
            "Croatia": "克罗地亚",
            "Netherlands": "荷兰",
        }
        return names.get(team, team)
