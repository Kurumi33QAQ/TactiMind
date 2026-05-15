package com.zsj.tactimind.analysis.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zsj.tactimind.agent.model.TacticalAnalysis;
import com.zsj.tactimind.analysis.model.AgentTraceLog;
import com.zsj.tactimind.analysis.model.AnalysisTaskHistoryItem;
import com.zsj.tactimind.analysis.model.AnalysisTaskResponse;
import com.zsj.tactimind.analysis.model.AnalysisTaskStatus;
import com.zsj.tactimind.analysis.model.TacticalReport;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@ConditionalOnProperty(name = "tactimind.mysql.enabled", havingValue = "true")
public class MysqlAnalysisPersistenceService extends AnalysisPersistenceFacade {
    private static final Logger log = LoggerFactory.getLogger(MysqlAnalysisPersistenceService.class);

    private final ObjectMapper objectMapper;
    private final String url;
    private final String username;
    private final String password;

    public MysqlAnalysisPersistenceService(
            ObjectMapper objectMapper,
            @Value("${tactimind.mysql.url}") String url,
            @Value("${tactimind.mysql.username}") String username,
            @Value("${tactimind.mysql.password}") String password
    ) {
        this.objectMapper = objectMapper;
        this.url = url;
        this.username = username;
        this.password = password;
    }

    @PostConstruct
    public void initTables() {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS analysis_task (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        task_id VARCHAR(64) NOT NULL UNIQUE,
                        match_id VARCHAR(128) NOT NULL,
                        analysis_type VARCHAR(64) NOT NULL,
                        status VARCHAR(32) NOT NULL,
                        current_step VARCHAR(128) NOT NULL,
                        progress INT NOT NULL,
                        report_id VARCHAR(128) NOT NULL,
                        created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS agent_trace_log (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        task_id VARCHAR(64) NOT NULL,
                        step_name VARCHAR(128) NOT NULL,
                        agent_name VARCHAR(64) NOT NULL,
                        tool_name VARCHAR(128) NOT NULL,
                        status VARCHAR(32) NOT NULL,
                        input_summary TEXT,
                        output_summary TEXT,
                        cost_time_ms BIGINT NOT NULL,
                        error_message TEXT,
                        created_at TIMESTAMP NOT NULL,
                        INDEX idx_trace_task_id (task_id)
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS tactical_report (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        report_id VARCHAR(128) NOT NULL UNIQUE,
                        task_id VARCHAR(64) NOT NULL,
                        match_id VARCHAR(128) NOT NULL,
                        summary TEXT NOT NULL,
                        conclusions JSON,
                        suggestions JSON,
                        risks JSON,
                        data_sources JSON,
                        simulated_data BOOLEAN NOT NULL,
                        created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                    )
                    """);
        } catch (SQLException e) {
            log.warn("Failed to initialize analysis tables, reason={}", e.getMessage());
        }
    }

    @Override
    public void saveTask(AnalysisTaskResponse task, String matchId, String analysisType) {
        String sql = """
                INSERT INTO analysis_task(task_id, match_id, analysis_type, status, current_step, progress, report_id)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    status = VALUES(status),
                    current_step = VALUES(current_step),
                    progress = VALUES(progress),
                    report_id = VALUES(report_id),
                    updated_time = CURRENT_TIMESTAMP
                """;
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, task.taskId());
            statement.setString(2, matchId);
            statement.setString(3, analysisType);
            statement.setString(4, task.status().name());
            statement.setString(5, task.currentStep());
            statement.setInt(6, task.progress());
            statement.setString(7, task.reportId());
            statement.executeUpdate();
        } catch (SQLException e) {
            log.warn("Failed to save analysis task, taskId={}, reason={}", task.taskId(), e.getMessage());
        }
    }

    @Override
    public void saveTraces(String taskId, List<AgentTraceLog> traces) {
        String deleteSql = "DELETE FROM agent_trace_log WHERE task_id = ?";
        String insertSql = """
                INSERT INTO agent_trace_log(
                    task_id, step_name, agent_name, tool_name, status,
                    input_summary, output_summary, cost_time_ms, error_message, created_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = getConnection();
             PreparedStatement deleteStatement = connection.prepareStatement(deleteSql);
             PreparedStatement insertStatement = connection.prepareStatement(insertSql)) {
            deleteStatement.setString(1, taskId);
            deleteStatement.executeUpdate();

            for (AgentTraceLog trace : traces) {
                insertStatement.setString(1, taskId);
                insertStatement.setString(2, trace.stepName());
                insertStatement.setString(3, trace.agentName());
                insertStatement.setString(4, trace.toolName());
                insertStatement.setString(5, trace.status());
                insertStatement.setString(6, trace.inputSummary());
                insertStatement.setString(7, trace.outputSummary());
                insertStatement.setLong(8, trace.costTimeMs());
                insertStatement.setString(9, trace.errorMessage());
                insertStatement.setTimestamp(10, Timestamp.from(trace.createdAt()));
                insertStatement.addBatch();
            }
            insertStatement.executeBatch();
        } catch (SQLException e) {
            log.warn("Failed to save agent traces, taskId={}, reason={}", taskId, e.getMessage());
        }
    }

    @Override
    public void saveReport(TacticalReport report) {
        String sql = """
                INSERT INTO tactical_report(
                    report_id, task_id, match_id, summary, conclusions,
                    suggestions, risks, data_sources, simulated_data
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    summary = VALUES(summary),
                    conclusions = VALUES(conclusions),
                    suggestions = VALUES(suggestions),
                    risks = VALUES(risks),
                    data_sources = VALUES(data_sources),
                    simulated_data = VALUES(simulated_data),
                    updated_time = CURRENT_TIMESTAMP
                """;
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, report.reportId());
            statement.setString(2, report.taskId());
            statement.setString(3, report.matchId());
            statement.setString(4, report.summary());
            statement.setString(5, objectMapper.writeValueAsString(report.conclusions()));
            statement.setString(6, objectMapper.writeValueAsString(report.suggestions()));
            statement.setString(7, objectMapper.writeValueAsString(report.risks()));
            statement.setString(8, objectMapper.writeValueAsString(report.dataSources()));
            statement.setBoolean(9, report.simulatedData());
            statement.executeUpdate();
        } catch (SQLException | JsonProcessingException e) {
            log.warn("Failed to save tactical report, reportId={}, reason={}", report.reportId(), e.getMessage());
        }
    }

