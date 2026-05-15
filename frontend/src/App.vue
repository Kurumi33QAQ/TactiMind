<template>
  <div :class="['app-shell', { 'simulation-mode': currentView === 'simulation' }]">
    <header class="topbar">
      <div>
        <div class="brand">TactiMind</div>
        <div class="subtitle">足球战术分析 AI Agent 系统</div>
      </div>
      <div class="topbar-right">
        <div class="nav-actions">
          <el-button :type="currentView === 'catalog' || currentView === 'match-detail' ? 'primary' : 'default'" plain @click="showCatalogView">比赛库</el-button>
          <el-button :type="currentView === 'history' ? 'primary' : 'default'" plain @click="showHistoryView">历史任务</el-button>
        </div>
        <div class="connection">
          <span :class="['connection-dot', { connected: wsConnected }]"></span>
          {{ connectionText }}
        </div>
      </div>
    </header>

    <section v-if="currentView === 'catalog'" class="soft-card search-panel">
      <div class="search-head">
        <div>
          <div class="section-title">可演练比赛库</div>
          <div class="section-desc">按日期、球队或赛事搜索比赛，选择后启动 Agent 战术分析流程。</div>
        </div>
        <div class="search-actions">
          <el-button @click="resetSearchForm">重置条件</el-button>
          <el-button type="primary" @click="searchMatches">搜索比赛</el-button>
        </div>
      </div>

      <div class="search-form">
        <el-autocomplete
          v-model="searchForm.team"
          clearable
          :fetch-suggestions="queryTeamSuggestions"
          placeholder="选择或输入球队，例如 曼城、阿根廷"
          @select="searchMatches"
        />
        <el-autocomplete
          v-model="searchForm.competition"
          clearable
          :fetch-suggestions="queryCompetitionSuggestions"
          placeholder="选择或输入赛事，例如 世界杯、英超"
          @select="searchMatches"
        />
        <el-date-picker
          v-model="searchForm.date"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="比赛日期"
          clearable
        />
        <el-select v-model="searchForm.dataLevel" clearable placeholder="数据类型">
          <el-option label="深度演练" value="DEEP_EVENT" />
          <el-option label="基础分析" value="BASIC_STATS" />
          <el-option label="模拟演练" value="SIMULATED_EVENT" />
          <el-option label="仅可查看" value="CATALOG_ONLY" />
        </el-select>
      </div>

      <div v-if="selectedMatch" class="selected-match">
        <div>
          <strong>当前选择：</strong>
          {{ teamDisplayName(selectedMatch.homeTeam) }} vs {{ teamDisplayName(selectedMatch.awayTeam) }}
          <span>｜{{ competitionDisplayName(selectedMatch.competition) }} {{ selectedMatch.season }}</span>
        </div>
        <div v-if="selectedMatch.simulated" class="inline-warning">模拟数据，仅用于战术演练</div>
        <el-button type="success" :disabled="!selectedMatch.playable" @click="enterSelectedMatchDetail">
          进入分析页
        </el-button>
      </div>

      <div v-if="matchLoading" class="empty">正在读取可演练比赛库...</div>
      <div v-else-if="matches.length === 0" class="empty">没有找到符合条件的可演练比赛。</div>
      <div v-else class="match-card-grid">
        <MatchCatalogCard
          v-for="match in matches"
          :key="match.matchCode"
          :match="match"
          :selected="selectedMatch?.matchCode === match.matchCode"
          @select="selectMatch"
        />
      </div>
    </section>

    <section v-if="currentView === 'match-detail' && selectedMatch" class="soft-card match-detail-page">
      <div class="section-head">
        <div>
          <div class="section-title">比赛分析准备</div>
          <div class="section-desc">先确认比赛信息和可用数据，再由你手动启动 Agent 分析。</div>
        </div>
        <el-button @click="showCatalogView">返回比赛库</el-button>
      </div>

      <div class="match-detail-hero">
        <div class="detail-team">
          <span class="team-logo detail-logo">{{ teamLogoText(selectedMatch.homeTeam) }}</span>
          <strong>{{ teamDisplayName(selectedMatch.homeTeam) }}</strong>
        </div>
        <div class="detail-center">
          <div class="detail-versus">vs</div>
          <div>{{ competitionDisplayName(selectedMatch.competition) }}</div>
          <div>{{ selectedMatch.season }} | {{ selectedMatch.matchDate }}</div>
        </div>
        <div class="detail-team away">
          <strong>{{ teamDisplayName(selectedMatch.awayTeam) }}</strong>
          <span class="team-logo detail-logo">{{ teamLogoText(selectedMatch.awayTeam) }}</span>
        </div>
      </div>

      <div v-if="selectedMatch.simulated" class="detail-warning">
        模拟数据，仅用于战术演练，不代表真实比赛事实。
      </div>

      <div class="match-detail-grid">
        <article>
          <span>数据类型</span>
          <strong>{{ dataLevelName(selectedMatch.dataLevel) }}</strong>
        </article>
        <article>
          <span>数据来源</span>
          <strong>{{ sourceTypeName(selectedMatch.sourceType) }}</strong>
        </article>
        <article>
          <span>是否可分析</span>
          <strong>{{ selectedMatch.playable ? '可以分析' : '仅可查看' }}</strong>
        </article>
      </div>

      <div class="capability-panel">
        <div class="section-title">可用分析能力</div>
        <div class="capability-tags">
          <span v-for="capability in selectedMatch.capabilities" :key="capability">{{ capability }}</span>
        </div>
      </div>

      <div class="detail-actions">
        <el-button @click="showCatalogView">返回选择</el-button>
        <el-button type="success" :disabled="!selectedMatch.playable" @click="startSelectedMatchAnalysis">
          开始分析
        </el-button>
      </div>
    </section>

    <section v-if="currentView === 'history'" class="soft-card history-page">
      <div class="section-head">
        <div>
          <div class="section-title">历史分析任务</div>
          <div class="section-desc">分页查看已生成的 Agent 分析任务，可查看报告、删除单条记录或清空历史。</div>
        </div>
        <div class="history-toolbar">
          <el-button @click="showCatalogView">返回比赛库</el-button>
          <el-button @click="loadHistoryTasks">刷新列表</el-button>
          <el-button type="danger" plain :disabled="historyTasks.length === 0" @click="deleteAllHistoryTasks">一键删除</el-button>
        </div>
      </div>

      <div v-if="historyTasks.length === 0" class="empty">暂无历史分析任务，回到比赛库选择比赛并启动 Agent 分析即可生成记录。</div>
      <div v-else class="history-list">
        <article v-for="task in historyTasks" :key="task.taskId" class="history-card">
          <div>
            <div class="history-title">{{ matchTitle(task.matchId) }}</div>
            <div class="history-meta">
              状态：{{ taskStatusName(task.status) }} | 进度：{{ task.progress }}% | 类型：{{ task.analysisType }} | {{ formatTime(task.createdAt) }}
            </div>
            <div class="history-id">任务编号：{{ task.taskId }}</div>
          </div>
          <div class="history-card-actions">
            <el-button type="primary" plain @click="openHistoryTask(task)">查看报告</el-button>
            <el-button type="danger" plain @click="deleteHistoryTask(task)">删除</el-button>
          </div>
        </article>
      </div>

      <div class="history-pagination">
        <el-pagination
          v-model:current-page="historyPage"
          v-model:page-size="historySize"
          background
          layout="total, sizes, prev, pager, next, jumper"
          :page-sizes="[5, 8, 10, 20]"
          :total="historyTotal"
          @size-change="loadHistoryTasks"
          @current-change="loadHistoryTasks"
        />
      </div>
    </section>

    <section v-if="currentView === 'simulation'" class="soft-card simulation-title-panel">
      <div class="simulation-title">
        <div>
          <div class="section-title">
            {{ selectedMatch ? `${teamDisplayName(selectedMatch.homeTeam)} vs ${teamDisplayName(selectedMatch.awayTeam)}` : '比赛模拟演练' }}
          </div>
          <div class="section-desc">
            {{ selectedMatch ? `${competitionDisplayName(selectedMatch.competition)} ${selectedMatch.season} | ${selectedMatch.matchDate}` : '当前比赛演练' }}
          </div>
          <div v-if="selectedMatch?.simulated" class="inline-warning">该比赛为模拟数据，仅用于战术演练，不代表真实比赛事实。</div>
        </div>
        <div class="simulation-actions">
          <el-button @click="backToCatalog">{{ backButtonText }}</el-button>
          <el-button type="primary" plain @click="loadSnapshot">刷新看板</el-button>
        </div>
      </div>
    </section>

    <main v-if="currentView === 'simulation'" class="dashboard-grid simulation-grid">
      <section class="soft-card control-panel">
        <div class="section-title">实时演练控制台</div>
        <div class="actions">
          <el-button type="success" @click="runAction('start')">开始演练</el-button>
          <el-button @click="runAction('pause')">暂停</el-button>
          <el-button type="danger" plain @click="runAction('reset')">重置</el-button>
          <el-button @click="loadSnapshot">刷新状态</el-button>
          <el-button type="primary" plain @click="runAction('analyze')">立即分析</el-button>
        </div>

        <div class="match-status">
          第 {{ state.currentMinute }} 分钟
          <span>{{ state.running ? '模拟中' : '已暂停' }}</span>
          <span v-if="state.finished">已结束</span>
        </div>

        <div class="scoreboard">
          <TeamStatsCard :team-name="teamNames[0]" :stats="homeStats" />
          <div class="score">{{ homeStats.goals }} - {{ awayStats.goals }}</div>
          <TeamStatsCard :team-name="teamNames[1]" :stats="awayStats" />
        </div>
      </section>

      <section class="soft-card analysis-panel">
        <div class="section-title">Agent 战术分析</div>
        <div v-if="analyses.length === 0" class="empty">等待 Agent 基于比赛数据生成战术分析...</div>
        <div v-else class="analysis-list">
          <article
            v-for="analysis in orderedAnalyses"
            :key="`${analysis.minute}-${analysis.conclusion}`"
            :class="['analysis-card', analysisLevelClass(analysis), teamSideClass(analysisTeamName(analysis))]"
          >
            <div class="analysis-meta">
              <span :class="['analysis-badge', analysisLevelClass(analysis)]">{{ analysisLevelName(analysis) }}</span>
              <span :class="['team-side-badge', teamSideClass(analysisTeamName(analysis))]">{{ analysisTeamName(analysis) }}</span>
              第 {{ analysis.minute }} 分钟 | {{ riskName(analysis.riskLevel) }} | 置信度 {{ confidenceText(analysis.confidence) }}
            </div>
            <div class="analysis-conclusion">{{ analysis.conclusion }}</div>
            <ul class="evidence-list">
              <li v-for="item in analysis.evidence" :key="item">{{ item }}</li>
              <li v-if="analysis.evidence.length === 0">当前数据不足，无法得出确定性结论。</li>
            </ul>
          </article>
        </div>
      </section>

      <section class="soft-card detail-panel">
        <el-tabs v-model="activeDetailTab" class="detail-tabs">
          <el-tab-pane label="战术图表" name="charts">
            <TacticalCharts :state="state" :events="events" embedded />
          </el-tab-pane>
          <el-tab-pane label="Agent 执行链路" name="trace">
            <div v-if="!analysisTask" class="empty">启动 Agent 分析后，将展示每一步工具调用过程。</div>
            <div v-else>
              <div class="task-progress">
                <span>{{ analysisTask.currentStep }}</span>
                <strong>{{ analysisTask.progress }}%</strong>
              </div>
              <el-progress :percentage="analysisTask.progress" :stroke-width="10" />
              <div class="trace-list">
                <article v-for="trace in agentTraces" :key="`${trace.stepName}-${trace.toolName}`" class="trace-card">
                  <div class="trace-title">{{ trace.stepName }} | {{ trace.status }}</div>
                  <div class="trace-meta">
                    Agent：{{ trace.agentName }}（{{ agentDisplayName(trace.agentName) }}）
                  </div>
                  <div class="trace-meta">
                    工具：{{ trace.toolName }}（{{ toolDisplayName(trace.toolName) }}）| 耗时：{{ trace.costTimeMs }}ms
                  </div>
                  <div class="trace-output">{{ trace.outputSummary }}</div>
                </article>
              </div>
            </div>
          </el-tab-pane>
          <el-tab-pane label="结构化报告" name="report">
            <div v-if="!agentReport" class="empty">暂无结构化报告，请先从比赛库启动 Agent 分析。</div>
            <div v-else class="structured-report">
              <div v-if="agentReport.simulatedData" class="inline-warning">
                该报告基于模拟数据，仅用于战术演练，不代表真实比赛事实。
              </div>
              <p class="report-summary">{{ agentReport.summary }}</p>
              <div class="report-block">
                <strong>关键结论</strong>
                <article
                  v-for="item in agentReport.conclusions"
                  :key="item.conclusion"
                  :class="['analysis-card', analysisLevelClass(item), teamSideClass(analysisTeamName(item))]"
                >
                  <div class="analysis-meta">
                    <span :class="['analysis-badge', analysisLevelClass(item)]">{{ analysisLevelName(item) }}</span>
                    <span :class="['team-side-badge', teamSideClass(analysisTeamName(item))]">{{ analysisTeamName(item) }}</span>
                    {{ riskName(item.riskLevel) }} | 置信度 {{ confidenceText(item.confidence) }}
                  </div>
                  <div class="analysis-conclusion">{{ item.conclusion }}</div>
                  <ul class="evidence-list">
                    <li v-for="evidence in item.evidence" :key="evidence">{{ evidence }}</li>
                  </ul>
                </article>
              </div>
              <div class="report-block">
                <strong>战术建议</strong>
                <ul class="evidence-list">
                  <li v-for="suggestion in agentReport.suggestions" :key="suggestion">{{ suggestion }}</li>
                </ul>
              </div>
              <div class="report-block">
                <strong>风险提示</strong>
                <ul class="evidence-list">
                  <li v-for="risk in agentReport.risks" :key="risk">{{ risk }}</li>
                </ul>
              </div>
            </div>
          </el-tab-pane>
          <el-tab-pane label="实时事件流" name="events">
            <div v-if="events.length === 0" class="empty">等待 WebSocket 推送比赛事件...</div>
            <div v-else class="event-list">
              <article
                v-for="event in orderedEvents"
                :key="`${event.minute}-${event.type}-${event.description}`"
                :class="['event-card', teamSideClass(event.team)]"
              >
                <div class="event-minute">{{ event.minute }}'</div>
                <div>
                  <div class="event-meta">
                    <span :class="['team-side-badge', teamSideClass(event.team)]">{{ teamDisplayName(event.team || '比赛') }}</span>
                    <span>{{ eventTypeName(event.type) }}</span>
                    <span>{{ zoneText(event.data.zone) }}</span>
                  </div>
                  <div class="event-desc">{{ event.description }}</div>
                </div>
              </article>
            </div>
          </el-tab-pane>
          <el-tab-pane label="赛后摘要" name="summary">
            <div class="section-head">
              <div class="section-title">赛后战术报告</div>
              <el-button size="small" @click="loadReport">刷新报告</el-button>
            </div>
            <div class="report-summary">{{ report?.summary ?? '比赛结束后可刷新查看结构化报告。' }}</div>
            <div class="report-metrics">
              <div>
                <strong>{{ report?.events.length ?? 0 }}</strong>
                <span>比赛事件</span>
              </div>
              <div>
                <strong>{{ report?.analyses.length ?? 0 }}</strong>
                <span>Agent 分析</span>
              </div>
              <div>
                <strong>{{ persistenceStatus.enabled ? 'MySQL' : '内存' }}</strong>
                <span>报告来源</span>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { analysisTaskApi } from './api/analysisTaskApi'
