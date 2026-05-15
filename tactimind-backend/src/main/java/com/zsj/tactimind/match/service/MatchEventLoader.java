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
    private final Path eventsFile;

    public MatchEventLoader(
            ObjectMapper objectMapper,
            @Value("${tactimind.match.events-file}") String eventsFile
    ) {
        this.objectMapper = objectMapper;
        this.eventsFile = Path.of(eventsFile);
    }

    public List<MatchEvent> loadEvents() {
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
            throw new IllegalStateException("Failed to load match events from " + eventsFile.toAbsolutePath(), e);
        }
    }
}
