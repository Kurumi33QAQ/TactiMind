from typing import Any, Dict, List, Optional

from pydantic import BaseModel, Field


class MatchEvent(BaseModel):
    """单条比赛事件，对应 Java 后端推送和聚合时使用的 MatchEvent。"""

    minute: int
    team: str
    type: str
    player: Optional[str] = ""
    description: str = ""
    data: Dict[str, Any] = Field(default_factory=dict)


class TeamStats(BaseModel):
    """单支球队的实时技术统计。"""

    goals: int = 0
    shots: int = 0
    shotsOnTarget: int = 0
    yellowCards: int = 0
    corners: int = 0
    dangerousAttacks: int = 0
    possessionRate: int = 0


class MatchState(BaseModel):
    """当前比赛状态，是 Agent 进行判断的核心依据。"""

    matchId: str
    currentMinute: int = 0
    running: bool = False
    finished: bool = False
    eventCursor: int = 0
    teams: Dict[str, TeamStats] = Field(default_factory=dict)


class PlayerProfile(BaseModel):
    """球员战术资料，来自 Java 后端的比赛 Profile。"""

    number: int = 0
    name: str
    position: str = ""
    role: str = ""
    status: str = ""
    pitchX: int = 0
    pitchY: int = 0
    abilityTags: List[str] = Field(default_factory=list)


class TeamTacticalProfile(BaseModel):
    """球队在当前比赛中的阵型、教练和球员上下文。"""

    team: str
    coach: str = ""
    formation: str = ""
    style: str = ""
    pressingStyle: str = ""
    buildUpFocus: str = ""
    startingLineup: List[PlayerProfile] = Field(default_factory=list)
    substitutes: List[PlayerProfile] = Field(default_factory=list)


class MatchTacticalProfile(BaseModel):
    """比赛战术资料，是事件流之外的辅助分析依据。"""

    matchCode: str
    home: TeamTacticalProfile
    away: TeamTacticalProfile
    keyFactors: List[str] = Field(default_factory=list)
    dataNotes: List[str] = Field(default_factory=list)


class AnalyzeRequest(BaseModel):
    """Java 后端调用 Agent 服务时提交的请求体。"""

    matchState: MatchState
    recentEvents: List[MatchEvent] = Field(default_factory=list)
    tacticalProfile: Optional[MatchTacticalProfile] = None


class DataInsight(BaseModel):
    """DataAgent 输出的数据洞察，只描述事实和趋势，不直接下战术结论。"""

    minute: int = 0
    code: str
    subjectTeam: str
    targetTeam: Optional[str] = None
    summary: str
    evidence: List[str]
    strength: float = Field(ge=0, le=1)


class TacticalCandidate(BaseModel):
    """TacticsAgent 生成的候选战术结论，后续还需要 VerifyAgent 校验。"""

    minute: int
    conclusion: str
    evidence: List[str]
    confidence: float = Field(ge=0, le=1)
    riskLevel: str


class TacticalAnalysis(BaseModel):
    """最终可以返回给 Java 后端和前端展示的战术分析结果。"""

    minute: int
    conclusion: str
    evidence: List[str]
    confidence: float = Field(ge=0, le=1)
    riskLevel: str


class AnalyzeResponse(BaseModel):
    """Agent 服务的统一响应结构。"""

    minute: int
    dataInsights: List[DataInsight]
    analyses: List[TacticalAnalysis]
