package com.zsj.tactimind.websocket;

import java.time.Instant;

public record WsMessage(String messageType, Object payload, Instant timestamp) {
    public static WsMessage of(String messageType, Object payload) {
        return new WsMessage(messageType, payload, Instant.now());
    }
}
