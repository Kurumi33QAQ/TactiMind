package com.zsj.tactimind.analysis.web;

import com.zsj.tactimind.analysis.model.AgentTraceLog;
import com.zsj.tactimind.analysis.model.AnalysisTaskHistoryItem;
import com.zsj.tactimind.analysis.model.AnalysisTaskResponse;
import com.zsj.tactimind.analysis.model.CreateAnalysisTaskRequest;
import com.zsj.tactimind.analysis.model.PageResult;
import com.zsj.tactimind.analysis.model.TacticalReport;
import com.zsj.tactimind.analysis.service.AnalysisTaskService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin
@RestController
public class AnalysisTaskController {
    private final AnalysisTaskService analysisTaskService;

    public AnalysisTaskController(AnalysisTaskService analysisTaskService) {
        this.analysisTaskService = analysisTaskService;
    }

    @PostMapping("/api/analysis/tasks")
    public AnalysisTaskResponse createTask(@RequestBody CreateAnalysisTaskRequest request) {
        return analysisTaskService.createTask(request);
    }

    @GetMapping("/api/analysis/tasks")
    public PageResult<AnalysisTaskHistoryItem> listTasks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "8") int size
    ) {
        return analysisTaskService.listTasks(page, size);
    }

    @DeleteMapping("/api/analysis/tasks/{taskId}")
    public void deleteTask(@PathVariable String taskId) {
        analysisTaskService.deleteTask(taskId);
    }

    @DeleteMapping("/api/analysis/tasks")
    public void deleteAllTasks() {
        analysisTaskService.deleteAllTasks();
    }

    @GetMapping("/api/analysis/tasks/{taskId}")
    public AnalysisTaskResponse getTask(@PathVariable String taskId) {
        return analysisTaskService.getTask(taskId);
    }

    @GetMapping("/api/analysis/reports/{reportId}")
    public TacticalReport getReport(@PathVariable String reportId) {
        return analysisTaskService.getReport(reportId);
    }

    @GetMapping("/api/agent/traces/{taskId}")
    public List<AgentTraceLog> listTraces(@PathVariable String taskId) {
        return analysisTaskService.listTraces(taskId);
    }
}
