package com.zsj.tactimind.analysis.persistence;

import com.zsj.tactimind.analysis.model.AgentTraceLog;
import com.zsj.tactimind.analysis.model.AnalysisTaskHistoryItem;
import com.zsj.tactimind.analysis.model.AnalysisTaskResponse;
import com.zsj.tactimind.analysis.model.TacticalReport;

import java.util.List;
import java.util.Optional;

public abstract class AnalysisPersistenceFacade {
    public abstract void saveTask(AnalysisTaskResponse task, String matchId, String analysisType);

    public abstract void saveTraces(String taskId, List<AgentTraceLog> traces);

    public abstract void saveReport(TacticalReport report);

    public abstract Optional<AnalysisTaskResponse> findTask(String taskId);

    public abstract List<AnalysisTaskHistoryItem> listTasks();

    public abstract List<AgentTraceLog> listTraces(String taskId);

    public abstract Optional<TacticalReport> findReport(String reportId);

    public abstract void deleteTask(String taskId);

    public abstract void deleteAllTasks();
}
