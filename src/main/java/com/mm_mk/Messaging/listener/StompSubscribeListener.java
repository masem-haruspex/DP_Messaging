package com.mm_mk.Messaging.listener;

import com.mm_mk.Messaging.repository.LocalRoomRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@RequiredArgsConstructor
public class StompSubscribeListener {

    private static final Logger logger = LoggerFactory.getLogger(StompSubscribeListener.class);

    private final LocalRoomRepository localRoomRepository;

    // remembers which session already has room data stored
    private final ConcurrentMap<String, Boolean> sessionInitialized = new ConcurrentHashMap<>();

    @EventListener
    public void onSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor headers = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headers.getSessionId();
        String destination = headers.getDestination();

		logger.debug("SUBSCRIBE event – session:{}, dest:{}, X-User-ID:{}", sessionId, destination, headers.getFirstNativeHeader("X-User-ID"));

        if (destination == null || !destination.startsWith("/topic/rooms/")) {
            return;
        }

        String[] parts = destination.split("/"); // /topic/rooms/{code}/whatever
        if (parts.length < 4) return;
        String roomCode = parts[3];

        // store only once per physical session
        if (sessionInitialized.putIfAbsent(sessionId, Boolean.TRUE) != null) {
            return;
        }

        //String userIdStr = headers.getFirstNativeHeader("X-User-ID");
        //if (userIdStr == null) {
        //    logger.warn("SUBSCRIBE without X-User-ID – session {}", sessionId);
        //    return;
        //}
        String userIdStr = (String) headers.getSessionAttributes().get("X-User-ID");
        if (userIdStr == null) {
            logger.warn("SUBSCRIBE – no X-User-ID in session attributes");
            return;
        }

        UUID roomId = localRoomRepository.findByCode(roomCode)
                .map(r -> r.getId())
                .orElse(null);
        if (roomId == null) {
            logger.warn("Room {} not found locally – session {}", roomCode, sessionId);
            return;
        }

        headers.getSessionAttributes().put("roomCode", roomCode);
        headers.getSessionAttributes().put("userId",   userIdStr);
        headers.getSessionAttributes().put("roomId",   roomId.toString());

        logger.info("Stored session data – session:{}, room:{}, user:{}", sessionId, roomCode, userIdStr);
    }
}
