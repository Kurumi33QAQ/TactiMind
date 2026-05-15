<template>
  <div class="team-card">
    <div class="team-title">
      <span class="team-logo">{{ teamLogoText(teamName) }}</span>
          <span>{{ teamDisplayName(teamName) }}</span>
    </div>
    <div class="stats-grid">
      <span>射门</span><strong>{{ stats.shots }}</strong>
      <span>射正</span><strong>{{ stats.shotsOnTarget }}</strong>
      <span>角球</span><strong>{{ stats.corners }}</strong>
      <span>危险进攻</span><strong>{{ stats.dangerousAttacks }}</strong>
      <span>黄牌</span><strong>{{ stats.yellowCards }}</strong>
      <span>控球率</span><strong>{{ stats.possessionRate }}%</strong>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { TeamStats } from '../api/types'
import { teamDisplayName } from '../utils/labels'
import { teamLogoText } from '../utils/teamLogoMap'

defineProps<{
  teamName: string
  stats: TeamStats
}>()
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
}
</style>
