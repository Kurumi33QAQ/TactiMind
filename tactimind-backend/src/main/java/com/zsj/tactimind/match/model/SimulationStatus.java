package com.zsj.tactimind.match.model;

public record SimulationStatus(
        boolean running,
        boolean finished,
        int eventCursor,
        int totalEvents,
        int currentMinute,
        double speed,
        int finalMinute
) {
}