import { legacyMatchApi } from './api/legacyMatchApi'
import { matchCatalogApi } from './api/matchCatalogApi'
import type {
  MatchCatalogItem,
  MatchEvent,
  MatchReport,
  MatchSearchParams,
  MatchState,
  PersistenceStatus,
  TacticalAnalysis,
  AgentTraceLog,
  AnalysisTask,
  AnalysisTaskHistoryItem,
  TacticalReport,
  TeamStats,
  WsMessage
} from './api/types'
import {
  agentDisplayName,
  competitionDisplayName,
  dataLevelName,
  eventTypeName,
  riskName,
  sourceTypeName,
  teamDisplayName,
  toolDisplayName
} from './utils/labels'
import MatchCatalogCard from './components/MatchCatalogCard.vue'
import TacticalCharts from './components/TacticalCharts.vue'
import TeamStatsCard from './components/TeamStatsCard.vue'
import { teamLogoText } from './utils/teamLogoMap'

const emptyStats: TeamStats = {
  goals: 0,
  shots: 0,
  shotsOnTarget: 0,
  yellowCards: 0,
  corners: 0,
  dangerousAttacks: 0,
  possessionRate: 0
}

const state = reactive<MatchState>({
  matchId: 'demo-match-001',
  currentMinute: 0,
  running: false,
  finished: false,
  eventCursor: 0,
  teams: {
    'Team A': { ...emptyStats },
    'Team B': { ...emptyStats }
  }
})

