package com.mm_mk.Messaging.controller;

import com.mm_mk.Messaging.repository.LocalRoomRepository;
import com.mm_mk.Messaging.request.SendMessageRequest;
import com.mm_mk.Messaging.response.MessageResponse;
import com.mm_mk.Messaging.service.MessagingService;
import com.mm_mk.Messaging.util.CorrelationIdUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.util.Map;
import java.util.UUID;

@Controller
@Tag(name = "WebSocket", description = "Real-time WebSocket endpoints for live messaging")
public class WebSocketController {

	private static final Logger logger = LoggerFactory.getLogger(WebSocketController.class);
	private static final long SLOW_OPERATION_THRESHOLD_MS = 1000;

	private final MessagingService messagingService;
	private final SimpMessagingTemplate messagingTemplate;
	private final LocalRoomRepository localRoomRepository;
	private final RabbitTemplate rabbitTemplate;

	@Value("${rabbitmq.exchange.rooms}")
	private String roomsExchange;

	@Value("${rabbitmq.routingkey.user.left}")
	private String userLeftRoutingKey;

	public WebSocketController(MessagingService messagingService,
			SimpMessagingTemplate messagingTemplate, LocalRoomRepository localRoomRepository, RabbitTemplate rabbitTemplate) {
		this.messagingService = messagingService;
		this.messagingTemplate = messagingTemplate;
		this.localRoomRepository = localRoomRepository;
		this.rabbitTemplate = rabbitTemplate;
	}

	@MessageMapping("/rooms/{roomCode}/sendMessage")
	public void handleChatMessage(@DestinationVariable String roomCode,
			SendMessageRequest request,
			@Header("simpSessionAttributes") Map<String, Object> sessionAttributes) {

			String userIdStr = (String) sessionAttributes.get("userId");
			if (userIdStr == null) {
				logger.error("No userId in session attributes");
				return;
			}
			UUID userId = UUID.fromString(userIdStr);

			long startTime = System.currentTimeMillis();
			logger.debug("WebSocket CHAT_MESSAGE - roomCode: {}, userId: {}, contentLength: {}",
					roomCode, userId, request.content().length());

			try {
				MessageResponse savedMessage = messagingService.sendMessage(roomCode, userId, request.content());

				messagingTemplate.convertAndSend(
						"/topic/rooms/" + roomCode + "/chat",
						Map.of(
							"type", "CHAT_MESSAGE",
							"payload", savedMessage,
							"timestamp", System.currentTimeMillis()
							)
						);

				logger.debug("WebSocket CHAT_MESSAGE broadcast successful - roomCode: {}, messageId: {}",
						roomCode, savedMessage.id());
			} catch (Exception e) {
				logger.error("Failed to handle chat message for room {} from user {}: {}",
						roomCode, userId, e.getMessage(), e);

				messagingTemplate.convertAndSendToUser(
						userId.toString(),
						"/queue/errors",
						Map.of(
							"type", "MESSAGE_SEND_ERROR",
							"error", "Failed to send message",
							"timestamp", System.currentTimeMillis()
							)
						);
			} finally {
				long duration = System.currentTimeMillis() - startTime;
				logger.debug("WebSocket CHAT_MESSAGE processing completed in {}ms", duration);
				if (duration > SLOW_OPERATION_THRESHOLD_MS) {
					logger.warn("SLOW WEBSOCKET: CHAT_MESSAGE took {}ms", duration);
				}
			}
	}

	@MessageMapping("/rooms/{roomCode}/keyEvent")
	@Operation(summary = "Send keyboard event", description = "Broadcast real-time keyboard events to room participants")
	public void handleKeyEvent(@DestinationVariable String roomCode,
                           Map<String, Object> keyEvent,
                           @Header("simpSessionAttributes") Map<String, Object> sessionAttributes) {

    String userIdStr = (String) sessionAttributes.get("userId");
    UUID userId = userIdStr != null ? UUID.fromString(userIdStr) : null;

		long startTime = System.currentTimeMillis();
		logger.debug("WebSocket KEY_EVENT - roomCode: {}, userId: {}, eventType: {}", roomCode, userId, keyEvent.get("type"));

		try {
			messagingTemplate.convertAndSend(
					"/topic/rooms/" + roomCode + "/keyEvents",
					Map.of(
						"type", "KEY_EVENT",
						"userId", userId.toString(),
						"payload", keyEvent,
						"timestamp", System.currentTimeMillis()
						)
					);

			logger.debug("WebSocket KEY_EVENT broadcast successful - roomCode: {}, userId: {}", roomCode, userId);
		} finally {
			long duration = System.currentTimeMillis() - startTime;
			logger.debug("WebSocket KEY_EVENT processing completed in {}ms", duration);
			if (duration > SLOW_OPERATION_THRESHOLD_MS) logger.warn("SLOW WEBSOCKET: KEY_EVENT took {}ms", duration);
		}
	}

	@MessageMapping("/rooms/{roomCode}/join")
	@Operation(summary = "Join room via WebSocket", description = "Notify room when user joins via WebSocket")
	public void handleUserJoin(@DestinationVariable String roomCode,
                           @Header("simpSessionAttributes") Map<String, Object> sessionAttributes) {

    String userIdStr = (String) sessionAttributes.get("userId");
    UUID userId = userIdStr != null ? UUID.fromString(userIdStr) : null;

			long startTime = System.currentTimeMillis();
			logger.info("WebSocket USER_JOIN - roomCode: {}, userId: {}", roomCode, userId);

			logger.debug("WebSocket USER_JOIN broadcast successful - roomCode: {}, userId: {}", roomCode, userId);
			long duration = System.currentTimeMillis() - startTime;
			logger.debug("WebSocket USER_JOIN processing completed in {}ms", duration);
			if (duration > SLOW_OPERATION_THRESHOLD_MS) {
				logger.warn("SLOW WEBSOCKET: USER_JOIN took {}ms", duration);
			}
	}

	@MessageMapping("/rooms/{roomCode}/leave")
	public void handleUserLeave(@DestinationVariable String roomCode,
                            @Header("simpSessionAttributes") Map<String, Object> sessionAttributes) {

    String userIdStr = (String) sessionAttributes.get("userId");

		logger.info("WebSocket USER_LEAVE – roomCode: {}, userId: {}", roomCode, userIdStr);

		UUID roomId = localRoomRepository.findByCode(roomCode)
			.map(r -> r.getId())
			.orElseThrow(() -> new RuntimeException("Room not found"));

		rabbitTemplate.convertAndSend(
				roomsExchange,
				userLeftRoutingKey,
				Map.of("roomId", roomId.toString(), "userId", userIdStr),
				m -> {
					m.getMessageProperties().setHeader("X-Correlation-ID", CorrelationIdUtil.getCorrelationId());
					return m;
				});
	}

}
