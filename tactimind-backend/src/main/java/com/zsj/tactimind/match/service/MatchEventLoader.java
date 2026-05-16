package com.zsj.tactimind.match.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zsj.tactimind.match.model.MatchEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

@Service
public class MatchEventLoader {
    private final ObjectMapper objectMapper;
    private final Path defaultEventsFile;

    public MatchEventLoader(
            ObjectMapper objectMapper,
            @Value("${tactimind.match.events-file}") String eventsFile
    ) {
        this.objectMapper = objectMapper;
        this.defaultEventsFile = Path.of(eventsFile);
    }

    public List<MatchEvent> loadEvents() {
        return loadEvents(defaultEventsFile);
    }

    public List<MatchEvent> loadEvents(String eventsFilePath) {
        if (eventsFilePath == null || eventsFilePath.isBlank()) {
            return loadEvents(defaultEventsFile);
        }
        return loadEvents(Path.of(eventsFilePath));
    }

    private List<MatchEvent> loadEvents(Path eventsFile) {
        try {
            List<MatchEvent> events = objectMapper.readValue(
                    eventsFile.toFile(),
                    new TypeReference<>() {
                    }
            );
            return events.stream()
                    .sorted(Comparator.comparingInt(MatchEvent::getMinute))
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("加载比赛事件流失败，文件路径=" + eventsFile.toAbsolutePath(), e);
        }
    }
}
