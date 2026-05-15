import type { MatchCatalogItem, MatchSearchParams } from './types'

function toQuery(params: MatchSearchParams): string {
  const query = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value) {
      query.set(key, value)
    }
  })
  const text = query.toString()
  return text ? `?${text}` : ''
}

async function requestJson<T>(url: string): Promise<T> {
  const response = await fetch(url)
  if (!response.ok) {
    throw new Error(`请求失败：${response.status}`)
  }
  return response.json() as Promise<T>
}

export const matchCatalogApi = {
  search: (params: MatchSearchParams) => requestJson<MatchCatalogItem[]>(`/api/matches/search${toQuery(params)}`),
  detail: (matchId: string | number) => requestJson<MatchCatalogItem>(`/api/matches/${matchId}`)
}