const events = ref<MatchEvent[]>([])
const analyses = ref<TacticalAnalysis[]>([])
const report = ref<MatchReport | null>(null)
const analysisTask = ref<AnalysisTask | null>(null)
const agentTraces = ref<AgentTraceLog[]>([])
const agentReport = ref<TacticalReport | null>(null)
const historyTasks = ref<AnalysisTaskHistoryItem[]>([])
const catalogOptions = ref<MatchCatalogItem[]>([])
const matches = ref<MatchCatalogItem[]>([])
const selectedMatch = ref<MatchCatalogItem | null>(null)
const matchLoading = ref(false)
const currentView = ref<'catalog' | 'match-detail' | 'simulation' | 'history'>('catalog')
const previousListView = ref<'catalog' | 'history'>('catalog')
const activeDetailTab = ref('charts')
const historyPage = ref(1)
const historySize = ref(8)
const historyTotal = ref(0)
const searchForm = reactive<MatchSearchParams>({
  team: '',
  competition: '',
  date: '',
  dataLevel: ''
})
const persistenceStatus = reactive<PersistenceStatus>({ enabled: false, mode: 'memory' })
const wsConnected = ref(false)
const connectionText = ref('未连接')
let socket: WebSocket | null = null

const teamNames = computed(() => {
  const names = Object.keys(state.teams)
  return names.length >= 2 ? names : ['Team A', 'Team B']
})

