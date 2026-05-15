package com.zsj.tactimind.analysis.persistence;

import com.zsj.tactimind.analysis.model.AgentTraceLog;
import com.zsj.tactimind.analysis.model.AnalysisTaskHistoryItem;
import com.zsj.tactimind.analysis.model.AnalysisTaskResponse;
import com.zsj.tactimind.analysis.model.TacticalReport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@ConditionalOnProperty(name = "tactimind.mysql.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpAnalysisPersistenceService extends AnalysisPersistenceFacade {
    @Override
    public void saveTask(AnalysisTaskResponse task, String matchId, String analysisType) {
    }

    @Override
    public void saveTraces(String taskId, List<AgentTraceLog> traces) {
    }

    @Override
    public void saveReport(TacticalReport report) {
    }

    @Override
    public Optional<AnalysisTaskResponse> findTask(String taskId) {
        return Optional.empty();
    }

    @Override
    public List<AnalysisTaskHistoryItem> listTasks() {
        return List.of();
    }

    @Override
    public List<AgentTraceLog> listTraces(String taskId) {
        return List.of();
    }

    @Override
    public Optional<TacticalReport> findReport(String reportId) {
        return Optional.empty();
    }

    @Override
    public void deleteTask(String taskId) {
    }

    @Override
    public void deleteAllTasks() {
    }
}
