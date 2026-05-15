import type {
  MatchEvent,
  MatchReport,
  MatchState,
  PersistenceStatus,
  SimulationStatus,
  TacticalAnalysis
} from './types'

async function requestJson<T>(url: string, init?: RequestInit): Promise<T> {
  const response = await fetch(url, init)
  if (!response.ok) {
    throw new Error(`请求失败：${response.status}`)
  }
  return response.json() as Promise<T>
}

export const legacyMatchApi = {
  start: () => requestJson<SimulationStatus>('/match/simulate/start', { method: 'POST' }),
  pause: () => requestJson<SimulationStatus>('/match/simulate/pause', { method: 'POST' }),
  reset: () => requestJson<SimulationStatus>('/match/simulate/reset', { method: 'POST' }),
  updateSpeed: (speed: number) =>
    requestJson<SimulationStatus>(`/match/simulate/speed?speed=${speed}`, { method: 'POST' }),
  seek: (minute: number) =>
    requestJson<SimulationStatus>(`/match/simulate/seek?minute=${minute}`, { method: 'POST' }),
  analyzeNow: () => requestJson<TacticalAnalysis[]>('/match/agent/analyze-now', { method: 'POST' }),
  getStatus: () => requestJson<SimulationStatus>('/match/simulate/status'),
  getEvents: () => requestJson<MatchEvent[]>('/match/events'),
  getState: () => requestJson<MatchState>('/match/state'),
  getAnalyses: () => requestJson<TacticalAnalysis[]>('/match/analysis'),
  getReport: () => requestJson<MatchReport>('/match/report'),
  getPersistenceStatus: () => requestJson<PersistenceStatus>('/match/persistence/status')
}