const homeStats = computed(() => state.teams[teamNames.value[0]] ?? emptyStats)
const awayStats = computed(() => state.teams[teamNames.value[1]] ?? emptyStats)
const orderedEvents = computed(() => [...events.value].reverse())
const orderedAnalyses = computed(() => [...analyses.value].reverse())
const backButtonText = computed(() => (previousListView.value === 'history' ? '返回历史任务' : '返回比赛库'))

onMounted(() => {
  connectWebSocket()
  loadCatalogOptions()
  searchMatches()
  loadHistoryTasks()
  loadSnapshot()
})

onUnmounted(() => {
  socket?.close()
})

async function runAction(action: 'start' | 'pause' | 'reset' | 'analyze') {
  try {
    if (action === 'start') await legacyMatchApi.start()
    if (action === 'pause') await legacyMatchApi.pause()
    if (action === 'reset') {
      await legacyMatchApi.reset()
      events.value = []
      analyses.value = []
      report.value = null
    }
    if (action === 'analyze') {
      const result = await legacyMatchApi.analyzeNow()
      analyses.value.push(...result)
    }
    await loadSnapshot()
  } catch (error) {
    ElMessage.error(userError(error))
  }
}

async function searchMatches() {
  matchLoading.value = true
  try {
    matches.value = await matchCatalogApi.search(searchForm)
    if (!selectedMatch.value && matches.value.length > 0) {
      selectedMatch.value = matches.value[0]
    }
  } catch (error) {
    ElMessage.error(userError(error))
  } finally {
    matchLoading.value = false
  }
}

async function loadCatalogOptions() {
  try {
    catalogOptions.value = await matchCatalogApi.search({})
  } catch (error) {
    ElMessage.error(userError(error))
  }
}

async function loadHistoryTasks() {
  try {
    const result = await analysisTaskApi.listTasks({
      page: historyPage.value,
      size: historySize.value
    })
    historyTasks.value = result.records
    historyPage.value = result.page
    historySize.value = result.size
    historyTotal.value = result.total
    if (historyTasks.value.length === 0 && historyPage.value > 1) {
      historyPage.value -= 1
      await loadHistoryTasks()
    }
  } catch (error) {
    ElMessage.error(userError(error))
  }
}

async function resetSearchForm() {
  searchForm.team = ''
  searchForm.competition = ''
  searchForm.date = ''
  searchForm.dataLevel = ''
  selectedMatch.value = null
  await searchMatches()
}

