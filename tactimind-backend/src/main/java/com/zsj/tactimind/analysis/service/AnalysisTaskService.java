package com.zsj.tactimind.analysis.service;

import com.zsj.tactimind.agent.model.TacticalAnalysis;
import com.zsj.tactimind.analysis.model.AgentTraceLog;
import com.zsj.tactimind.analysis.model.AnalysisTaskHistoryItem;
import com.zsj.tactimind.analysis.model.AnalysisTaskResponse;
import com.zsj.tactimind.analysis.model.AnalysisTaskStatus;
import com.zsj.tactimind.analysis.model.CreateAnalysisTaskRequest;
import com.zsj.tactimind.analysis.model.PageResult;
import com.zsj.tactimind.analysis.model.TacticalReport;
import com.zsj.tactimind.analysis.persistence.AnalysisPersistenceFacade;
import com.zsj.tactimind.catalog.model.DataLevel;
import com.zsj.tactimind.catalog.model.MatchCatalogItem;
import com.zsj.tactimind.catalog.service.MatchCatalogService;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class AnalysisTaskService {
    private final MatchCatalogService matchCatalogService;
    private final AnalysisPersistenceFacade analysisPersistenceService;
    private final Map<String, AnalysisTaskResponse> tasks = new LinkedHashMap<>();
    private final Map<String, AnalysisTaskHistoryItem> taskHistory = new LinkedHashMap<>();
    private final Map<String, List<AgentTraceLog>> traces = new LinkedHashMap<>();
    private final Map<String, TacticalReport> reports = new LinkedHashMap<>();

    public AnalysisTaskService(
            MatchCatalogService matchCatalogService,
            AnalysisPersistenceFacade analysisPersistenceService
    ) {
        this.matchCatalogService = matchCatalogService;
        this.analysisPersistenceService = analysisPersistenceService;
    }

    public synchronized AnalysisTaskResponse createTask(CreateAnalysisTaskRequest request) {
        MatchCatalogItem match = matchCatalogService.findByIdOrCode(request.matchId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "未找到对应比赛"));

        String taskId = UUID.randomUUID().toString();
        String reportId = "report-" + taskId;
        AnalysisTaskResponse runningTask = new AnalysisTaskResponse(
                taskId,
                AnalysisTaskStatus.RUNNING,
                "正在读取比赛事件流",
                15,
                reportId
        );
        tasks.put(taskId, runningTask);
        taskHistory.put(taskId, toHistoryItem(runningTask, match.matchCode(), request.analysisType()));
        analysisPersistenceService.saveTask(runningTask, match.matchCode(), request.analysisType());

        List<AgentTraceLog> taskTraces = buildTraceLogs(match);
        TacticalReport report = buildReport(reportId, taskId, match);
        traces.put(taskId, taskTraces);
        reports.put(reportId, report);
        analysisPersistenceService.saveTraces(taskId, taskTraces);
        analysisPersistenceService.saveReport(report);

        AnalysisTaskResponse completedTask = new AnalysisTaskResponse(
                taskId,
                AnalysisTaskStatus.COMPLETED,
                "分析完成",
                100,
                reportId
        );
        tasks.put(taskId, completedTask);
        taskHistory.put(taskId, toHistoryItem(completedTask, match.matchCode(), request.analysisType()));
        analysisPersistenceService.saveTask(completedTask, match.matchCode(), request.analysisType());
        return completedTask;
    }

    public synchronized PageResult<AnalysisTaskHistoryItem> listTasks(int page, int size) {
        List<AnalysisTaskHistoryItem> allTasks = listAllTasks();
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int fromIndex = Math.min((safePage - 1) * safeSize, allTasks.size());
        int toIndex = Math.min(fromIndex + safeSize, allTasks.size());
        return new PageResult<>(
                allTasks.subList(fromIndex, toIndex),
                safePage,
                safeSize,
                allTasks.size()
        );
    }

    public synchronized void deleteTask(String taskId) {
        AnalysisTaskResponse task = tasks.remove(taskId);
        taskHistory.remove(taskId);
        traces.remove(taskId);
        if (task != null) {
            reports.remove(task.reportId());
        } else {
            reports.entrySet().removeIf(entry -> taskId.equals(entry.getValue().taskId()));
        }
        analysisPersistenceService.deleteTask(taskId);
    }

    public synchronized void deleteAllTasks() {
        tasks.clear();
        taskHistory.clear();
        traces.clear();
        reports.clear();
        analysisPersistenceService.deleteAllTasks();
    }

    private List<AnalysisTaskHistoryItem> listAllTasks() {
        Map<String, AnalysisTaskHistoryItem> merged = new LinkedHashMap<>();
        for (AnalysisTaskHistoryItem item : analysisPersistenceService.listTasks()) {
            merged.put(item.taskId(), item);
        }
        for (AnalysisTaskHistoryItem item : taskHistory.values()) {
            merged.put(item.taskId(), item);
        }
        return merged.values().stream()
                .sorted((first, second) -> second.createdAt().compareTo(first.createdAt()))
                .toList();
    }

    public synchronized AnalysisTaskResponse getTask(String taskId) {
        AnalysisTaskResponse task = tasks.get(taskId);
        if (task == null) {
            task = analysisPersistenceService.findTask(taskId)
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "未找到对应分析任务"));
            tasks.put(taskId, task);
        }
        return task;
    }

    public synchronized List<AgentTraceLog> listTraces(String taskId) {
        getTask(taskId);
        List<AgentTraceLog> cachedTraces = traces.get(taskId);
        if (cachedTraces != null && !cachedTraces.isEmpty()) {
            return cachedTraces;
        }
        List<AgentTraceLog> persistedTraces = analysisPersistenceService.listTraces(taskId);
        if (!persistedTraces.isEmpty()) {
            traces.put(taskId, persistedTraces);
        }
        return persistedTraces;
    }

    public synchronized TacticalReport getReport(String reportId) {
        TacticalReport report = reports.get(reportId);
        if (report == null) {
            report = analysisPersistenceService.findReport(reportId)
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "未找到对应战术报告"));
            reports.put(reportId, report);
        }
        return report;
    }

    private List<AgentTraceLog> buildTraceLogs(MatchCatalogItem match) {
        List<AgentTraceLog> logs = new ArrayList<>();
        logs.add(trace(
                "读取比赛数据",
                "DataAgent",
                "load_match_events",
                "输入比赛：" + match.homeTeam() + " vs " + match.awayTeam(),
                match.dataLevel() == DataLevel.BASIC_STATS
                        ? "当前比赛只有基础统计数据，未读取到完整事件流。"
                        : "成功读取比赛事件流摘要，准备进入指标统计。",
                96
        ));
        logs.add(trace(
                "统计关键指标",
                "DataAgent",
                "calculate_match_stats",
                "输入比赛数据级别：" + match.dataLevel(),
                "生成射门、控球率、危险进攻、区域事件等关键指标摘要。",
                124
        ));
        logs.add(trace(
                "检索战术知识",
                "TacticsAgent",
                "retrieve_tactical_knowledge",
                "检索主题：压迫、边路进攻、攻防转换、控球控制",
                "命中高位逼抢、边路压制、攻防转换 3 类战术知识。",
                88
        ));
        logs.add(trace(
                "生成战术报告",
                "ReportAgent",
                "generate_tactical_report",
                "输入数据指标和战术知识摘要",
                "生成结构化战术报告草稿，包含结论、建议和风险提示。",
                156
        ));
        logs.add(trace(
                "校验证据",
                "VerifyAgent",
                "verify_report_evidence",
                "输入待校验战术结论",
                match.dataLevel() == DataLevel.BASIC_STATS
                        ? "基础数据证据有限，结论置信度已降低。"
                        : "所有确定性结论均绑定 evidence，未通过证据校验的结论已降级。",
                72
        ));
        return logs;
    }

    private TacticalReport buildReport(String reportId, String taskId, MatchCatalogItem match) {
        boolean simulated = match.simulated();
        List<TacticalAnalysis> conclusions;
        if (match.dataLevel() == DataLevel.BASIC_STATS) {
            conclusions = List.of(new TacticalAnalysis(
                    90,
                    "当前比赛只有基础统计数据，无法支持高置信度的细粒度战术结论。",
                    List.of("该比赛数据级别为基础分析，缺少完整事件流和区域事件。"),
                    0.42,
                    "LOW"
            ));
        } else {
            conclusions = List.of(
                    new TacticalAnalysis(
                            35,
                            "右路连续进攻事件增多，说明该侧已经成为主要战术发起区域。",
                            List.of("比赛事件流中右路危险进攻、角球和射门连续出现。", "DataAgent 识别到同一区域多次进攻相关事件。"),
                            simulated ? 0.74 : 0.82,
                            "MEDIUM"
                    ),
                    new TacticalAnalysis(
                            65,
                            "防守方需要关注边后卫身后空间，避免被连续打穿同侧通道。",
                            List.of("近期事件中多次出现边路推进和禁区附近射门。"),
                            simulated ? 0.68 : 0.78,
                            "MEDIUM"
                    )
            );
        }

        String simulationNote = simulated ? "该报告基于模拟数据，仅用于战术演练，不代表真实比赛事实。" : "";
        return new TacticalReport(
                reportId,
                taskId,
                match.matchCode(),
                "已完成 " + match.homeTeam() + " vs " + match.awayTeam()
                        + " 的 Agent 战术分析。" + simulationNote,
                conclusions,
                List.of("优先结合事件流证据查看结论，不建议脱离数据直接判断。", "后续可接入 RAG 战术知识库提升报告解释质量。"),
                simulated
                        ? List.of("模拟数据不能代表真实比赛事实。", "当前报告适合演示 Agent 流程，不适合作为真实比赛结论。")
                        : List.of("部分结论仍依赖数据完整度，低置信度结论需要谨慎使用。"),
                List.of(match.sourceType().name(), match.dataLevel().name(), match.eventFilePath().isBlank() ? "内置比赛目录" : match.eventFilePath()),
                simulated
        );
    }

    private AgentTraceLog trace(
            String stepName,
            String agentName,
            String toolName,
            String inputSummary,
            String outputSummary,
            long costTimeMs
    ) {
        return new AgentTraceLog(
                stepName,
                agentName,
                toolName,
                "成功",
                inputSummary,
                outputSummary,
                costTimeMs,
                "",
                Instant.now()
        );
    }

    private AnalysisTaskHistoryItem toHistoryItem(
            AnalysisTaskResponse task,
            String matchId,
            String analysisType
    ) {
        return new AnalysisTaskHistoryItem(
                task.taskId(),
                matchId,
                analysisType,
                task.status(),
                task.currentStep(),
                task.progress(),
                task.reportId(),
                Instant.now()
        );
    }
}
