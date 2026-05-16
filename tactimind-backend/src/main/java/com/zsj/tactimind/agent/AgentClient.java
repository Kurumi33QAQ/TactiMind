package com.zsj.tactimind.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zsj.tactimind.agent.model.AgentAnalyzeRequest;
import com.zsj.tactimind.agent.model.AgentAnalyzeResponse;
import com.zsj.tactimind.catalog.model.MatchTacticalProfile;
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
    public AgentAnalyzeResponse analyze(
            MatchState state,
            List<MatchEvent> recentEvents,
            MatchTacticalProfile tacticalProfile
    ) {
        try {
            AgentAnalyzeRequest request = new AgentAnalyzeRequest(state, recentEvents, tacticalProfile);
            String requestBody = objectMapper.writeValueAsString(request);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            log.info("Calling Python Agent, url={}, minute={}, recentEvents={}, hasProfile={}, bodyLength={}",
                    analyzeUrl, state.getCurrentMinute(), recentEvents.size(), tacticalProfile != null, requestBody.length());

            String responseBody = restTemplate.postForObject(
                    analyzeUrl,
                    new HttpEntity<>(requestBody, headers),
                    String.class
            );

            AgentAnalyzeResponse response = objectMapper.readValue(responseBody, AgentAnalyzeResponse.class);
            if (response == null || response.analyses() == null) {
                log.warn("Python Agent returned empty response, minute={}", state.getCurrentMinute());
                return emptyResponse(state);
            }

            log.info("Python Agent returned {} insights and {} analyses, minute={}",
                    response.dataInsights() == null ? 0 : response.dataInsights().size(),
                    response.analyses().size(),
                    state.getCurrentMinute());
            return response;
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize or parse Agent JSON, reason={}", e.getMessage());
            return emptyResponse(state);
        } catch (RestClientException e) {
            log.warn("Failed to call Python Agent, url={}, reason={}", analyzeUrl, e.getMessage());
            return emptyResponse(state);
        }
    }

    private AgentAnalyzeResponse emptyResponse(MatchState state) {
        return new AgentAnalyzeResponse(state.getCurrentMinute(), List.of(), List.of());
    }
}