type Suggestion = { value: string }

function queryTeamSuggestions(query: string, callback: (items: Suggestion[]) => void) {
  const names = new Set<string>()
  catalogOptions.value.forEach((match) => {
    names.add(teamDisplayName(match.homeTeam))
    names.add(teamDisplayName(match.awayTeam))
  })
  callback(filterSuggestions([...names], query))
}

function queryCompetitionSuggestions(query: string, callback: (items: Suggestion[]) => void) {
  const names = new Set<string>()
  catalogOptions.value.forEach((match) => names.add(competitionDisplayName(match.competition)))
  callback(filterSuggestions([...names], query))
}

function filterSuggestions(values: string[], query: string): Suggestion[] {
  const keyword = query.trim()
  return values
    .filter((value) => !keyword || value.includes(keyword))
    .sort((first, second) => first.localeCompare(second, 'zh-Hans-CN'))
    .map((value) => ({ value }))
}

function selectMatch(match: MatchCatalogItem) {
  selectedMatch.value = match
}

function enterSelectedMatchDetail() {
  if (!selectedMatch.value) {
    ElMessage.warning('请先选择一场可演练比赛。')
    return
  }
  if (!selectedMatch.value.playable) {
    ElMessage.warning('该比赛当前仅可查看，暂不支持启动 Agent 分析。')
    return
  }
  currentView.value = 'match-detail'
}

async function startSelectedMatchAnalysis() {
  if (!selectedMatch.value) {
    ElMessage.warning('请先选择一场可演练比赛。')
    return
  }
  if (!selectedMatch.value.playable) {
    ElMessage.warning('该比赛当前仅可查看，暂不支持启动 Agent 分析。')
    return
  }
  if (selectedMatch.value.matchCode !== 'world-cup-2022-argentina-france') {
    ElMessage.info('当前实时演练引擎先接入 demo 事件流，后续会按比赛编号加载不同事件文件。')
  }
  await createAnalysisTask(selectedMatch.value)
  await runAction('reset')
  await runAction('start')
  previousListView.value = 'catalog'
  currentView.value = 'simulation'
}

async function backToCatalog() {
  currentView.value = previousListView.value
  if (previousListView.value === 'history') {
    await loadHistoryTasks()
  }
}

function showCatalogView() {
  currentView.value = 'catalog'
}

async function showHistoryView() {
  currentView.value = 'history'
  await loadHistoryTasks()
}

async function createAnalysisTask(match: MatchCatalogItem) {
  try {
    const task = await analysisTaskApi.createTask({
      matchId: match.matchCode,
      analysisType: 'TACTICAL_REPORT'
    })
    analysisTask.value = task
    agentTraces.value = await analysisTaskApi.listTraces(task.taskId)
    agentReport.value = await analysisTaskApi.getReport(task.reportId)
    await loadHistoryTasks()
  } catch (error) {
    ElMessage.error(userError(error))
  }
}

async function openHistoryTask(task: AnalysisTaskHistoryItem) {
  try {
    analysisTask.value = await analysisTaskApi.getTask(task.taskId)
    agentTraces.value = await analysisTaskApi.listTraces(task.taskId)
    agentReport.value = await analysisTaskApi.getReport(task.reportId)
    const detail = await matchCatalogApi.detail(task.matchId)
    selectedMatch.value = detail
    previousListView.value = 'history'
    currentView.value = 'simulation'
  } catch (error) {
    ElMessage.error(userError(error))
  }
}

