package com.mm_mk.Messaging.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Component
public class RateLimitProperties {

    private boolean enabled = true;
    private DefaultLimits defaults = new DefaultLimits();
    private Map<String, EndpointLimit> endpoints = new HashMap<>();

    @Getter
    @Setter
    public static class DefaultLimits {
        private int requests = 60;
        private Duration window = Duration.ofMinutes(1);
    }

    @Getter
    @Setter
    public static class EndpointLimit {
        private int requests = 60;
        private Duration window = Duration.ofMinutes(1);
        private boolean enabled = true;
    }

    public RateLimitProperties() {
        defaults.setRequests(60);
        defaults.setWindow(Duration.ofMinutes(1));

        // POST /api/messages
        EndpointLimit sendMessage = new EndpointLimit();
        sendMessage.setRequests(15);
        sendMessage.setWindow(Duration.ofMinutes(1));
        endpoints.put("/api/messages", sendMessage);

        // GET /api/messages/rooms/{roomCode} - fetching messages (can be frequent)
        EndpointLimit getMessages = new EndpointLimit();
        getMessages.setRequests(30);
        getMessages.setWindow(Duration.ofMinutes(1));
        endpoints.put("/api/messages/rooms/*", getMessages);


        // --- WebSocket Endpoints (/ws/**) ---

        // Message send over WebSocket (/rooms/{roomCode}/sendMessage)
        EndpointLimit wsSendMessage = new EndpointLimit();
        wsSendMessage.setRequests(20);
        wsSendMessage.setWindow(Duration.ofMinutes(1));
        endpoints.put("/ws/rooms/*/sendMessage", wsSendMessage);

        // Key events (keyboard, typing indicators, etc.)
        EndpointLimit wsKeyEvent = new EndpointLimit();
        wsKeyEvent.setRequests(500);
        wsKeyEvent.setWindow(Duration.ofMinutes(1));
        endpoints.put("/ws/rooms/*/keyEvent", wsKeyEvent);

        // WebSocket join/leave events — usually lower rate
        EndpointLimit wsJoin = new EndpointLimit();
        wsJoin.setRequests(10);
        wsJoin.setWindow(Duration.ofMinutes(1));
        endpoints.put("/ws/rooms/*/join", wsJoin);

        EndpointLimit wsLeave = new EndpointLimit();
        wsLeave.setRequests(10);
        wsLeave.setWindow(Duration.ofMinutes(1));
        endpoints.put("/ws/rooms/*/leave", wsLeave);
    }
}
