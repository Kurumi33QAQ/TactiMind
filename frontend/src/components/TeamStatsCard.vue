<template>
  <div class="team-card">
    <div class="team-title">
      <span class="team-logo">{{ teamLogoText(teamName) }}</span>
      <span>{{ teamDisplayName(teamName) }}</span>
    </div>
    <div class="stats-grid">
      <span>射门</span><strong :class="{ 'stat-updated': changedStats.shots }">{{ stats.shots }}</strong>
      <span>射正</span><strong :class="{ 'stat-updated': changedStats.shotsOnTarget }">{{ stats.shotsOnTarget }}</strong>
      <span>角球</span><strong :class="{ 'stat-updated': changedStats.corners }">{{ stats.corners }}</strong>
      <span>危险进攻</span><strong :class="{ 'stat-updated': changedStats.dangerousAttacks }">{{ stats.dangerousAttacks }}</strong>
      <span>黄牌</span><strong :class="{ 'stat-updated': changedStats.yellowCards }">{{ stats.yellowCards }}</strong>
      <span>控球率</span><strong :class="{ 'stat-updated': changedStats.possessionRate }">{{ stats.possessionRate }}%</strong>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, reactive, watch } from 'vue'
import type { TeamStats } from '../api/types'
import { teamDisplayName } from '../utils/labels'
import { teamLogoText } from '../utils/teamLogoMap'

const props = defineProps<{
  teamName: string
  stats: TeamStats
}>()

type StatKey = 'shots' | 'shotsOnTarget' | 'corners' | 'dangerousAttacks' | 'yellowCards' | 'possessionRate'

const changedStats = reactive<Record<StatKey, boolean>>({
  shots: false,
  shotsOnTarget: false,
  corners: false,
  dangerousAttacks: false,
  yellowCards: false,
  possessionRate: false
})

const statKeys: StatKey[] = ['shots', 'shotsOnTarget', 'corners', 'dangerousAttacks', 'yellowCards', 'possessionRate']
const resetTimers = new Map<StatKey, number>()

watch(
  () => ({ ...props.stats }),
  (nextStats, previousStats) => {
    if (!previousStats) return

    statKeys.forEach((key) => {
      if (nextStats[key] !== previousStats[key]) {
        markStatChanged(key)
      }
    })
  }
)

onBeforeUnmount(() => {
  resetTimers.forEach((timer) => window.clearTimeout(timer))
})

function markStatChanged(key: StatKey) {
  changedStats[key] = false
  window.requestAnimationFrame(() => {
    changedStats[key] = true
  })

  const existingTimer = resetTimers.get(key)
  if (existingTimer) {
    window.clearTimeout(existingTimer)
  }

  resetTimers.set(
    key,
    window.setTimeout(() => {
      changedStats[key] = false
    }, 700)
  )
}
</script>

<style scoped>
.team-card {
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 12px;
  background: rgba(15, 28, 49, 0.76);
  padding: 14px;
}

.team-title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  font-weight: 800;
}

.stats-grid {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px 14px;
  color: #9fb0c7;
}

.stats-grid strong {
  color: #e7edf8;
  border-radius: 8px;
  padding: 1px 6px;
  transition: color 0.2s ease, background 0.2s ease, transform 0.2s ease;
}

.stats-grid strong.stat-updated {
  color: #101827;
  background: #f2c66d;
  animation: stat-pulse 0.7s ease;
}

@keyframes stat-pulse {
  0% {
    transform: scale(1);
    box-shadow: 0 0 0 rgba(242, 198, 109, 0);
  }

  35% {
    transform: scale(1.12);
    box-shadow: 0 0 18px rgba(242, 198, 109, 0.5);
  }

  100% {
    transform: scale(1);
    box-shadow: 0 0 0 rgba(242, 198, 109, 0);
  }
}
</style>