async function deleteHistoryTask(task: AnalysisTaskHistoryItem) {
  try {
    await ElMessageBox.confirm(
      `确定删除「${matchTitle(task.matchId)}」这条历史分析任务吗？`,
      '删除确认',
      { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' }
    )
    await analysisTaskApi.deleteTask(task.taskId)
    ElMessage.success('已删除该条历史任务。')
    await loadHistoryTasks()
  } catch (error) {
    if (isDialogCancel(error)) return
    ElMessage.error(userError(error))
  }
}

async function deleteAllHistoryTasks() {
  try {
    await ElMessageBox.confirm(
      '确定清空全部历史分析任务吗？该操作会同时删除任务、报告和 Agent 执行链路记录。',
      '一键删除确认',
      { confirmButtonText: '确认清空', cancelButtonText: '取消', type: 'warning' }
    )
    await analysisTaskApi.deleteAllTasks()
    historyPage.value = 1
    historyTasks.value = []
    historyTotal.value = 0
    ElMessage.success('已清空历史分析任务。')
  } catch (error) {
    if (isDialogCancel(error)) return
    ElMessage.error(userError(error))
  }
}

async function loadSnapshot() {
  try {
    const [latestState, latestAnalyses, latestPersistence] = await Promise.all([
      legacyMatchApi.getState(),
      legacyMatchApi.getAnalyses(),
      legacyMatchApi.getPersistenceStatus()
    ])
    Object.assign(state, latestState)
    analyses.value = latestAnalyses
    Object.assign(persistenceStatus, latestPersistence)
  } catch (error) {
    ElMessage.error(userError(error))
  }
}

async function loadReport() {
  try {
    report.value = await legacyMatchApi.getReport()
  } catch (error) {
    ElMessage.error(userError(error))
  }
}

function connectWebSocket() {
  const protocol = location.protocol === 'https:' ? 'wss' : 'ws'
  socket = new WebSocket(`${protocol}://${location.host}/ws/match`)

  socket.onopen = () => {
    wsConnected.value = true
    connectionText.value = '已连接'
  }
  socket.onclose = () => {
    wsConnected.value = false
    connectionText.value = '连接断开，正在尝试重连'
    window.setTimeout(connectWebSocket, 2000)
  }
  socket.onerror = () => {
    wsConnected.value = false
    connectionText.value = '连接异常'
  }
  socket.onmessage = (event) => {
    handleWsMessage(JSON.parse(event.data) as WsMessage)
  }
}

function handleWsMessage(message: WsMessage) {
  if (message.messageType === 'MATCH_EVENT') {
    events.value.push(message.payload as MatchEvent)
    return
  }
  if (message.messageType === 'MATCH_STATE') {
    Object.assign(state, message.payload as MatchState)
    return
  }
  if (message.messageType === 'TACTICAL_ANALYSIS') {
    analyses.value.push(message.payload as TacticalAnalysis)
    return
  }
  if (message.messageType === 'SIMULATION_STATUS') {
    const status = message.payload as { currentMinute: number; running: boolean; finished: boolean; eventCursor: number }
    state.currentMinute = status.currentMinute
    state.running = status.running
    state.finished = status.finished
    state.eventCursor = status.eventCursor
  }
  if (message.messageType === 'SIMULATION_ERROR') {
    ElMessage.error('比赛模拟异常，请查看后端日志。')
  }
}

function confidenceText(value: number) {
  return `${Math.round(value * 100)}%`
}

function analysisLevelClass(analysis: { evidence: string[]; confidence: number; riskLevel: string }) {
  if (analysis.evidence.length === 0 || analysis.confidence < 0.5) {
    return 'analysis-low'
  }
  if (analysis.confidence >= 0.75 || analysis.riskLevel === 'HIGH') {
    return 'analysis-key'
  }
  return 'analysis-observe'
}

function analysisLevelName(analysis: { evidence: string[]; confidence: number; riskLevel: string }) {
  const level = analysisLevelClass(analysis)
  const names: Record<string, string> = {
    'analysis-key': '重点结论',
    'analysis-observe': '观察提示',
    'analysis-low': '数据不足'
  }
  return names[level]
}

function teamSideClass(team?: string) {
  const displayName = teamDisplayName(team || '')
  if (displayName === teamDisplayName(teamNames.value[0])) {
    return 'team-home'
  }
  if (displayName === teamDisplayName(teamNames.value[1])) {
    return 'team-away'
  }
  return 'team-neutral'
}

function analysisTeamName(analysis: { conclusion: string; evidence: string[] }) {
  const text = `${analysis.conclusion} ${analysis.evidence.join(' ')}`
  const homeName = teamDisplayName(teamNames.value[0])
  const awayName = teamDisplayName(teamNames.value[1])
  if (text.includes(homeName) || text.includes(teamNames.value[0])) {
    return homeName
  }
  if (text.includes(awayName) || text.includes(teamNames.value[1])) {
    return awayName
  }
  return '双方'
}

function zoneText(zone: unknown) {
  const names: Record<string, string> = {
    left: '左路',
    right: '右路',
    middle: '中路',
    unknown: '未知区域'
  }
  return names[String(zone ?? 'unknown')] ?? String(zone)
}

function userError(error: unknown) {
  console.error(error)
  return '请求失败，请确认后端服务和 Agent 服务是否正常运行。'
}

function isDialogCancel(error: unknown) {
  return error === 'cancel' || error === 'close'
}

function matchTitle(matchId: string) {
  const match = catalogOptions.value.find((item) => item.matchCode === matchId || String(item.id) === matchId)
  if (!match) {
    return matchId
  }
  return `${teamDisplayName(match.homeTeam)} vs ${teamDisplayName(match.awayTeam)}`
}

function taskStatusName(status: string) {
  const names: Record<string, string> = {
    PENDING: '等待中',
    RUNNING: '分析中',
    COMPLETED: '已完成',
    FAILED: '失败'
  }
  return names[status] ?? status
}

function formatTime(value: string) {
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}
</script>

<style scoped>
.app-shell {
  width: min(1440px, 100%);
  margin: 0 auto;
  padding: 22px;
}

.simulation-mode {
  width: min(1680px, 100%);
  height: 100vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.topbar,
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.topbar {
  margin-bottom: 18px;
}

.simulation-mode .topbar {
  flex: 0 0 auto;
  margin-bottom: 10px;
}

.search-panel {
  margin-bottom: 16px;
  padding: 18px;
}

.match-detail-page {
  display: grid;
  gap: 18px;
  padding: 18px;
}

.match-detail-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 220px minmax(0, 1fr);
  align-items: center;
  gap: 16px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 14px;
  background: rgba(15, 28, 49, 0.7);
  padding: 20px;
}

.detail-team {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 22px;
}

.detail-team.away {
  justify-content: flex-end;
}

.detail-logo {
  width: 48px;
  height: 48px;
}

.detail-center {
  display: grid;
  gap: 6px;
  color: #9fb0c7;
  text-align: center;
}

.detail-versus {
  color: #4ade80;
  font-size: 24px;
  font-weight: 900;
}

.detail-warning {
  border: 1px solid rgba(251, 191, 36, 0.35);
  border-radius: 12px;
  background: rgba(120, 53, 15, 0.26);
  color: #fde68a;
  padding: 12px;
}

.match-detail-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.match-detail-grid article {
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 12px;
  background: rgba(15, 28, 49, 0.72);
  padding: 14px;
}

.match-detail-grid span,
.match-detail-grid strong {
  display: block;
}

.match-detail-grid span {
  color: #9fb0c7;
}

.match-detail-grid strong {
  margin-top: 6px;
  font-size: 18px;
}

.capability-panel {
  display: grid;
  gap: 10px;
}

.capability-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.capability-tags span {
  border: 1px solid rgba(96, 165, 250, 0.28);
  border-radius: 999px;
  background: rgba(30, 64, 175, 0.18);
  color: #bfdbfe;
  padding: 6px 10px;
}

.detail-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.simulation-title-panel {
  flex: 0 0 auto;
  margin-bottom: 10px;
  padding: 12px 18px;
}

.search-head,
.search-actions,
.selected-match,
.simulation-title,
.simulation-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.search-actions,
.simulation-actions {
  justify-content: flex-end;
}

.section-desc {
  color: #9fb0c7;
  line-height: 1.6;
}

.search-form {
  display: grid;
  grid-template-columns: minmax(180px, 1fr) minmax(180px, 1fr) 180px 160px;
  gap: 10px;
  margin: 16px 0;
}

.selected-match {
  border: 1px solid rgba(74, 222, 128, 0.28);
  border-radius: 12px;
  background: rgba(20, 83, 45, 0.18);
  color: #d7fbe8;
  padding: 12px;
  margin-bottom: 16px;
}

.selected-match span {
  color: #9fb0c7;
}

.inline-warning {
  color: #fde68a;
  font-size: 13px;
}

.match-card-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.history-page {
  padding: 18px;
}

.history-list {
  display: grid;
  gap: 10px;
  margin-top: 16px;
}

.history-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 12px;
  background: rgba(15, 28, 49, 0.7);
  padding: 12px;
}

