import type {
  AgentTraceLog,
  AnalysisTask,
  AnalysisTaskHistoryItem,
  CreateAnalysisTaskRequest,
  PageResult,
  TacticalReport
} from './types'

async function requestJson<T>(url: string, init?: RequestInit): Promise<T> {
  const response = await fetch(url, init)
  if (!response.ok) {
    throw new Error(`请求失败：${response.status}`)
  }
  return response.json() as Promise<T>
}

export const analysisTaskApi = {
  createTask: (request: CreateAnalysisTaskRequest) =>
    requestJson<AnalysisTask>('/api/analysis/tasks', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request)
    }),
  listTasks: (params: { page: number; size: number }) =>
    requestJson<PageResult<AnalysisTaskHistoryItem>>(`/api/analysis/tasks?page=${params.page}&size=${params.size}`),
  deleteTask: async (taskId: string) => {
    const response = await fetch(`/api/analysis/tasks/${taskId}`, { method: 'DELETE' })
    if (!response.ok) throw new Error(`请求失败：${response.status}`)
  },
  deleteAllTasks: async () => {
    const response = await fetch('/api/analysis/tasks', { method: 'DELETE' })
    if (!response.ok) throw new Error(`请求失败：${response.status}`)
  },
  getTask: (taskId: string) => requestJson<AnalysisTask>(`/api/analysis/tasks/${taskId}`),
  getReport: (reportId: string) => requestJson<TacticalReport>(`/api/analysis/reports/${reportId}`),
  listTraces: (taskId: string) => requestJson<AgentTraceLog[]>(`/api/agent/traces/${taskId}`)
}
