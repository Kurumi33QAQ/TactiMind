export function eventTypeName(type: string): string {
  const names: Record<string, string> = {
    MATCH_START: '比赛开始',
    GOAL: '进球',
    SHOT: '射门',
    SHOT_ON_TARGET: '射正',
    POSSESSION_CHANGE: '控球率变化',
    YELLOW_CARD: '黄牌',
    SUBSTITUTION: '换人',
    CORNER: '角球',
    DANGEROUS_ATTACK: '危险进攻',
    KEY_PASS: '关键传球',
    TACKLE: '抢断',
    TURNOVER: '丢失球权',
    TRANSITION: '攻防转换',
    MATCH_END: '比赛结束'
  }
  return names[type] ?? type
}

export function riskName(level: string): string {
  const names: Record<string, string> = {
    HIGH: '高风险',
    MEDIUM: '中风险',
    LOW: '低风险'
  }
  return names[level] ?? '未知风险'
}

export function dataLevelName(level: string): string {
  const names: Record<string, string> = {
    DEEP_EVENT: '深度演练',
    BASIC_STATS: '基础分析',
    SIMULATED_EVENT: '模拟演练',
    CATALOG_ONLY: '仅可查看'
  }
  return names[level] ?? level
}

export function sourceTypeName(type: string): string {
  const names: Record<string, string> = {
    STATSBOMB_OPEN_DATA: '公开事件数据',
    OPENFOOTBALL: '公开赛程数据',
    API_FOOTBALL: '足球数据接口',
    FOOTBALL_DATA_ORG: '足球数据接口',
    AI_SIMULATED: 'AI 模拟数据',
    MANUAL_BUILT: '手工构造数据'
  }
  return names[type] ?? type
}

export function teamDisplayName(team: string): string {
  const names: Record<string, string> = {
    'Team A': 'A队',
    'Team B': 'B队',
    Argentina: '阿根廷',
    France: '法国',
    'Manchester City': '曼城',
    Arsenal: '阿森纳',
    Spain: '西班牙',
    England: '英格兰',
    'Real Madrid': '皇家马德里',
    Barcelona: '巴塞罗那',
    'Bayern Munich': '拜仁慕尼黑',
    Dortmund: '多特蒙德',
    'Open Data Team A': '开放数据A队',
    'Open Data Team B': '开放数据B队'
  }
  return names[team] ?? team
}

export function competitionDisplayName(competition: string): string {
  const names: Record<string, string> = {
    'World Cup': '世界杯',
    'Premier League': '英超',
    Euro: '欧洲杯',
    'La Liga': '西甲',
    Bundesliga: '德甲',
    'Open Data Demo': '开放数据演示'
  }
  return names[competition] ?? competition
}

export function agentDisplayName(agentName: string): string {
  const names: Record<string, string> = {
    DataAgent: '数据分析 Agent',
    TacticsAgent: '战术推理 Agent',
    VerifyAgent: '证据校验 Agent',
    ReportAgent: '报告生成 Agent'
  }
  return names[agentName] ?? agentName
}

export function toolDisplayName(toolName: string): string {
  const names: Record<string, string> = {
    search_playable_matches: '搜索可演练比赛',
    get_match_profile: '读取比赛资料',
    load_match_events: '读取比赛事件流',
    calculate_match_stats: '计算比赛关键指标',
    retrieve_tactical_knowledge: '检索战术知识库',
    generate_tactical_report: '生成战术报告',
    verify_report_evidence: '校验报告证据',
    detect_repeated_pressure: '检测连续区域施压',
    detect_shot_pressure: '检测射门压力变化',
    detect_possession_gap: '检测控球率差距',
    merge_tactical_profile: '融合阵型阵容资料',
    detect_key_pass_creation: '检测关键传球机会',
    detect_turnover_risk: '检测丢失球权风险',
    detect_transition_threat: '检测攻防转换威胁',
    detect_realtime_trend: '检测实时比赛趋势'
  }
  return names[toolName] ?? toolName
}