.history-title {
  font-weight: 800;
}

.history-meta {
  margin-top: 6px;
  color: #9fb0c7;
  font-size: 13px;
}

.history-id {
  margin-top: 6px;
  color: #64748b;
  font-size: 12px;
}

.brand {
  font-size: 26px;
  font-weight: 900;
  letter-spacing: 0;
}

.subtitle {
  margin-top: 4px;
  color: #9fb0c7;
}

.connection {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #b8c7da;
}

.topbar-right,
.nav-actions,
.history-toolbar,
.history-card-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.topbar-right {
  justify-content: flex-end;
}

.history-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.connection-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #ef4444;
}

.connection-dot.connected {
  background: #22c55e;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: minmax(420px, 0.95fr) minmax(520px, 1.05fr);
  gap: 16px;
}

.simulation-grid {
  flex: 1;
  min-height: 0;
  grid-template-rows: minmax(360px, 0.58fr) minmax(240px, 0.42fr);
  gap: 12px;
  overflow: hidden;
}

.simulation-grid > section {
  min-height: 0;
  overflow: hidden;
}

.simulation-grid .control-panel {
  grid-column: 1;
  grid-row: 1;
}

.simulation-grid .analysis-panel {
  grid-column: 2;
  grid-row: 1;
}

.detail-panel {
  grid-column: 1 / -1;
  grid-row: 2;
  min-height: 0;
  padding: 10px 16px;
}

