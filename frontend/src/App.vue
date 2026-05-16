<template>
  <div :class="['app-shell', { 'simulation-mode': currentView === 'simulation' }]">
    <header class="topbar">
      <div>
        <div class="brand">TactiMind</div>
        <div class="subtitle">足球战术分析 AI Agent 系统</div>
      </div>
      <div v-if="currentView === 'simulation'" class="topbar-match-summary">
        <div class="topbar-match-title">
          {{ selectedMatch ? `${teamDisplayName(selectedMatch.homeTeam)} vs ${teamDisplayName(selectedMatch.awayTeam)}` : '比赛模拟演练' }}
        </div>
        <div class="topbar-match-meta">
          {{ selectedMatch ? `${competitionDisplayName(selectedMatch.competition)} ${selectedMatch.season} | ${selectedMatch.matchDate}` : '当前比赛演练' }}
          <span v-if="selectedMatch?.simulated">模拟数据，仅用于战术演练</span>
        </div>
      </div>
      <div class="topbar-right">
        <div v-if="currentView !== 'simulation'" class="nav-actions">
          <el-button :type="currentView === 'catalog' || currentView === 'match-detail' ? 'primary' : 'default'" plain @click="showCatalogView">比赛库</el-button>
          <el-button :type="currentView === 'history' ? 'primary' : 'default'" plain @click="showHistoryView">历史任务</el-button>
        </div>
        <div v-else class="simulation-topbar-actions">
          <el-button @click="backToCatalog">{{ backButtonText }}</el-button>
          <el-button type="primary" plain @click="loadSnapshot">刷新看板</el-button>
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
        本场为{{ competitionDisplayName(selectedMatch.competition) }}主题模拟演练：事件流、阵容能力标签和战术资料均为项目演示数据，仅用于验证 Agent 流程，不代表真实比赛事实。
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
        <article>
          <span>事件流状态</span>
          <strong>{{ selectedMatch.eventFilePath ? '已绑定事件流' : '暂无事件流' }}</strong>
        </article>
      </div>

      <div class="capability-panel">
        <div class="section-title">可用分析能力</div>
        <div class="capability-tags">
          <span v-for="capability in selectedMatch.capabilities" :key="capability">{{ capability }}</span>
        </div>
      </div>

      <div class="profile-panel">
        <div class="section-head compact-head">
          <div>
            <div class="section-title">比赛战术资料</div>
            <div class="section-desc">阵型、球员状态和能力标签会作为后续 Agent 分析的辅助依据。</div>
          </div>
          <el-button size="small" @click="loadSelectedProfile">刷新资料</el-button>
        </div>

        <div v-if="!selectedProfile" class="empty">该比赛暂无战术资料，后续可接入阵容和球员数据。</div>
        <div v-else class="profile-grid">
          <article class="team-profile-card">
            <div class="profile-card-title">
              <span class="team-logo">{{ teamLogoText(selectedProfile.home.team) }}</span>
              <div>
                <strong>{{ teamDisplayName(selectedProfile.home.team) }}</strong>
                <span>{{ selectedProfile.home.formation }}｜{{ selectedProfile.home.style }}</span>
              </div>
            </div>
            <div class="profile-meta">主教练：{{ selectedProfile.home.coach }}</div>
            <div class="profile-meta">压迫方式：{{ selectedProfile.home.pressingStyle }}</div>
            <div class="profile-meta">推进重点：{{ selectedProfile.home.buildUpFocus }}</div>

            <div class="formation-pitch">
              <div
                v-for="(player, index) in selectedProfile.home.startingLineup"
                :key="player.name"
                class="pitch-player"
                :style="lineupPositionStyle(player, index, selectedProfile.home.startingLineup.length)"
              >
                <strong>{{ player.number }}</strong>
                <span>{{ player.name }}</span>
                <em>{{ player.position }}</em>
              </div>
            </div>

            <div class="substitute-panel">
              <strong>替补席</strong>
              <div class="substitute-list">
                <div v-for="player in selectedProfile.home.substitutes" :key="player.name" class="substitute-chip">
                  {{ player.number }}号 {{ player.name }}｜{{ player.position }}
                </div>
              </div>
            </div>
          </article>

          <article class="team-profile-card away-profile">
            <div class="profile-card-title">
              <span class="team-logo">{{ teamLogoText(selectedProfile.away.team) }}</span>
              <div>
                <strong>{{ teamDisplayName(selectedProfile.away.team) }}</strong>
                <span>{{ selectedProfile.away.formation }}｜{{ selectedProfile.away.style }}</span>
              </div>
            </div>
            <div class="profile-meta">主教练：{{ selectedProfile.away.coach }}</div>
            <div class="profile-meta">压迫方式：{{ selectedProfile.away.pressingStyle }}</div>
            <div class="profile-meta">推进重点：{{ selectedProfile.away.buildUpFocus }}</div>

            <div class="formation-pitch">
              <div
                v-for="(player, index) in selectedProfile.away.startingLineup"
                :key="player.name"
                class="pitch-player"
                :style="lineupPositionStyle(player, index, selectedProfile.away.startingLineup.length)"
              >
                <strong>{{ player.number }}</strong>
                <span>{{ player.name }}</span>
                <em>{{ player.position }}</em>
              </div>
            </div>

            <div class="substitute-panel">
              <strong>替补席</strong>
              <div class="substitute-list">
                <div v-for="player in selectedProfile.away.substitutes" :key="player.name" class="substitute-chip">
                  {{ player.number }}号 {{ player.name }}｜{{ player.position }}
                </div>
              </div>
            </div>
          </article>

          <article class="profile-notes">
            <strong>关键影响因素</strong>
            <ul>
              <li v-for="factor in selectedProfile.keyFactors" :key="factor">{{ factor }}</li>
            </ul>
            <strong>数据说明</strong>
            <ul>
              <li v-for="note in selectedProfile.dataNotes" :key="note">{{ note }}</li>
            </ul>
          </article>
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

    <main v-if="currentView === 'simulation'" class="dashboard-grid simulation-grid">
      <section class="soft-card control-panel">
        <div class="control-panel-head">
          <div class="section-title">实时演练控制台</div>
          <div class="match-status">
            第 {{ state.currentMinute }} 分钟
            <span>{{ state.running ? '模拟中' : '已暂停' }}</span>
            <span>{{ simulationSpeed }}x</span>
            <span v-if="state.finished">已结束</span>
          </div>
        </div>
        <div class="actions">
          <el-button :type="state.running ? 'warning' : 'success'" @click="toggleSimulation">
            {{ state.running ? '暂停演练' : '开始演练' }}
          </el-button>
          <el-button type="danger" plain @click="runAction('reset')">重置</el-button>
          <el-button @click="loadSnapshot">刷新状态</el-button>
          <el-button type="primary" plain @click="runAction('analyze')">立即分析</el-button>
        </div>

        <div class="playback-tools">
          <div class="speed-control">
            <span>演练倍速</span>
            <el-segmented v-model="simulationSpeed" :options="speedOptions" @change="changeSimulationSpeed" />
          </div>

          <div class="timeline-control">
            <div class="timeline-head">
              <span>比赛时间轴</span>
              <strong>目标第 {{ seekMinute }} 分钟</strong>
            </div>
            <el-slider
              v-model="seekMinute"
              :min="0"
              :max="finalMinute"
              :step="1"
              :show-tooltip="false"
              :disabled="seeking"
              @input="markTimelineEdited"
              @change="jumpToMinute"
            />
            <div class="timeline-marks">
              <span
                v-for="mark in timelineMarks"
                :key="mark.minute"
                :class="{
                  highlight: mark.highlight,
                  start: mark.minute === 0,
                  end: mark.minute === finalMinute
                }"
                :style="{ left: `${timelineMarkPosition(mark.minute)}%` }"
              >
                {{ mark.label }}
              </span>
            </div>
            <div class="timeline-signals" aria-label="比赛事件和 Agent 分析时间点">
              <button
                v-for="dot in timelineSignalDots"
                :key="dot.minute"
                type="button"
                :class="[
                  'timeline-signal',
                  {
                    'has-analysis': dot.analysisCount > 0,
                    'is-current': dot.minute === state.currentMinute
                  }
                ]"
                :style="{ left: `${timelineMarkPosition(dot.minute)}%` }"
                :title="`第 ${dot.minute} 分钟：${dot.eventCount} 个事件，${dot.analysisCount} 条分析`"
                @click="jumpToTimelineMinute(dot.minute)"
              >
                <span>{{ dot.minute }}'</span>
              </button>
            </div>
          </div>
        </div>

        <div class="scoreboard">
          <TeamStatsCard :team-name="homeDisplayTeam" :stats="homeStats" />
          <div :class="['score', { 'score-updated': scoreChanged }]">{{ homeStats.goals }} - {{ awayStats.goals }}</div>
          <TeamStatsCard :team-name="awayDisplayTeam" :stats="awayStats" />
        </div>
      </section>

      <section class="soft-card analysis-panel">
        <div v-if="selectedMatch?.simulated" class="simulation-data-note">
          当前 Agent 依据的是模拟事件流 + 手工战术资料 Profile，用于演示工具调用、证据链和防幻觉机制，不等同于真实历史比赛数据。
        </div>
        <div class="section-head compact-head">
          <div class="section-title">Agent 战术分析</div>
          <span class="focus-counter">
            当前第 {{ state.currentMinute }} 分钟：{{ currentMinuteEvents.length }} 个事件 / {{ currentMinuteAnalyses.length }} 条分析
          </span>
        </div>
        <div v-if="realtimeTrendHints.length > 0" class="trend-hints trend-hints-compact">
          <article
            :class="['trend-hint-card', 'trend-hint-primary', `trend-${realtimeTrendHints[0].level}`, teamSideClass(realtimeTrendHints[0].team)]"
          >
            <div class="trend-primary-row">
              <div class="analysis-meta">
                <span class="analysis-badge trend-badge">DataAgent 实时提示</span>
                <span :class="['team-side-badge', teamSideClass(realtimeTrendHints[0].team)]">
                  {{ displayTeamName(realtimeTrendHints[0].team) }}
                </span>
                近 10 分钟 | 置信度 {{ confidenceText(realtimeTrendHints[0].confidence) }}
              </div>
              <strong>{{ realtimeTrendHints[0].title }}</strong>
            </div>
            <div class="trend-evidence-line">
              {{ realtimeTrendHints[0].evidence[0] }}
            </div>
          </article>

          <div v-if="realtimeTrendHints.length > 1" class="trend-mini-list">
            <span
              v-for="hint in realtimeTrendHints.slice(1)"
              :key="hint.id"
              :class="['trend-mini-chip', teamSideClass(hint.team)]"
              :title="`${hint.title}｜${hint.evidence.join('；')}`"
            >
              {{ displayTeamName(hint.team) }}：{{ hint.title }}
            </span>
          </div>
        </div>
        <div v-if="analyses.length === 0" class="empty">等待 Agent 基于比赛数据生成战术分析...</div>
        <div v-else class="analysis-list">
          <article
            v-for="analysis in orderedAnalyses"
            :key="`${analysis.minute}-${analysis.conclusion}`"
            :class="[
              'analysis-card',
              analysisLevelClass(analysis),
              teamSideClass(analysisTeamName(analysis)),
              { 'is-current-minute': analysis.minute === state.currentMinute }
            ]"
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
          <el-tab-pane label="战术图表" name="charts" class="charts-tab">
            <TacticalCharts :state="state" :events="events" :home-team-name="homeDisplayTeam" :away-team-name="awayDisplayTeam" embedded />
          </el-tab-pane>
          <el-tab-pane label="Agent 执行链路" name="trace">
            <div v-if="!analysisTask && visibleAgentTraces.length === 0" class="empty">
              启动 Agent 分析或比赛演练后，将展示每一步工具调用过程。
            </div>
            <div v-else>
              <div v-if="analysisTask" class="task-progress">
                <span>{{ analysisTask.currentStep }}</span>
                <strong>{{ analysisTask.progress }}%</strong>
              </div>
              <el-progress v-if="analysisTask" :percentage="analysisTask.progress" :stroke-width="10" />
              <div v-else class="task-progress">
                <span>实时演练链路</span>
                <strong>{{ visibleAgentTraces.length }} 条</strong>
              </div>
              <div class="trace-list">
                <article
                  v-for="trace in visibleAgentTraces"
                  :key="`${trace.createdAt}-${trace.stepName}-${trace.toolName}`"
                  class="trace-card"
                >
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
                :class="['event-card', teamSideClass(event.team), { 'is-current-minute': event.minute === state.currentMinute }]"
              >
                <div class="event-minute">{{ event.minute }}'</div>
                <div>
                  <div class="event-meta">
                    <span :class="['team-side-badge', teamSideClass(event.team)]">{{ displayTeamName(event.team || '比赛') }}</span>
                    <span>{{ eventTypeName(event.type) }}</span>
                    <span v-for="chip in eventDetailChips(event)" :key="chip" class="event-data-chip">{{ chip }}</span>
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
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { analysisTaskApi } from './api/analysisTaskApi'
import { legacyMatchApi } from './api/legacyMatchApi'
import { matchCatalogApi } from './api/matchCatalogApi'
import type {
  AgentTraceLog,
  AnalysisTask,
  AnalysisTaskHistoryItem,
  DataInsight,
  MatchCatalogItem,
  MatchEvent,
  MatchReport,
  MatchSearchParams,
  MatchState,
  MatchTacticalProfile,
  PlayerProfile,
  PersistenceStatus,
  TacticalAnalysis,
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

interface RealtimeTrendHint {
  id: string
  team: string
  title: string
  evidence: string[]
  confidence: number
  level: 'high' | 'medium'
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
const dataInsights = ref<DataInsight[]>([])
const report = ref<MatchReport | null>(null)
const analysisTask = ref<AnalysisTask | null>(null)
const agentTraces = ref<AgentTraceLog[]>([])
const realtimeAgentTraces = ref<AgentTraceLog[]>([])
const agentReport = ref<TacticalReport | null>(null)
const historyTasks = ref<AnalysisTaskHistoryItem[]>([])
const catalogOptions = ref<MatchCatalogItem[]>([])
const matches = ref<MatchCatalogItem[]>([])
const selectedMatch = ref<MatchCatalogItem | null>(null)
const selectedProfile = ref<MatchTacticalProfile | null>(null)
const matchLoading = ref(false)
const currentView = ref<'catalog' | 'match-detail' | 'simulation' | 'history'>('catalog')
const previousListView = ref<'catalog' | 'history'>('catalog')
const activeDetailTab = ref('charts')
const simulationSpeed = ref(1)
const seekMinute = ref(0)
const finalMinute = ref(90)
const timelineEdited = ref(false)
const seeking = ref(false)
const speedOptions = [
  { label: '0.5x', value: 0.5 },
  { label: '1x', value: 1 },
  { label: '2x', value: 2 },
  { label: '4x', value: 4 },
  { label: '8x', value: 8 }
]
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
const scoreChanged = ref(false)
let socket: WebSocket | null = null
let scoreResetTimer: number | null = null

const teamNames = computed(() => {
  const names = Object.keys(state.teams)
  return names.length >= 2 ? names : ['Team A', 'Team B']
})

const homeStats = computed(() => state.teams[teamNames.value[0]] ?? emptyStats)
const awayStats = computed(() => state.teams[teamNames.value[1]] ?? emptyStats)
const homeDisplayTeam = computed(() => selectedMatch.value?.homeTeam ?? selectedProfile.value?.home.team ?? teamNames.value[0])
const awayDisplayTeam = computed(() => selectedMatch.value?.awayTeam ?? selectedProfile.value?.away.team ?? teamNames.value[1])
const selectedMatchTitle = computed(() => {
  if (!selectedMatch.value) return '比赛模拟演练'
  const title = `${teamDisplayName(selectedMatch.value.homeTeam)} vs ${teamDisplayName(selectedMatch.value.awayTeam)}`
  return selectedMatch.value.simulated ? `主题模拟演练：${title}` : title
})

const selectedMatchMeta = computed(() => {
  if (!selectedMatch.value) return '当前比赛演练'
  const base = `${competitionDisplayName(selectedMatch.value.competition)} ${selectedMatch.value.season} | ${selectedMatch.value.matchDate}`
  return selectedMatch.value.simulated ? `${base} | 非真实事件流` : base
})
const orderedEvents = computed(() => [...events.value].reverse())
const orderedAnalyses = computed(() => [...analyses.value].reverse())
const visibleAgentTraces = computed(() => [...realtimeAgentTraces.value, ...agentTraces.value].slice(-60).reverse())
const backButtonText = computed(() => (previousListView.value === 'history' ? '返回历史任务' : '返回比赛库'))
const currentMinuteEvents = computed(() => events.value.filter((event) => event.minute === state.currentMinute))
const currentMinuteAnalyses = computed(() => analyses.value.filter((analysis) => analysis.minute === state.currentMinute))
const recentWindowStart = computed(() => Math.max(0, state.currentMinute - 10))
const recentWindowEvents = computed(() =>
  events.value.filter((event) => event.minute >= recentWindowStart.value && event.minute <= state.currentMinute)
)
const realtimeTrendHints = computed<RealtimeTrendHint[]>(() => {
  const agentHints = dataInsights.value
    .filter((insight) => insight.minute >= recentWindowStart.value && insight.minute <= state.currentMinute)
    .map((insight) => ({
      id: `agent-${insight.code}-${insight.subjectTeam}-${insight.minute}`,
      team: insight.subjectTeam,
      title: insight.summary,
      evidence: insight.evidence,
      confidence: Math.max(0.5, Math.min(0.95, insight.strength)),
      level: insight.strength >= 0.78 ? 'high' : 'medium'
    }) satisfies RealtimeTrendHint)
    .sort((first, second) => second.confidence - first.confidence)
    .slice(0, 3)

  if (agentHints.length > 0) {
    return agentHints
  }

  if (state.currentMinute <= 0 || recentWindowEvents.value.length < 2) return []

  const stats = new Map<string, { total: number; threat: number; zones: Map<string, number> }>()
  teamNames.value.forEach((team) => {
    stats.set(team, { total: 0, threat: 0, zones: new Map<string, number>() })
  })

  recentWindowEvents.value.forEach((event) => {
    const bucket = stats.get(event.team) ?? { total: 0, threat: 0, zones: new Map<string, number>() }
    bucket.total += 1

    if (['SHOT', 'GOAL', 'DANGEROUS_ATTACK', 'CORNER'].includes(event.type)) {
      bucket.threat += 1
    }

    const zone = String(event.data.zone ?? 'unknown')
    if (zone !== 'unknown') {
      bucket.zones.set(zone, (bucket.zones.get(zone) ?? 0) + 1)
    }

    stats.set(event.team, bucket)
  })

  const hints: RealtimeTrendHint[] = []
  const sortedByTotal = [...stats.entries()].sort((first, second) => second[1].total - first[1].total)
  const [mostActive, secondActive] = sortedByTotal

  if (mostActive && mostActive[1].total >= 3 && mostActive[1].total - (secondActive?.[1].total ?? 0) >= 2) {
    hints.push({
      id: `active-${mostActive[0]}-${state.currentMinute}`,
      team: mostActive[0],
      title: `${displayTeamName(mostActive[0])} 近 10 分钟比赛参与度明显提高`,
      evidence: [
        `第 ${recentWindowStart.value} 到 ${state.currentMinute} 分钟，${displayTeamName(mostActive[0])} 触发 ${mostActive[1].total} 个事件`,
        `同期对手事件数为 ${secondActive?.[1].total ?? 0} 个`
      ],
      confidence: 0.68,
      level: 'medium'
    })
  }

  stats.forEach((teamStats, team) => {
    if (teamStats.threat >= 2) {
      hints.push({
        id: `threat-${team}-${state.currentMinute}`,
        team,
        title: `${displayTeamName(team)} 近期进攻威胁正在上升`,
        evidence: [
          `近 10 分钟出现 ${teamStats.threat} 次射门、进球、角球或危险进攻事件`,
          `当前比赛时间为第 ${state.currentMinute} 分钟`
        ],
        confidence: teamStats.threat >= 3 ? 0.78 : 0.66,
        level: teamStats.threat >= 3 ? 'high' : 'medium'
      })
    }

    const topZone = [...teamStats.zones.entries()].sort((first, second) => second[1] - first[1])[0]
    if (topZone && topZone[1] >= 2) {
      hints.push({
        id: `zone-${team}-${topZone[0]}-${state.currentMinute}`,
        team,
        title: `${displayTeamName(team)} 近期更集中从${zoneText(topZone[0])}发起行动`,
        evidence: [
          `近 10 分钟 ${zoneText(topZone[0])} 相关事件达到 ${topZone[1]} 次`,
          '该提示只代表阶段性趋势，仍需结合阵型和球员位置继续校验'
        ],
        confidence: 0.62,
        level: 'medium'
      })
    }
  })

  return hints
    .sort((first, second) => second.confidence - first.confidence)
    .slice(0, 3)
})
const timelineMarks = computed(() => {
  const matchEnd = finalMinute.value
  const marks = [
    { minute: 0, label: "0'", highlight: false },
    { minute: 15, label: "15'", highlight: false },
    { minute: 30, label: "30'", highlight: false },
    { minute: 45, label: "半场 45'", highlight: true },
    { minute: 60, label: "60'", highlight: false },
    { minute: 75, label: "75'", highlight: false }
  ]

  if (matchEnd > 90) {
    marks.push({ minute: 90, label: "常规 90'", highlight: true })
  }
  if (matchEnd > 105) {
    marks.push({ minute: 105, label: "加时半场 105'", highlight: true })
  }
  if (matchEnd > 120) {
    marks.push({ minute: 120, label: "加时结束 120'", highlight: matchEnd > 120 })
  }
  marks.push({ minute: matchEnd, label: `终场 ${matchEnd}'`, highlight: true })

  return marks.filter(
    (mark, index) =>
      mark.minute <= matchEnd
      && marks.findIndex((item) => item.minute === mark.minute) === index
  )
})

const timelineSignalDots = computed(() => {
  const buckets = new Map<number, { minute: number; eventCount: number; analysisCount: number }>()
  const ensureBucket = (minute: number) => {
    if (!buckets.has(minute)) {
      buckets.set(minute, { minute, eventCount: 0, analysisCount: 0 })
    }
    return buckets.get(minute)!
  }

  events.value.forEach((event) => {
    if (event.minute < 0 || event.minute > finalMinute.value) return
    ensureBucket(event.minute).eventCount += 1
  })

  analyses.value.forEach((analysis) => {
    if (analysis.minute < 0 || analysis.minute > finalMinute.value) return
    ensureBucket(analysis.minute).analysisCount += 1
  })

  return [...buckets.values()].sort((first, second) => first.minute - second.minute)
})

watch(
  () => [homeStats.value.goals, awayStats.value.goals],
  (nextScore, previousScore) => {
    if (!previousScore || nextScore[0] === previousScore[0] && nextScore[1] === previousScore[1]) return
    markScoreChanged()
  }
)

onMounted(() => {
  connectWebSocket()
  loadCatalogOptions()
  searchMatches()
  loadHistoryTasks()
  loadSnapshot()
})

onUnmounted(() => {
  socket?.close()
  if (scoreResetTimer) {
    window.clearTimeout(scoreResetTimer)
  }
})

async function runAction(action: 'start' | 'pause' | 'reset' | 'analyze') {
  try {
    if (action === 'start') await legacyMatchApi.start()
    if (action === 'pause') await legacyMatchApi.pause()
    if (action === 'reset') {
      await legacyMatchApi.reset()
      events.value = []
      analyses.value = []
      dataInsights.value = []
      realtimeAgentTraces.value = []
      report.value = null
      timelineEdited.value = false
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

async function toggleSimulation() {
  await runAction(state.running ? 'pause' : 'start')
}

async function changeSimulationSpeed(value: number | string) {
  try {
    const nextSpeed = Number(value)
    const status = await legacyMatchApi.updateSpeed(nextSpeed)
    simulationSpeed.value = status.speed
  } catch (error) {
    ElMessage.error(userError(error))
  }
}

async function jumpToMinute() {
  try {
    seeking.value = true
    const status = await legacyMatchApi.seek(seekMinute.value)
    const allEvents = await legacyMatchApi.getEvents()
    events.value = allEvents.filter((event) => event.minute <= status.currentMinute)
    analyses.value = analyses.value.filter((analysis) => analysis.minute <= status.currentMinute)
    dataInsights.value = dataInsights.value.filter((insight) => insight.minute <= status.currentMinute)
    realtimeAgentTraces.value = []
    report.value = null
    timelineEdited.value = false
    seekMinute.value = status.currentMinute
    finalMinute.value = status.finalMinute
    await loadSnapshot()
    ElMessage.success(`已跳转到第 ${status.currentMinute} 分钟，可继续演练。`)
  } catch (error) {
    ElMessage.error(userError(error))
  } finally {
    seeking.value = false
  }
}

function markTimelineEdited() {
  timelineEdited.value = true
}

async function jumpToTimelineMinute(minute: number) {
  seekMinute.value = minute
  timelineEdited.value = true
  await jumpToMinute()
}

function timelineMarkPosition(minute: number) {
  if (finalMinute.value === 0) {
    return 0
  }
  return Math.min(100, Math.max(0, (minute / finalMinute.value) * 100))
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
  selectedProfile.value = null
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
  selectedProfile.value = null
}

async function enterSelectedMatchDetail() {
  if (!selectedMatch.value) {
    ElMessage.warning('请先选择一场可演练比赛。')
    return
  }
  if (!selectedMatch.value.playable) {
    ElMessage.warning('该比赛当前仅可查看，暂不支持启动 Agent 分析。')
    return
  }
  await loadSelectedProfile()
  currentView.value = 'match-detail'
}

async function loadSelectedProfile() {
  if (!selectedMatch.value) return
  try {
    selectedProfile.value = await matchCatalogApi.profile(selectedMatch.value.matchCode)
  } catch (error) {
    selectedProfile.value = null
    console.info('当前比赛暂无战术资料', error)
  }
}

function lineupPositionStyle(player: PlayerProfile, index: number, total: number) {
  const fallback = fallbackLineupPosition(index, total)
  const x = Number.isFinite(player.pitchX) && player.pitchX > 0 ? player.pitchX : fallback.x
  const y = Number.isFinite(player.pitchY) && player.pitchY > 0 ? player.pitchY : fallback.y

  return {
    left: `${x}%`,
    top: `${y}%`
  }
}

function fallbackLineupPosition(index: number, total: number) {
  if (total >= 11) {
    const fallback433 = [
      { x: 50, y: 92 },
      { x: 82, y: 72 },
      { x: 62, y: 76 },
      { x: 38, y: 76 },
      { x: 18, y: 72 },
      { x: 70, y: 52 },
      { x: 50, y: 56 },
      { x: 30, y: 52 },
      { x: 72, y: 30 },
      { x: 50, y: 24 },
      { x: 28, y: 30 }
    ]
    return fallback433[index] ?? { x: 50, y: 50 }
  }

  const rowY = [82, 62, 42, 24]
  const y = rowY[Math.min(index, rowY.length - 1)]
  const x = total === 1 ? 50 : 20 + (index * 60) / Math.max(1, total - 1)
  return { x, y }
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
  if (!selectedMatch.value.eventFilePath) {
    ElMessage.warning('该比赛暂无可演练事件流文件，请选择模拟演练或深度演练比赛。')
    return
  }
  await legacyMatchApi.select(selectedMatch.value.matchCode)
  events.value = []
  analyses.value = []
  dataInsights.value = []
  realtimeAgentTraces.value = []
  report.value = null
  timelineEdited.value = false
  await createAnalysisTask(selectedMatch.value)
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
    await loadSelectedProfile()
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
    const latestStatus = await legacyMatchApi.getStatus()
    Object.assign(state, latestState)
    analyses.value = latestAnalyses
    Object.assign(persistenceStatus, latestPersistence)
    simulationSpeed.value = latestStatus.speed
    finalMinute.value = latestStatus.finalMinute
    if (!timelineEdited.value) {
      seekMinute.value = latestStatus.currentMinute
    }
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
  if (message.messageType === 'DATA_INSIGHT') {
    dataInsights.value.push(message.payload as DataInsight)
    dataInsights.value = dataInsights.value.slice(-20)
    return
  }
  if (message.messageType === 'AGENT_TRACE') {
    realtimeAgentTraces.value.push(message.payload as AgentTraceLog)
    realtimeAgentTraces.value = realtimeAgentTraces.value.slice(-40)
    return
  }
  if (message.messageType === 'SIMULATION_STATUS') {
    const status = message.payload as {
      currentMinute: number
      running: boolean
      finished: boolean
      eventCursor: number
      speed: number
      finalMinute: number
    }
    state.currentMinute = status.currentMinute
    state.running = status.running
    state.finished = status.finished
    state.eventCursor = status.eventCursor
    simulationSpeed.value = status.speed
    finalMinute.value = status.finalMinute
    if (!timelineEdited.value) {
      seekMinute.value = status.currentMinute
    }
  }
  if (message.messageType === 'SIMULATION_ERROR') {
    ElMessage.error('比赛模拟异常，请查看后端日志。')
  }
}

function confidenceText(value: number) {
  return `${Math.round(value * 100)}%`
}

function markScoreChanged() {
  scoreChanged.value = false
  window.requestAnimationFrame(() => {
    scoreChanged.value = true
  })

  if (scoreResetTimer) {
    window.clearTimeout(scoreResetTimer)
  }

  scoreResetTimer = window.setTimeout(() => {
    scoreChanged.value = false
  }, 850)
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

function displayTeamName(team?: string) {
  if (team === teamNames.value[0] || team === 'Team A') {
    return teamDisplayName(homeDisplayTeam.value)
  }
  if (team === teamNames.value[1] || team === 'Team B') {
    return teamDisplayName(awayDisplayTeam.value)
  }
  return teamDisplayName(team || '比赛')
}

function teamSideClass(team?: string) {
  const displayName = displayTeamName(team)
  if (displayName === teamDisplayName(homeDisplayTeam.value)) {
    return 'team-home'
  }
  if (displayName === teamDisplayName(awayDisplayTeam.value)) {
    return 'team-away'
  }
  return 'team-neutral'
}

function analysisTeamName(analysis: { conclusion: string; evidence: string[] }) {
  const text = `${analysis.conclusion} ${analysis.evidence.join(' ')}`
  const homeName = teamDisplayName(homeDisplayTeam.value)
  const awayName = teamDisplayName(awayDisplayTeam.value)
  if (text.includes(homeName) || text.includes(homeDisplayTeam.value) || text.includes(teamNames.value[0])) {
    return homeName
  }
  if (text.includes(awayName) || text.includes(awayDisplayTeam.value) || text.includes(teamNames.value[1])) {
    return awayName
  }
  return '双方'
}

function zoneText(zone: unknown) {
  const names: Record<string, string> = {
    left: '左路',
    right: '右路',
    middle: '中路',
    left_half_space: '左肋部',
    right_half_space: '右肋部',
    box: '禁区',
    unknown: '未知区域'
  }
  return names[String(zone ?? 'unknown')] ?? String(zone)
}

function eventDetailChips(event: MatchEvent) {
  const chips: string[] = []
  const zone = zoneText(event.data.zone)
  const direction = directionText(event.data.direction)
  const phase = phaseText(event.data.phase)
  const result = resultText(event.data.result)
  if (zone !== '未知区域') chips.push(`区域：${zone}`)
  if (direction !== '未知方向') chips.push(`方向：${direction}`)
  if (phase !== '未知阶段') chips.push(`阶段：${phase}`)
  if (result !== '未知结果') chips.push(`结果：${result}`)
  if (event.data.receiver) chips.push(`接应：${event.data.receiver}`)
  return chips
}

function directionText(direction: unknown) {
  const names: Record<string, string> = {
    left: '左路',
    right: '右路',
    middle: '中路',
    unknown: '未知方向'
  }
  return names[String(direction ?? 'unknown')] ?? String(direction)
}

function phaseText(phase: unknown) {
  const names: Record<string, string> = {
    build_up: '组织推进',
    transition: '攻防转换',
    set_piece: '定位球',
    defense: '防守阶段',
    adjustment: '人员调整',
    unknown: '未知阶段'
  }
  return names[String(phase ?? 'unknown')] ?? String(phase)
}

function resultText(result: unknown) {
  const names: Record<string, string> = {
    success: '成功',
    lost: '丢失球权',
    threat: '形成威胁',
    goal: '进球',
    on_target: '射正',
    off_target: '偏出',
    corner_won: '赢得角球',
    yellow_card: '黄牌',
    substitution: '换人',
    unknown: '未知结果'
  }
  return names[String(result ?? 'unknown')] ?? String(result)
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
  border-bottom: 1px solid rgba(242, 198, 109, 0.14);
  padding-bottom: 14px;
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
  color: #f2c66d;
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

.profile-panel {
  display: grid;
  gap: 12px;
}

.profile-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 12px;
}

.team-profile-card,
.profile-notes {
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 14px;
  background: rgba(15, 28, 49, 0.72);
  padding: 14px;
}

.team-profile-card {
  box-shadow: inset 4px 0 0 rgba(242, 198, 109, 0.78);
}

.team-profile-card.away-profile {
  box-shadow: inset 4px 0 0 rgba(96, 165, 250, 0.78);
}

.profile-card-title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.profile-card-title strong,
.profile-card-title span {
  display: block;
}

.profile-card-title strong {
  color: #eef3fb;
  font-size: 18px;
}

.profile-card-title div > span,
.profile-meta {
  color: #9fb0c7;
  line-height: 1.6;
}

.formation-pitch {
  position: relative;
  height: 430px;
  margin-top: 14px;
  overflow: hidden;
  border: 1px solid rgba(125, 184, 255, 0.24);
  border-radius: 18px;
  background:
    linear-gradient(90deg, rgba(255, 255, 255, 0.12) 1px, transparent 1px) 50% 0 / 1px 100% no-repeat,
    radial-gradient(circle at 50% 50%, transparent 0 58px, rgba(255, 255, 255, 0.16) 59px 61px, transparent 62px),
    linear-gradient(180deg, rgba(34, 91, 65, 0.74), rgba(18, 69, 52, 0.82));
}

.formation-pitch::before,
.formation-pitch::after {
  content: '';
  position: absolute;
  left: 26%;
  width: 48%;
  height: 68px;
  border: 1px solid rgba(255, 255, 255, 0.22);
  pointer-events: none;
}

.formation-pitch::before {
  top: 0;
  border-top: 0;
}

.formation-pitch::after {
  bottom: 0;
  border-bottom: 0;
}

.pitch-player {
  position: absolute;
  display: grid;
  justify-items: center;
  min-width: 78px;
  transform: translate(-50%, -50%);
  text-align: center;
}

.pitch-player strong {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border-radius: 999px;
  background: #f2c66d;
  color: #101827;
  box-shadow: 0 8px 22px rgba(0, 0, 0, 0.28);
}

.away-profile .pitch-player strong {
  background: #7db8ff;
}

.pitch-player span {
  margin-top: 4px;
  max-width: 90px;
  color: #eef3fb;
  font-size: 12px;
  font-weight: 800;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pitch-player em {
  color: #c8d6e8;
  font-size: 11px;
  font-style: normal;
}

.substitute-panel {
  margin-top: 12px;
}

.substitute-panel > strong {
  display: block;
  color: #eef3fb;
  margin-bottom: 8px;
}

.substitute-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.substitute-chip {
  border: 1px solid rgba(148, 163, 184, 0.16);
  border-radius: 999px;
  background: rgba(11, 18, 31, 0.58);
  color: #dbeafe;
  padding: 6px 10px;
  font-size: 12px;
  font-weight: 700;
}

.player-list {
  display: grid;
  gap: 8px;
  margin-top: 12px;
}

.player-row {
  border: 1px solid rgba(148, 163, 184, 0.14);
  border-radius: 10px;
  background: rgba(11, 18, 31, 0.55);
  padding: 10px;
}

.player-row span,
.player-row strong,
.player-row em {
  display: block;
}

.player-row strong {
  margin-top: 4px;
  color: #dbeafe;
}

.player-row em {
  margin-top: 2px;
  color: #f2c66d;
  font-style: normal;
  font-size: 13px;
}

.ability-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.ability-tags span {
  border-radius: 999px;
  background: rgba(242, 198, 109, 0.13);
  color: #fde7ae;
  padding: 3px 8px;
  font-size: 12px;
  font-weight: 700;
}

.profile-notes {
  grid-column: 1 / -1;
  color: #c8d6e8;
}

.profile-notes strong {
  display: block;
  margin-top: 4px;
  color: #eef3fb;
}

.profile-notes ul {
  margin: 8px 0 12px;
  padding-left: 18px;
  line-height: 1.7;
}

.detail-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.search-head,
.search-actions,
.selected-match,
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
  border: 1px solid rgba(125, 184, 255, 0.26);
  border-radius: 12px;
  background: rgba(22, 33, 53, 0.58);
  color: #e7eefb;
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
  color: #f8f4ea;
}

.subtitle {
  margin-top: 4px;
  color: #b9c7db;
}

.topbar-match-summary {
  flex: 1;
  min-width: 280px;
  margin: 0 18px;
  border-left: 1px solid rgba(242, 198, 109, 0.18);
  padding-left: 18px;
}

.topbar-match-title {
  font-size: 18px;
  font-weight: 800;
  color: #f8f4ea;
}

.topbar-match-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 4px;
  color: #b9c7db;
  font-size: 13px;
}

.topbar-match-meta span {
  border: 1px solid rgba(242, 198, 109, 0.28);
  border-radius: 999px;
  background: rgba(242, 198, 109, 0.12);
  color: #fde7ae;
  padding: 2px 8px;
}

.connection {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #b8c7da;
}

.topbar-right,
.nav-actions,
.simulation-topbar-actions,
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
  background: #f2c66d;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: minmax(420px, 0.95fr) minmax(520px, 1.05fr);
  gap: 16px;
}

.simulation-grid {
  flex: 1;
  min-height: 0;
  grid-template-rows: minmax(405px, 0.6fr) minmax(280px, 0.4fr);
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

.detail-tabs :deep(.el-tab-pane.charts-tab) {
  overflow: hidden;
  padding-right: 0;
}

.detail-tabs :deep(.el-tabs__item.is-active) {
  color: #f2c66d;
}

.detail-tabs :deep(.el-tabs__active-bar) {
  background-color: #f2c66d;
}

.detail-tabs :deep(.el-tabs__nav-wrap::after) {
  background-color: rgba(242, 198, 109, 0.14);
}

.detail-tabs :deep(.el-tabs__item) {
  color: #97a99d;
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

.control-panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.control-panel-head .section-title {
  margin-bottom: 0;
}

.event-panel {
  grid-row: span 3;
}

.section-title {
  margin-bottom: 12px;
  font-size: 18px;
  font-weight: 800;
  display: flex;
  align-items: center;
  gap: 9px;
}

.section-title::before {
  content: '';
  width: 4px;
  height: 18px;
  border-radius: 999px;
  background: linear-gradient(180deg, #f2c66d, #7db8ff);
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.playback-tools {
  display: grid;
  grid-template-columns: auto minmax(220px, 1fr);
  align-items: end;
  gap: 16px;
  margin-top: 12px;
}

.speed-control {
  display: flex;
  align-items: center;
  gap: 12px;
}

.speed-control span {
  color: #9fb0c7;
  font-weight: 700;
}

.timeline-control {
  display: grid;
  gap: 4px;
}

.timeline-head {
  display: flex;
  align-items: center;
  gap: 12px;
}

.timeline-head {
  justify-content: space-between;
  color: #9fb0c7;
  font-size: 14px;
}

.timeline-head strong {
  color: #f2c66d;
}

.timeline-control :deep(.el-slider__bar) {
  background-color: #f2c66d;
}

.timeline-control :deep(.el-slider__button) {
  border-color: #f2c66d;
}

.timeline-marks {
  position: relative;
  height: 18px;
  color: #7f8ca2;
  font-size: 12px;
}

.timeline-marks span {
  position: absolute;
  top: 0;
  transform: translateX(-50%);
  white-space: nowrap;
}

.timeline-marks span.start {
  transform: translateX(0);
}

.timeline-marks span.end {
  transform: translateX(-100%);
}

.timeline-marks span.highlight {
  color: #fde7ae;
  font-weight: 700;
}

.timeline-marks span:last-child {
  color: #d4deee;
  text-align: right;
}

.timeline-signals {
  position: relative;
  height: 18px;
}

.timeline-signal {
  position: absolute;
  top: 1px;
  width: 9px;
  height: 9px;
  padding: 0;
  border: 0;
  border-radius: 999px;
  background: #7db8ff;
  box-shadow: 0 0 0 4px rgba(125, 184, 255, 0.13);
  cursor: pointer;
  transform: translateX(-50%);
  transition: transform 0.16s ease, box-shadow 0.16s ease, background 0.16s ease;
}

.timeline-signal.has-analysis {
  background: #f2c66d;
  box-shadow: 0 0 0 4px rgba(242, 198, 109, 0.16);
}

.timeline-signal.is-current,
.timeline-signal:hover {
  transform: translateX(-50%) scale(1.35);
  box-shadow: 0 0 18px rgba(242, 198, 109, 0.38);
}

.timeline-signal span {
  position: absolute;
  left: 50%;
  top: 11px;
  color: #9fb0c7;
  font-size: 10px;
  opacity: 0;
  transform: translateX(-50%);
  white-space: nowrap;
  pointer-events: none;
}

.timeline-signal:hover span,
.timeline-signal.is-current span {
  opacity: 1;
}

.match-status {
  display: flex;
  gap: 12px;
  margin: 0;
  color: #f2c66d;
  font-weight: 800;
  white-space: nowrap;
}

.scoreboard {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 82px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
  min-height: 0;
  flex: 1;
}

.simulation-grid .scoreboard :deep(.team-card) {
  padding: 12px;
  background:
    linear-gradient(180deg, rgba(21, 31, 49, 0.94), rgba(12, 18, 31, 0.88));
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
  border-left: 4px solid #f2c66d;
}

.analysis-card.team-away,
.event-card.team-away {
  border-left: 4px solid #60a5fa;
}

.analysis-card.team-neutral,
.event-card.team-neutral {
  border-left: 4px solid #94a3b8;
}

.analysis-card.is-current-minute,
.event-card.is-current-minute {
  border-color: rgba(242, 198, 109, 0.62);
  box-shadow:
    inset 0 0 0 1px rgba(242, 198, 109, 0.16),
    0 0 20px rgba(242, 198, 109, 0.1);
}

.trend-hints {
  display: grid;
  gap: 8px;
  margin-bottom: 10px;
}

.trend-hints-compact {
  flex: 0 0 auto;
}

.trend-hint-card {
  border: 1px solid rgba(125, 184, 255, 0.22);
  border-radius: 12px;
  background:
    linear-gradient(90deg, rgba(20, 83, 135, 0.18), rgba(15, 28, 49, 0.7));
  padding: 12px;
  box-shadow: inset 4px 0 0 rgba(125, 184, 255, 0.7);
}

.trend-hint-primary {
  padding: 10px 12px;
}

.trend-primary-row {
  display: grid;
  gap: 5px;
}

.trend-primary-row strong {
  color: #eef3fb;
  font-size: 15px;
  line-height: 1.35;
}

.trend-evidence-line {
  margin-top: 6px;
  color: #c8d6e8;
  font-size: 13px;
  line-height: 1.45;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.trend-hint-card.trend-high {
  border-color: rgba(242, 198, 109, 0.4);
  background:
    linear-gradient(90deg, rgba(113, 63, 18, 0.22), rgba(15, 28, 49, 0.76));
  box-shadow: inset 4px 0 0 rgba(242, 198, 109, 0.86);
}

.trend-hint-card.team-home {
  border-left: 4px solid #f2c66d;
}

.trend-hint-card.team-away {
  border-left: 4px solid #60a5fa;
}

.trend-badge {
  background: rgba(125, 184, 255, 0.16);
  color: #bfdbfe;
}

.trend-mini-list {
  display: flex;
  flex-wrap: nowrap;
  gap: 8px;
  min-width: 0;
  overflow: hidden;
}

.trend-mini-chip {
  min-width: 0;
  max-width: 32%;
  border: 1px solid rgba(148, 163, 184, 0.16);
  border-radius: 999px;
  background: rgba(11, 18, 31, 0.58);
  color: #c8d6e8;
  padding: 5px 9px;
  font-size: 12px;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.trend-mini-chip.team-home {
  color: #fde7ae;
}

.trend-mini-chip.team-away {
  color: #bfdbfe;
}

.compact-evidence {
  margin-top: 6px;
  line-height: 1.55;
}

.compact-head {
  align-items: center;
  margin-bottom: 10px;
}

.focus-counter {
  color: #9fb0c7;
  font-size: 13px;
  font-weight: 700;
}

.score {
  text-align: center;
  font-size: 32px;
  font-weight: 900;
  color: #f3f7e8;
  border-radius: 14px;
  transition: color 0.2s ease, background 0.2s ease, transform 0.2s ease;
}

.score.score-updated {
  color: #101827;
  background: #f2c66d;
  animation: score-pulse 0.85s ease;
}

@keyframes score-pulse {
  0% {
    transform: scale(1);
    box-shadow: 0 0 0 rgba(242, 198, 109, 0);
  }

  35% {
    transform: scale(1.12);
    box-shadow: 0 0 24px rgba(242, 198, 109, 0.48);
  }

  100% {
    transform: scale(1);
    box-shadow: 0 0 0 rgba(242, 198, 109, 0);
  }
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
  background: rgba(242, 198, 109, 0.14);
  color: #fde7ae;
}

.team-side-badge.team-away {
  background: rgba(96, 165, 250, 0.15);
  color: #bfdbfe;
}

.team-side-badge.team-neutral {
  background: rgba(148, 163, 184, 0.12);
  color: #cbd5e1;
}

.event-data-chip {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
  border-radius: 999px;
  padding: 2px 8px;
  background: rgba(15, 23, 42, 0.7);
  border: 1px solid rgba(148, 163, 184, 0.16);
  color: #c8d6e8;
  font-size: 12px;
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
  color: #f2c66d;
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
  color: #7db8ff;
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

  .topbar-match-summary {
    width: 100%;
    margin: 0;
    border-left: 0;
    padding-left: 0;
  }

  .topbar-match-meta {
    align-items: flex-start;
    flex-direction: column;
  }

  .playback-tools {
    grid-template-columns: 1fr;
  }

  .control-panel-head {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>


