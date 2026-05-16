from typing import List

from schemas.analysis_schema import DataInsight, TacticalCandidate


class TacticsAgent:
    """战术分析 Agent：把 DataAgent 的数据事实转成候选战术判断。"""

    def generate(self, minute: int, insights: List[DataInsight]) -> List[TacticalCandidate]:
        """根据不同类型的数据洞察生成战术结论。"""
        candidates: List[TacticalCandidate] = []
        for insight in insights:
            team_label = self._team_name(insight.subjectTeam)
            if insight.code == "REPEATED_ZONE_PRESSURE":
                candidates.append(TacticalCandidate(
                    minute=minute,
                    conclusion=f"{team_label}正在通过固定区域持续施压，对手该侧防守压力增大。",
                    evidence=insight.evidence,
                    confidence=self._confidence(insight.strength),
                    riskLevel=self._risk_level(insight.strength),
                ))
            elif insight.code == "SHOT_PRESSURE":
                candidates.append(TacticalCandidate(
                    minute=minute,
                    conclusion=f"{team_label}近期射门频率上升，进攻威胁正在增强。",
                    evidence=insight.evidence,
                    confidence=self._confidence(insight.strength),
                    riskLevel=self._risk_level(insight.strength),
                ))
            elif insight.code == "POSSESSION_GAP":
                candidates.append(TacticalCandidate(
                    minute=minute,
                    conclusion=f"{team_label}控球优势明显，比赛节奏更可能由该队主导。",
                    evidence=insight.evidence,
                    confidence=self._confidence(insight.strength),
                    riskLevel=self._risk_level(insight.strength),
                ))
            elif insight.code == "PROFILE_CONTEXT_SUPPORT":
                candidates.append(TacticalCandidate(
                    minute=minute,
                    conclusion=f"结合阵型、教练和关键球员特点，{team_label}当前趋势具备一定人员与战术结构支撑。",
                    evidence=insight.evidence,
                    confidence=self._confidence(insight.strength),
                    riskLevel=self._risk_level(insight.strength),
                ))
        return candidates

    def _confidence(self, strength: float) -> float:
        """根据数据强度计算置信度，第一版先使用规则评分。"""
        return round(min(0.95, max(0.5, strength)), 2)

    def _risk_level(self, strength: float) -> str:
        """根据数据强度给出风险等级，方便前端做醒目展示。"""
        if strength >= 0.8:
            return "HIGH"
        if strength >= 0.6:
            return "MEDIUM"
        return "LOW"

    def _team_name(self, team: str) -> str:
        """把内部球队名转换成中文展示名。"""
        names = {
            "Team A": "A队",
            "Team B": "B队",
            "Argentina": "阿根廷",
            "France": "法国",
        }
        return names.get(team, team)
