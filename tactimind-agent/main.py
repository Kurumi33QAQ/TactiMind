from fastapi import FastAPI

from agents.data_agent import DataAgent
from agents.tactics_agent import TacticsAgent
from agents.verify_agent import VerifyAgent
from schemas.analysis_schema import AnalyzeRequest, AnalyzeResponse

app = FastAPI(title="TactiMind 战术分析 Agent 服务", version="0.1.0")

# 第一版使用规则 Agent 编排，暂时不接入付费大模型，保证项目 0 成本可运行。
data_agent = DataAgent()
tactics_agent = TacticsAgent()
verify_agent = VerifyAgent()


@app.get("/health")
def health() -> dict[str, str]:
    """健康检查接口，Java 后端可用它判断 Agent 服务是否可用。"""
    return {"status": "UP"}


@app.post("/analyze", response_model=AnalyzeResponse)
def analyze(request: AnalyzeRequest) -> AnalyzeResponse:
    """战术分析入口：数据分析 -> 战术生成 -> 防幻觉校验。"""
    minute = request.matchState.currentMinute
    data_insights = data_agent.analyze(request)
    for insight in data_insights:
        insight.minute = minute
    tactical_candidates = tactics_agent.generate(minute, data_insights)
    verified_analyses = verify_agent.verify(minute, tactical_candidates)
    return AnalyzeResponse(minute=minute, dataInsights=data_insights, analyses=verified_analyses)
