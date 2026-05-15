<template>
  <section :class="['chart-panel', { 'soft-card': !embedded, 'chart-panel-embedded': embedded }]">
    <div class="section-title">战术数据图表</div>
    <div class="chart-grid">
      <div ref="shotsChartRef" class="chart-box"></div>
      <div ref="possessionChartRef" class="chart-box"></div>
      <div ref="eventChartRef" class="chart-box"></div>
    </div>
    <div v-if="events.length === 0" class="chart-hint">
      当前暂无实时事件，开始演练后图表会随比赛事件更新。
    </div>
  </section>
</template>

<script setup lang="ts">
import * as echarts from 'echarts'
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import type { ECharts } from 'echarts'
import type { MatchEvent, MatchState, TeamStats } from '../api/types'
import { eventTypeName, teamDisplayName } from '../utils/labels'

const props = defineProps<{
  state: MatchState
  events: MatchEvent[]
  embedded?: boolean
}>()

const shotsChartRef = ref<HTMLDivElement | null>(null)
const possessionChartRef = ref<HTMLDivElement | null>(null)
const eventChartRef = ref<HTMLDivElement | null>(null)

let shotsChart: ECharts | null = null
let possessionChart: ECharts | null = null
let eventChart: ECharts | null = null
let resizeObserver: ResizeObserver | null = null

const teams = computed(() => Object.entries(props.state.teams))

onMounted(async () => {
  await nextTick()
  initCharts()
  renderCharts()
  window.addEventListener('resize', resizeCharts)
  observeChartSize()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  resizeObserver?.disconnect()
  shotsChart?.dispose()
  possessionChart?.dispose()
  eventChart?.dispose()
})

watch(
  () => [props.state.currentMinute, props.events.length, JSON.stringify(props.state.teams)],
  () => renderCharts()
)

function initCharts() {
  if (shotsChartRef.value) shotsChart = echarts.init(shotsChartRef.value)
  if (possessionChartRef.value) possessionChart = echarts.init(possessionChartRef.value)
  if (eventChartRef.value) eventChart = echarts.init(eventChartRef.value)
}

function renderCharts() {
  renderShotsChart()
  renderPossessionChart()
  renderEventChart()
}

function renderShotsChart() {
  const names = teams.value.map(([team]) => teamDisplayName(team))
  const shots = teams.value.map(([, stats]) => stats.shots)
  const shotsOnTarget = teams.value.map(([, stats]) => stats.shotsOnTarget)

  shotsChart?.setOption({
    backgroundColor: 'transparent',
    title: chartTitle('射门对比'),
    tooltip: { trigger: 'axis' },
    legend: textLegend(['射门', '射正']),
    grid: { left: 36, right: 18, top: 58, bottom: 28 },
    xAxis: axis('category', names),
    yAxis: axis('value'),
    series: [
      { name: '射门', type: 'bar', data: shots, itemStyle: { color: '#f2c66d' } },
      { name: '射正', type: 'bar', data: shotsOnTarget, itemStyle: { color: '#7db8ff' } }
    ]
  })
}

function renderPossessionChart() {
  const data = teams.value.map(([team, stats]) => ({
    name: teamDisplayName(team),
    value: stats.possessionRate || 0
  }))

  possessionChart?.setOption({
    backgroundColor: 'transparent',
    title: chartTitle('控球率'),
    tooltip: { trigger: 'item', formatter: '{b}: {c}%' },
    legend: textLegend(data.map((item) => item.name), 'bottom'),
    series: [
      {
        name: '控球率',
        type: 'pie',
        radius: ['46%', '68%'],
        center: ['50%', '48%'],
        data,
        label: { color: '#dbeafe', formatter: '{b}\n{c}%' }
      }
    ]
  })
}

function renderEventChart() {
  const counter = new Map<string, number>()
  props.events.forEach((event) => {
    counter.set(event.type, (counter.get(event.type) ?? 0) + 1)
  })

  const data = [...counter.entries()]
    .map(([type, count]) => ({ name: eventTypeName(type), value: count }))
    .sort((first, second) => second.value - first.value)

  const colors = ['#f2c66d', '#7db8ff', '#8fd3c8', '#f08a5d', '#b48cff', '#f7a6b6', '#8cc8ff']

  eventChart?.setOption({
    backgroundColor: 'transparent',
    title: chartTitle('事件类型分布'),
    tooltip: { trigger: 'item' },
    color: colors,
    legend: {
      orient: 'vertical',
      right: 12,
      top: 'center',
      itemWidth: 10,
      itemHeight: 10,
      itemGap: 10,
      textStyle: { color: '#d4deee', fontSize: 12 }
    },
    series: [
      {
        name: '事件数量',
        type: 'pie',
        radius: ['38%', '66%'],
        center: ['32%', '54%'],
        avoidLabelOverlap: true,
        label: {
          color: '#eef3fb',
          formatter: '{c}',
          fontWeight: 700
        },
        labelLine: {
          length: 8,
          length2: 6
        },
        data
      }
    ]
  })
}

function chartTitle(text: string) {
  return {
    text,
    left: 8,
    top: 6,
    textStyle: { color: '#e7edf8', fontSize: 14, fontWeight: 700 }
  }
}

function textLegend(data: string[], bottom?: string) {
  return {
    data,
    top: bottom ? undefined : 28,
    bottom,
    textStyle: { color: '#9fb0c7' }
  }
}

function axis(type: 'category' | 'value', data?: string[]) {
  return {
    type,
    data,
    axisLine: { lineStyle: { color: '#334155' } },
    axisLabel: { color: '#9fb0c7' },
    splitLine: { lineStyle: { color: 'rgba(148, 163, 184, 0.12)' } }
  }
}

function resizeCharts() {
  shotsChart?.resize()
  possessionChart?.resize()
  eventChart?.resize()
}

function observeChartSize() {
  resizeObserver = new ResizeObserver(() => resizeCharts())
  ;[shotsChartRef.value, possessionChartRef.value, eventChartRef.value]
    .filter((item): item is HTMLDivElement => Boolean(item))
    .forEach((item) => resizeObserver?.observe(item))
}
</script>

<style scoped>
.chart-panel {
  padding: 18px;
}

.chart-panel-embedded {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding: 4px 4px 0;
}

.chart-grid {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(0, 0.92fr) minmax(0, 0.92fr) minmax(0, 1.16fr);
  gap: 12px;
}

.chart-box {
  height: 260px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 12px;
  background: rgba(11, 18, 31, 0.72);
}

.chart-panel-embedded .chart-box {
  height: auto;
  min-height: 0;
}

.chart-panel-embedded .section-title {
  display: none;
}

.chart-hint {
  margin-top: 12px;
  color: #9fb0c7;
  font-size: 13px;
}

@media (max-width: 1080px) {
  .chart-grid {
    grid-template-columns: 1fr;
  }
}
</style>
