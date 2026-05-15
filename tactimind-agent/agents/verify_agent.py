from typing import List

from schemas.analysis_schema import TacticalAnalysis, TacticalCandidate


class VerifyAgent:
    """防幻觉校验 Agent：没有数据证据的结论不允许输出。"""

    def verify(self, minute: int, candidates: List[TacticalCandidate]) -> List[TacticalAnalysis]:
        """校验候选结论，确保每条结论都绑定 evidence。"""
        verified: List[TacticalAnalysis] = []
        for candidate in candidates:
            if not candidate.evidence:
                continue
            confidence = candidate.confidence
            # 证据越充分，置信度可以小幅上调，但最高不超过 0.95。
            if len(candidate.evidence) >= 2:
                confidence = min(0.95, confidence + 0.05)
            verified.append(TacticalAnalysis(
                minute=candidate.minute,
                conclusion=candidate.conclusion,
                evidence=candidate.evidence,
                confidence=round(confidence, 2),
                riskLevel=candidate.riskLevel,
            ))

        if verified:
            return verified

        # 没有证据时明确拒绝下结论，这是本项目防幻觉设计的核心。
        return [TacticalAnalysis(
            minute=minute,
            conclusion="当前数据不足，无法得出明确战术结论。",
            evidence=[],
            confidence=0.3,
            riskLevel="LOW",
        )]
