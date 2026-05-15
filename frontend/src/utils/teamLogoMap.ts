const teamShortNameMap: Record<string, string> = {
  'Team A': 'A',
  'Team B': 'B',
  Argentina: '阿',
  France: '法',
  'Manchester City': '城',
  Arsenal: '枪',
  Spain: '西',
  England: '英',
  'Real Madrid': '皇',
  Barcelona: '巴',
  'Bayern Munich': '拜',
  Dortmund: '多'
}

export function teamLogoText(teamName: string): string {
  const fallback = teamName.trim().slice(0, 1).toUpperCase()
  return teamShortNameMap[teamName] ?? (fallback || '队')
}
