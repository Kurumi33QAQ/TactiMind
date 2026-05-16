<template>
  <section :class="['chart-panel', { 'soft-card': !embedded, 'chart-panel-embedded': embedded }]">
    <div class="section-title">战术数据图表</div>
    <div class="chart-snapshot">
      <div>
        <span>当前分钟</span>
        <strong>第 {{ state.currentMinute }} 分钟</strong>
      </div>
      <div>
        <span>近 10 分钟事件</span>
        <strong>{{ recentEvents.length }} 个</strong>
      </div>
      <div>
        <span>近期更活跃</span>
        <strong>{{ activeTeamName }}</strong>
      </div>
      <div>
        <span>最新事件</span>
        <strong>{{ latestEventText }}</strong>
      </div>
    </div>
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
  homeTeamName?: string
  awayTeamName?: string
}>()

const shotsChartRef = ref<HTMLDivElement | null>(null)
const possessionChartRef = ref<HTMLDivElement | null>(null)
const eventChartRef = ref<HTMLDivElement | null>(null)

let shotsChart: ECharts | null = null
let possessionChart: ECharts | null = null
let eventChart: ECharts | null = null
let resizeObserver: ResizeObserver | null = null

const teams = computed(() => Object.entries(props.state.teams))
const recentStartMinute = computed(() => Math.max(0, props.state.currentMinute - 10))
const recentEvents = computed(() =>
  props.events.filter(
    (event) => event.minute >= recentStartMinute.value && event.minute <= props.state.currentMinute
  )
)
const latestEventText = computed(() => {
  const latestEvent = [...props.events].sort((first, second) => second.minute - first.minute)[0]
  if (!latestEvent) return '暂无'
  return `${latestEvent.minute}' ${displayTeamName(latestEvent.team)} ${eventTypeName(latestEvent.type)}`
})
const activeTeamName = computed(() => {
  if (recentEvents.value.length === 0) return '暂无'

  const counter = new Map<string, number>()
  recentEvents.value.forEach((event) => {
    counter.set(event.team, (counter.get(event.team) ?? 0) + 1)
  })

  const [team] = [...counter.entries()].sort((first, second) => second[1] - first[1])[0]
  return displayTeamName(team)
})

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

function displayTeamName(team: string) {
  if (team === 'Team A' && props.homeTeamName) {
    return teamDisplayName(props.homeTeamName)
  }
  if (team === 'Team B' && props.awayTeamName) {
    return teamDisplayName(props.awayTeamName)
  }
  return teamDisplayName(team)
}
function renderShotsChart() {
  const names = teams.value.map(([team]) => displayTeamName(team))
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
    name: displayTeamName(team),
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
  const minuteAxis = buildRecentMinuteAxis()
  const teamNames = teams.value.map(([team]) => team)
  const colors = ['#f2c66d', '#7db8ff', '#8fd3c8', '#f08a5d']

  eventChart?.setOption({
    backgroundColor: 'transparent',
    title: chartTitle('近 10 分钟事件趋势'),
    tooltip: { trigger: 'axis' },
    legend: textLegend(teamNames.map((team) => displayTeamName(team))),
    grid: { left: 36, right: 20, top: 58, bottom: 30 },
    xAxis: axis('category', minuteAxis.map((minute) => `${minute}'`)),
    yAxis: axis('value'),
    color: colors,
    series: [
      ...teamNames.map((team) => ({
        name: displayTeamName(team),
        type: 'line',
        smooth: true,
        symbolSize: 8,
        data: minuteAxis.map((minute) => countTeamEventsAtMinute(team, minute)),
        areaStyle: { opacity: 0.12 },
        markLine: currentMinuteMarkLine()
      }))
    ]
  })
}

function buildRecentMinuteAxis() {
  const start = recentStartMinute.value
  const end = Math.max(start, props.state.currentMinute)
  return Array.from({ length: end - start + 1 }, (_, index) => start + index)
}

function countTeamEventsAtMinute(team: string, minute: number) {
  return props.events.filter((event) => event.team === team && event.minute === minute).length
}

function currentMinuteMarkLine() {
  return {
    symbol: 'none',
    label: {
      formatter: '当前',
      color: '#f2c66d',
      fontSize: 11
    },
    lineStyle: {
      color: '#f2c66d',
      type: 'dashed',
      width: 1
    },
    data: [{ xAxis: `${props.state.currentMinute}'` }]
  }
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

.chart-snapshot {
  display: grid;
  grid-template-columns: 0.8fr 0.8fr 0.9fr 1.4fr;
  gap: 8px;
  margin-bottom: 10px;
}

.chart-snapshot div {
  min-width: 0;
  border: 1px solid rgba(148, 163, 184, 0.14);
  border-radius: 10px;
  background: rgba(11, 18, 31, 0.58);
  padding: 8px 10px;
}

.chart-snapshot span,
.chart-snapshot strong {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chart-snapshot span {
  color: #9fb0c7;
  font-size: 12px;
}

.chart-snapshot strong {
  margin-top: 3px;
  color: #f2c66d;
  font-size: 14px;
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

  .chart-snapshot {
    grid-template-columns: 1fr 1fr;
  }
}
</style>


