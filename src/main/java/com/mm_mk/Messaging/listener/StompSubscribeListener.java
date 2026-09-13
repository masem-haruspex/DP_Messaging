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

	@EventListener
	public void onSubscribe(SessionSubscribeEvent event) {
		StompHeaderAccessor headers = StompHeaderAccessor.wrap(event.getMessage());
		String sessionId = headers.getSessionId();
		String destination = headers.getDestination();

		logger.debug("SUBSCRIBE event – session:{}, dest:{}", sessionId, destination);

		if (destination == null || !destination.startsWith("/topic/rooms/")) {
			return;
		}

		String[] parts = destination.split("/");
		if (parts.length < 4) return;
		String roomCode = parts[3];

		String userIdStr = (String) headers.getSessionAttributes().get("userId");
		if (userIdStr == null) {
			logger.warn("SUBSCRIBE without userId in session – session {}", sessionId);
			return;
		}

		localRoomRepository.findByCode(roomCode).ifPresent(room -> {
			headers.getSessionAttributes().put("roomCode", roomCode);
			headers.getSessionAttributes().put("roomId", room.getId().toString());
			logger.info("Stored session data – session:{}, room:{}, user:{}", sessionId, roomCode, userIdStr);
		});
	}
}
