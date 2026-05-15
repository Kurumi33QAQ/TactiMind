package com.zsj.tactimind.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zsj.tactimind.agent.model.TacticalAnalysis;
import com.zsj.tactimind.match.model.MatchEvent;
import com.zsj.tactimind.match.model.MatchState;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@ConditionalOnProperty(name = "tactimind.mysql.enabled", havingValue = "true")
public class MysqlMatchPersistenceService extends MatchPersistenceFacade {
    private static final Logger log = LoggerFactory.getLogger(MysqlMatchPersistenceService.class);

    private final ObjectMapper objectMapper;
    private final String url;
    private final String username;
    private final String password;

    public MysqlMatchPersistenceService(
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
                    CREATE TABLE IF NOT EXISTS match_info (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        match_id VARCHAR(64) NOT NULL UNIQUE,
                        match_name VARCHAR(128) NOT NULL,
                        home_team VARCHAR(64) NOT NULL,
                        away_team VARCHAR(64) NOT NULL,
                        status VARCHAR(32) NOT NULL,
                        created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS match_event (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        match_id VARCHAR(64) NOT NULL,
                        event_index INT NOT NULL,
                        minute INT NOT NULL,
                        team VARCHAR(64),
                        event_type VARCHAR(64),
                        player VARCHAR(64),
                        description TEXT,
                        raw_data JSON,
                        created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        UNIQUE KEY uk_match_event_index (match_id, event_index)
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS match_analysis (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        match_id VARCHAR(64) NOT NULL,
                        minute INT NOT NULL,
                        conclusion TEXT NOT NULL,
                        evidence JSON,
                        confidence DOUBLE NOT NULL,
                        risk_level VARCHAR(32) NOT NULL,
                        created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
        } catch (SQLException e) {
            log.warn("Failed to initialize MySQL tables, reason={}", e.getMessage());
        }
    }

    @Override
    public void saveMatchInfo(MatchState state) {
        String sql = """
                INSERT INTO match_info(match_id, match_name, home_team, away_team, status)
                VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE status = VALUES(status), updated_time = CURRENT_TIMESTAMP
                """;
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, state.getMatchId());
            statement.setString(2, "TactiMind Demo Match");
            statement.setString(3, "Team A");
            statement.setString(4, "Team B");
            statement.setString(5, statusOf(state));
            statement.executeUpdate();
        } catch (SQLException e) {
            log.warn("Failed to save match info, matchId={}, reason={}", state.getMatchId(), e.getMessage());
        }
    }

    @Override
    public void clearMatchData(String matchId) {
        try (Connection connection = getConnection();
             PreparedStatement deleteAnalysis = connection.prepareStatement("DELETE FROM match_analysis WHERE match_id = ?");
             PreparedStatement deleteEvents = connection.prepareStatement("DELETE FROM match_event WHERE match_id = ?")) {
            // 重置比赛时清空旧事件和旧分析，避免多次演示后赛后报告混入历史数据。
            deleteAnalysis.setString(1, matchId);
            deleteAnalysis.executeUpdate();
            deleteEvents.setString(1, matchId);
            deleteEvents.executeUpdate();
        } catch (SQLException e) {
            log.warn("Failed to clear match data, matchId={}, reason={}", matchId, e.getMessage());
        }
    }

    @Override
    public void saveEvent(String matchId, int eventIndex, MatchEvent event) {
        String sql = """
                INSERT INTO match_event(match_id, event_index, minute, team, event_type, player, description, raw_data)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    minute = VALUES(minute),
                    team = VALUES(team),
                    event_type = VALUES(event_type),
                    player = VALUES(player),
                    description = VALUES(description),
                    raw_data = VALUES(raw_data)
                """;
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, matchId);
            statement.setInt(2, eventIndex);
            statement.setInt(3, event.getMinute());
            statement.setString(4, event.getTeam());
            statement.setString(5, event.getType() == null ? null : event.getType().name());
            statement.setString(6, event.getPlayer());
            statement.setString(7, event.getDescription());
            statement.setString(8, objectMapper.writeValueAsString(event));
            statement.executeUpdate();
        } catch (SQLException | JsonProcessingException e) {
            log.warn("Failed to save match event, matchId={}, eventIndex={}, reason={}", matchId, eventIndex, e.getMessage());
        }
    }

    @Override
    public void saveAnalysis(String matchId, TacticalAnalysis analysis) {
        String sql = """
                INSERT INTO match_analysis(match_id, minute, conclusion, evidence, confidence, risk_level)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, matchId);
            statement.setInt(2, analysis.minute());
            statement.setString(3, analysis.conclusion());
            statement.setString(4, objectMapper.writeValueAsString(analysis.evidence()));
            statement.setDouble(5, analysis.confidence());
            statement.setString(6, analysis.riskLevel());
            statement.executeUpdate();
        } catch (SQLException | JsonProcessingException e) {
            log.warn("Failed to save analysis, matchId={}, minute={}, reason={}", matchId, analysis.minute(), e.getMessage());
        }
    }

    @Override
    public List<MatchEvent> listEvents(String matchId) {
        String sql = "SELECT raw_data FROM match_event WHERE match_id = ? ORDER BY event_index";
        List<MatchEvent> events = new ArrayList<>();
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, matchId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    events.add(objectMapper.readValue(resultSet.getString("raw_data"), MatchEvent.class));
                }
            }
        } catch (SQLException | JsonProcessingException e) {
            log.warn("Failed to list events, matchId={}, reason={}", matchId, e.getMessage());
        }
        return events;
    }

    @Override
    public List<TacticalAnalysis> listAnalyses(String matchId) {
        String sql = """
                SELECT minute, conclusion, evidence, confidence, risk_level
                FROM match_analysis
                WHERE match_id = ?
                ORDER BY minute, id
                """;
        List<TacticalAnalysis> analyses = new ArrayList<>();
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, matchId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    List<String> evidence = objectMapper.readValue(
                            resultSet.getString("evidence"),
                            new TypeReference<List<String>>() {
                            }
                    );
                    analyses.add(new TacticalAnalysis(
                            resultSet.getInt("minute"),
                            resultSet.getString("conclusion"),
                            evidence,
                            resultSet.getDouble("confidence"),
                            resultSet.getString("risk_level")
                    ));
                }
            }
        } catch (SQLException | JsonProcessingException e) {
            log.warn("Failed to list analyses, matchId={}, reason={}", matchId, e.getMessage());
        }
        return analyses;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    private String statusOf(MatchState state) {
        if (state.isFinished()) {
            return "FINISHED";
        }
        if (state.isRunning()) {
            return "RUNNING";
        }
        return "PAUSED";
    }
}