.detail-tabs {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.detail-tabs :deep(.el-tabs__content) {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.detail-tabs :deep(.el-tab-pane) {
  height: 100%;
  min-height: 0;
  overflow: auto;
  padding-right: 4px;
}

.control-panel,
.analysis-panel,
.report-panel,
.event-panel {
  padding: 18px;
}

.simulation-grid .control-panel,
.simulation-grid .analysis-panel {
  display: flex;
  flex-direction: column;
  padding: 16px 18px;
}

.event-panel {
  grid-row: span 3;
}

.section-title {
  margin-bottom: 12px;
  font-size: 18px;
  font-weight: 800;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.match-status {
  display: flex;
  gap: 12px;
  margin: 16px 0;
  color: #4ade80;
  font-weight: 800;
}

.scoreboard {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 82px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
  min-height: 0;
}

.simulation-grid .scoreboard :deep(.team-card) {
  padding: 12px;
}

.simulation-grid .scoreboard :deep(.team-title) {
  margin-bottom: 8px;
}

.simulation-grid .scoreboard :deep(.stats-grid) {
  gap: 5px 12px;
  font-size: 14px;
}

.simulation-grid .scoreboard :deep(.team-logo) {
  width: 34px;
  height: 34px;
}

.analysis-card,
.event-card {
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 12px;
  background: rgba(15, 28, 49, 0.76);
  padding: 14px;
}

.analysis-card.analysis-key {
  border-color: rgba(248, 113, 113, 0.46);
  background: linear-gradient(90deg, rgba(127, 29, 29, 0.26), rgba(15, 28, 49, 0.82));
  box-shadow: inset 4px 0 0 rgba(248, 113, 113, 0.85);
}

.analysis-card.analysis-observe {
  border-color: rgba(96, 165, 250, 0.38);
  background: linear-gradient(90deg, rgba(30, 64, 175, 0.18), rgba(15, 28, 49, 0.78));
  box-shadow: inset 4px 0 0 rgba(96, 165, 250, 0.78);
}

.analysis-card.analysis-low {
  border-color: rgba(148, 163, 184, 0.2);
  background: rgba(15, 28, 49, 0.52);
  box-shadow: inset 4px 0 0 rgba(148, 163, 184, 0.5);
  opacity: 0.78;
}

.analysis-card.team-home,
.event-card.team-home {
  border-left: 4px solid #22c55e;
}

.analysis-card.team-away,
.event-card.team-away {
  border-left: 4px solid #60a5fa;
}

.analysis-card.team-neutral,
.event-card.team-neutral {
  border-left: 4px solid #94a3b8;
}

.score {
  text-align: center;
  font-size: 32px;
  font-weight: 900;
}

.analysis-list,
.event-list {
  display: grid;
  gap: 10px;
  max-height: 560px;
  overflow: auto;
  padding-right: 4px;
}

.simulation-grid .analysis-list,
.detail-panel .event-list,
.detail-panel .trace-list {
  max-height: none;
  min-height: 0;
}

.simulation-grid .analysis-list {
  flex: 1;
}

.detail-panel .event-list,
.detail-panel .trace-list {
  height: 100%;
}

.analysis-meta,
.event-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  color: #9fb0c7;
  font-size: 13px;
}

.analysis-badge {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
  border-radius: 999px;
  padding: 2px 9px;
  font-size: 12px;
  font-weight: 800;
}

.analysis-badge.analysis-key {
  background: rgba(248, 113, 113, 0.16);
  color: #fecaca;
}

.analysis-badge.analysis-observe {
  background: rgba(96, 165, 250, 0.16);
  color: #bfdbfe;
}

.analysis-badge.analysis-low {
  background: rgba(148, 163, 184, 0.12);
  color: #cbd5e1;
}

.team-side-badge {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
  border-radius: 999px;
  padding: 2px 9px;
  font-size: 12px;
  font-weight: 900;
}

.team-side-badge.team-home {
  background: rgba(34, 197, 94, 0.14);
  color: #bbf7d0;
}

.team-side-badge.team-away {
  background: rgba(96, 165, 250, 0.15);
  color: #bfdbfe;
}

.team-side-badge.team-neutral {
  background: rgba(148, 163, 184, 0.12);
  color: #cbd5e1;
}

.analysis-conclusion,
.event-desc {
  margin-top: 6px;
  line-height: 1.6;
}

.evidence-list {
  margin: 10px 0 0;
  padding-left: 18px;
  color: #c8d6e8;
  line-height: 1.7;
}

.event-card {
  display: grid;
  grid-template-columns: 48px 1fr;
  gap: 12px;
}

.event-minute {
  color: #4ade80;
  font-weight: 900;
}

.report-summary {
  color: #c8d6e8;
  line-height: 1.7;
}

.task-progress {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
  color: #d7fbe8;
}

.trace-list {
  display: grid;
  gap: 10px;
  margin-top: 14px;
  max-height: 420px;
  overflow: auto;
  padding-right: 4px;
}

.trace-card {
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 12px;
  background: rgba(15, 28, 49, 0.76);
  padding: 12px;
}

.trace-title {
  color: #4ade80;
  font-weight: 800;
}

.trace-meta {
  margin-top: 6px;
  color: #9fb0c7;
  font-size: 13px;
}

.trace-output {
  margin-top: 8px;
  color: #c8d6e8;
  line-height: 1.6;
}

.structured-report {
  display: grid;
  gap: 12px;
}

.report-block {
  display: grid;
  gap: 10px;
}

.report-metrics {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-top: 14px;
}

.report-metrics div {
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 12px;
  padding: 12px;
  background: rgba(15, 28, 49, 0.66);
}

.report-metrics strong,
.report-metrics span {
  display: block;
}

.report-metrics strong {
  font-size: 20px;
}

.report-metrics span {
  margin-top: 4px;
  color: #9fb0c7;
  font-size: 13px;
}

.empty {
  border: 1px dashed rgba(148, 163, 184, 0.26);
  border-radius: 12px;
  padding: 22px;
  color: #9fb0c7;
  text-align: center;
}

@media (max-width: 1080px) {
  .simulation-mode {
    height: auto;
    min-height: 100vh;
    overflow: visible;
  }

  .dashboard-grid {
    grid-template-columns: 1fr;
  }

  .simulation-grid {
    grid-template-rows: auto;
    overflow: visible;
  }

  .simulation-grid > section {
    overflow: visible;
  }

  .simulation-grid .control-panel,
  .simulation-grid .analysis-panel,
  .detail-panel {
    grid-column: auto;
    grid-row: auto;
  }

  .match-card-grid,
  .search-form {
    grid-template-columns: 1fr 1fr;
  }

  .match-detail-hero,
  .match-detail-grid {
    grid-template-columns: 1fr;
  }

  .detail-team,
  .detail-team.away {
    justify-content: center;
  }

  .event-panel {
    grid-row: auto;
  }
}

@media (max-width: 640px) {
  .app-shell {
    padding: 12px;
  }

  .topbar,
  .search-head,
  .selected-match,
  .simulation-title,
  .history-card,
  .section-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .match-card-grid,
  .search-form,
  .scoreboard,
  .match-detail-grid,
  .report-metrics {
    grid-template-columns: 1fr;
  }
}
</style>
