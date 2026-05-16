export interface TeamStats {
  goals: number
  shots: number
  shotsOnTarget: number
  yellowCards: number
  corners: number
  dangerousAttacks: number
  possessionRate: number
}

export interface MatchState {
  matchId: string
  currentMinute: number
  running: boolean
  finished: boolean
  eventCursor: number
  teams: Record<string, TeamStats>
}

export interface MatchEvent {
  minute: number
  team: string
  type: string
  player?: string
  description: string
  data: Record<string, unknown>
}

export interface TacticalAnalysis {
  minute: number
  conclusion: string
  evidence: string[]
  confidence: number
  riskLevel: string
}

export interface DataInsight {
  minute: number
  code: string
  subjectTeam: string
  targetTeam?: string
  summary: string
  evidence: string[]
  strength: number
}

export interface SimulationStatus {
  running: boolean
  finished: boolean
  eventCursor: number
  totalEvents: number
  currentMinute: number
  speed: number
  finalMinute: number
}

export interface MatchReport {
  matchId: string
  currentState: MatchState
  events: MatchEvent[]
  analyses: TacticalAnalysis[]
  summary: string
}

export interface PersistenceStatus {
  enabled: boolean
  mode: 'mysql' | 'memory'
}

export interface WsMessage<T = unknown> {
  messageType: string
  payload: T
  timestamp: string
}

export interface MatchCatalogItem {
  id: number
  matchCode: string
  homeTeam: string
  awayTeam: string
  homeTeamLogo: string
  awayTeamLogo: string
  competition: string
  season: string
  matchDate: string
  sourceType: string
  dataLevel: string
  eventFilePath: string
  playable: boolean
  description: string
  capabilities: string[]
  simulated: boolean
}

export interface PlayerProfile {
  number: number
  name: string
  position: string
  role: string
  status: string
  pitchX: number
  pitchY: number
  abilityTags: string[]
}

export interface TeamTacticalProfile {
  team: string
  coach: string
  formation: string
  style: string
  pressingStyle: string
  buildUpFocus: string
  startingLineup: PlayerProfile[]
  substitutes: PlayerProfile[]
}

export interface MatchTacticalProfile {
  matchCode: string
  home: TeamTacticalProfile
  away: TeamTacticalProfile
  keyFactors: string[]
  dataNotes: string[]
}

export interface MatchSearchParams {
  date?: string
  team?: string
  competition?: string
  dataLevel?: string
}

export interface AnalysisTask {
  taskId: string
  status: string
  currentStep: string
  progress: number
  reportId: string
}

export interface AnalysisTaskHistoryItem extends AnalysisTask {
  matchId: string
  analysisType: string
  createdAt: string
}

export interface PageResult<T> {
  records: T[]
  page: number
  size: number
  total: number
}

export interface CreateAnalysisTaskRequest {
  matchId: string
  analysisType: string
}

export interface AgentTraceLog {
  stepName: string
  agentName: string
  toolName: string
  status: string
  inputSummary: string
  outputSummary: string
  costTimeMs: number
  errorMessage: string
  createdAt: string
}

export interface TacticalReport {
  reportId: string
  taskId: string
  matchId: string
  summary: string
  conclusions: TacticalAnalysis[]
  suggestions: string[]
  risks: string[]
  dataSources: string[]
  simulatedData: boolean
}
