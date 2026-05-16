<template>
  <article :class="['match-card', { selected }]">
    <div class="card-top">
      <div class="match-date">{{ match.matchDate }}</div>
      <div class="tags">
        <span class="tag">{{ dataLevelName(match.dataLevel) }}</span>
        <span class="tag source">{{ sourceTypeName(match.sourceType) }}</span>
        <span :class="['tag', match.eventFilePath ? 'event-ready' : 'event-missing']">
          {{ match.eventFilePath ? '可实时演练' : '暂无事件流' }}
        </span>
      </div>
    </div>

    <div class="teams">
      <div class="team-side">
        <span class="team-logo">{{ teamLogoText(match.homeTeam) }}</span>
        <strong>{{ teamDisplayName(match.homeTeam) }}</strong>
      </div>
      <span class="versus">vs</span>
      <div class="team-side away">
        <strong>{{ teamDisplayName(match.awayTeam) }}</strong>
        <span class="team-logo">{{ teamLogoText(match.awayTeam) }}</span>
      </div>
    </div>

    <div class="competition">{{ competitionDisplayName(match.competition) }} | {{ match.season }}</div>
    <p class="description">{{ match.description }}</p>

    <div v-if="match.simulated" class="simulated-warning">
      模拟数据，仅用于战术演练，不代表真实比赛事实。
    </div>

    <div class="capability-list">
      <span v-for="capability in match.capabilities" :key="capability">{{ capability }}</span>
    </div>

    <div class="card-actions">
      <el-button type="primary" :disabled="!match.playable" @click="$emit('select', match)">
        {{ match.playable ? '选择比赛' : '暂不可分析' }}
      </el-button>
    </div>
  </article>
</template>

<script setup lang="ts">
import type { MatchCatalogItem } from '../api/types'
import { competitionDisplayName, dataLevelName, sourceTypeName, teamDisplayName } from '../utils/labels'
import { teamLogoText } from '../utils/teamLogoMap'

defineProps<{
  match: MatchCatalogItem
  selected: boolean
}>()

defineEmits<{
  select: [match: MatchCatalogItem]
}>()
</script>

<style scoped>
.match-card {
  display: grid;
  gap: 12px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 14px;
  background: rgba(15, 28, 49, 0.78);
  padding: 16px;
  transition: border-color 0.2s ease, transform 0.2s ease;
}

.match-card.selected {
  border-color: rgba(74, 222, 128, 0.7);
}

.match-card:hover {
  transform: translateY(-2px);
}

.card-top,
.teams,
.card-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.match-date,
.competition,
.description {
  color: #9fb0c7;
}

.tags,
.capability-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.tag,
.capability-list span {
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 999px;
  background: rgba(7, 17, 31, 0.62);
  color: #c8d6e8;
  padding: 4px 8px;
  font-size: 12px;
}

.tag.source {
  color: #7dd3fc;
}

.tag.event-ready {
  border-color: rgba(74, 222, 128, 0.32);
  color: #bbf7d0;
}

.tag.event-missing {
  border-color: rgba(148, 163, 184, 0.18);
  color: #94a3b8;
}

.teams {
  font-size: 17px;
}

.team-side {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.away {
  text-align: right;
}

.versus {
  color: #4ade80;
  font-weight: 900;
}

.description {
  margin: 0;
  line-height: 1.6;
}

.simulated-warning {
  border: 1px solid rgba(251, 191, 36, 0.35);
  border-radius: 10px;
  background: rgba(120, 53, 15, 0.26);
  color: #fde68a;
  padding: 10px;
  line-height: 1.5;
}
</style>
