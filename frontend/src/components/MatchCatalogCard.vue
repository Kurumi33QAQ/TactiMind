<template>
  <article :class="['match-card', dataSourceClass(match), { selected }]">
    <div class="data-source-strip">
      <span>{{ dataSourceHeadline(match) }}</span>
      <strong>{{ dataSourceSubtitle(match) }}</strong>
    </div>

    <div class="card-top">
      <div class="match-date">{{ match.matchDate }}</div>
      <div class="tags">
        <span class="tag">{{ dataLevelName(match.dataLevel) }}</span>
        <span :class="['tag', 'source', dataSourceClass(match)]">{{ sourceTypeName(match.sourceType) }}</span>
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

    <div v-if="match.simulated" class="data-notice simulated-warning">
      模拟数据，仅用于战术演练，不代表真实比赛事实。
    </div>
    <div v-else-if="isOpenData(match)" class="data-notice open-data-notice">
      真实公开事件数据，来源 StatsBomb Open Data；阵容能力标签如无真实来源，仍按辅助资料展示。
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

function isOpenData(match: MatchCatalogItem): boolean {
  return match.sourceType === 'STATSBOMB_OPEN_DATA'
}

function dataSourceClass(match: MatchCatalogItem): string {
  if (match.simulated) return 'is-simulated'
  if (isOpenData(match)) return 'is-open-data'
  return 'is-manual-data'
}

function dataSourceHeadline(match: MatchCatalogItem): string {
  if (match.simulated) return 'AI 模拟演练'
  if (isOpenData(match)) return '真实公开事件'
  return '基础资料'
}

function dataSourceSubtitle(match: MatchCatalogItem): string {
  if (match.simulated) return '仅用于流程演示'
  if (isOpenData(match)) return 'StatsBomb Open Data'
  return '数据完整度有限'
}
</script>

<style scoped>
.match-card {
  display: grid;
  gap: 12px;
  position: relative;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 14px;
  background: rgba(15, 28, 49, 0.78);
  padding: 16px;
  overflow: hidden;
  transition: border-color 0.2s ease, transform 0.2s ease;
}

.match-card::before {
  content: '';
  position: absolute;
  inset: 0 auto 0 0;
  width: 4px;
  background: rgba(148, 163, 184, 0.5);
}

.match-card.is-open-data::before {
  background: linear-gradient(180deg, #8bd3ff, #67e8f9);
}

.match-card.is-simulated::before {
  background: linear-gradient(180deg, #f2c66d, #fb923c);
}

.match-card.selected {
  border-color: rgba(74, 222, 128, 0.7);
}

.match-card:hover {
  transform: translateY(-2px);
}

.data-source-strip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-radius: 10px;
  padding: 8px 10px;
  background: rgba(7, 17, 31, 0.48);
}

.data-source-strip span {
  font-weight: 900;
}

.data-source-strip strong {
  color: #9fb0c7;
  font-size: 12px;
}

.match-card.is-open-data .data-source-strip span {
  color: #bae6fd;
}

.match-card.is-simulated .data-source-strip span {
  color: #fde68a;
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

.tag.source.is-open-data {
  border-color: rgba(125, 211, 252, 0.36);
  color: #bae6fd;
}

.tag.source.is-simulated {
  border-color: rgba(242, 198, 109, 0.36);
  color: #fde7ae;
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
  color: #f2c66d;
  font-weight: 900;
}

.description {
  margin: 0;
  line-height: 1.6;
}

.data-notice {
  border-radius: 10px;
  padding: 10px;
  line-height: 1.5;
}

.simulated-warning {
  border: 1px solid rgba(251, 191, 36, 0.35);
  background: rgba(120, 53, 15, 0.26);
  color: #fde68a;
}

.open-data-notice {
  border: 1px solid rgba(125, 211, 252, 0.34);
  background: rgba(14, 116, 144, 0.18);
  color: #bae6fd;
}
</style>