    @Override
    public Optional<AnalysisTaskResponse> findTask(String taskId) {
        String sql = """
                SELECT task_id, status, current_step, progress, report_id
                FROM analysis_task
                WHERE task_id = ?
                """;
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, taskId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(new AnalysisTaskResponse(
                            resultSet.getString("task_id"),
                            AnalysisTaskStatus.valueOf(resultSet.getString("status")),
                            resultSet.getString("current_step"),
                            resultSet.getInt("progress"),
                            resultSet.getString("report_id")
                    ));
                }
            }
        } catch (SQLException e) {
            log.warn("Failed to find analysis task, taskId={}, reason={}", taskId, e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<AnalysisTaskHistoryItem> listTasks() {
        String sql = """
                SELECT task_id, match_id, analysis_type, status, current_step, progress, report_id, created_time
                FROM analysis_task
                ORDER BY created_time DESC, id DESC
                """;
        List<AnalysisTaskHistoryItem> result = new ArrayList<>();
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                result.add(new AnalysisTaskHistoryItem(
                        resultSet.getString("task_id"),
                        resultSet.getString("match_id"),
                        resultSet.getString("analysis_type"),
                        AnalysisTaskStatus.valueOf(resultSet.getString("status")),
                        resultSet.getString("current_step"),
                        resultSet.getInt("progress"),
                        resultSet.getString("report_id"),
                        toInstant(resultSet.getTimestamp("created_time"))
                ));
            }
        } catch (SQLException e) {
            log.warn("Failed to list analysis tasks, reason={}", e.getMessage());
        }
        return result;
    }

    @Override
    public List<AgentTraceLog> listTraces(String taskId) {
        String sql = """
                SELECT step_name, agent_name, tool_name, status, input_summary,
                       output_summary, cost_time_ms, error_message, created_at
                FROM agent_trace_log
                WHERE task_id = ?
                ORDER BY id
                """;
        List<AgentTraceLog> result = new ArrayList<>();
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, taskId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    result.add(new AgentTraceLog(
                            resultSet.getString("step_name"),
                            resultSet.getString("agent_name"),
                            resultSet.getString("tool_name"),
                            resultSet.getString("status"),
                            resultSet.getString("input_summary"),
                            resultSet.getString("output_summary"),
                            resultSet.getLong("cost_time_ms"),
                            resultSet.getString("error_message"),
                            toInstant(resultSet.getTimestamp("created_at"))
                    ));
                }
            }
        } catch (SQLException e) {
            log.warn("Failed to list agent traces, taskId={}, reason={}", taskId, e.getMessage());
        }
        return result;
    }

    @Override
    public Optional<TacticalReport> findReport(String reportId) {
        String sql = """
                SELECT report_id, task_id, match_id, summary, conclusions,
                       suggestions, risks, data_sources, simulated_data
                FROM tactical_report
                WHERE report_id = ?
                """;
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, reportId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(new TacticalReport(
                            resultSet.getString("report_id"),
                            resultSet.getString("task_id"),
                            resultSet.getString("match_id"),
                            resultSet.getString("summary"),
                            readList(resultSet.getString("conclusions"), new TypeReference<List<TacticalAnalysis>>() {
                            }),
                            readList(resultSet.getString("suggestions"), new TypeReference<List<String>>() {
                            }),
                            readList(resultSet.getString("risks"), new TypeReference<List<String>>() {
                            }),
                            readList(resultSet.getString("data_sources"), new TypeReference<List<String>>() {
                            }),
                            resultSet.getBoolean("simulated_data")
                    ));
                }
            }
        } catch (SQLException | JsonProcessingException e) {
            log.warn("Failed to find tactical report, reportId={}, reason={}", reportId, e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public void deleteTask(String taskId) {
        String deleteReportSql = "DELETE FROM tactical_report WHERE task_id = ?";
        String deleteTraceSql = "DELETE FROM agent_trace_log WHERE task_id = ?";
        String deleteTaskSql = "DELETE FROM analysis_task WHERE task_id = ?";
        try (Connection connection = getConnection();
             PreparedStatement deleteReport = connection.prepareStatement(deleteReportSql);
             PreparedStatement deleteTrace = connection.prepareStatement(deleteTraceSql);
             PreparedStatement deleteTask = connection.prepareStatement(deleteTaskSql)) {
            deleteReport.setString(1, taskId);
            deleteReport.executeUpdate();
            deleteTrace.setString(1, taskId);
            deleteTrace.executeUpdate();
            deleteTask.setString(1, taskId);
            deleteTask.executeUpdate();
        } catch (SQLException e) {
            log.warn("Failed to delete analysis task, taskId={}, reason={}", taskId, e.getMessage());
        }
    }

    @Override
    public void deleteAllTasks() {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM tactical_report");
            statement.executeUpdate("DELETE FROM agent_trace_log");
            statement.executeUpdate("DELETE FROM analysis_task");
        } catch (SQLException e) {
            log.warn("Failed to delete all analysis tasks, reason={}", e.getMessage());
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? Instant.now() : timestamp.toInstant();
    }

    private <T> T readList(String json, TypeReference<T> typeReference) throws JsonProcessingException {
        return objectMapper.readValue(json, typeReference);
    }
}
