package com.zsj.tactimind.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zsj.tactimind.agent.model.AgentAnalyzeRequest;
import com.zsj.tactimind.agent.model.AgentAnalyzeResponse;
import com.zsj.tactimind.agent.model.TacticalAnalysis;
import com.zsj.tactimind.match.model.MatchEvent;
import com.zsj.tactimind.match.model.MatchState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
public class AgentClient {
    private static final Logger log = LoggerFactory.getLogger(AgentClient.class);

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;
    private final String analyzeUrl;

    public AgentClient(
            ObjectMapper objectMapper,
            @Value("${tactimind.agent.base-url}") String agentBaseUrl
    ) {
        this.objectMapper = objectMapper;
        this.analyzeUrl = agentBaseUrl + "/analyze";
    }

    /*
     * 调用 Python Agent 服务。
     * 如果 Agent 暂时不可用，返回空列表，让比赛模拟继续运行。
     */
    public List<TacticalAnalysis> analyze(MatchState state, List<MatchEvent> recentEvents) {
        try {
            AgentAnalyzeRequest request = new AgentAnalyzeRequest(state, recentEvents);
            String requestBody = objectMapper.writeValueAsString(request);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            log.info("Calling Python Agent, url={}, minute={}, recentEvents={}, bodyLength={}",
                    analyzeUrl, state.getCurrentMinute(), recentEvents.size(), requestBody.length());

            String responseBody = restTemplate.postForObject(
                    analyzeUrl,
                    new HttpEntity<>(requestBody, headers),
                    String.class
            );

            AgentAnalyzeResponse response = objectMapper.readValue(responseBody, AgentAnalyzeResponse.class);
            if (response == null || response.analyses() == null) {
                log.warn("Python Agent returned empty response, minute={}", state.getCurrentMinute());
                return List.of();
            }

            log.info("Python Agent returned {} analyses, minute={}", response.analyses().size(), state.getCurrentMinute());
            return response.analyses();
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize or parse Agent JSON, reason={}", e.getMessage());
            return List.of();
        } catch (RestClientException e) {
            log.warn("Failed to call Python Agent, url={}, reason={}", analyzeUrl, e.getMessage());
            return List.of();
        }
    }
}
