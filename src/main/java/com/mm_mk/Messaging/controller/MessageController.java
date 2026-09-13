package com.mm_mk.Messaging.controller;

import com.mm_mk.Messaging.util.RateLimiter;
import com.mm_mk.Messaging.request.SendMessageRequest;
import com.mm_mk.Messaging.response.MessageResponse;
import com.mm_mk.Messaging.service.MessagingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/messages")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
@Tag(name = "Messaging", description = "Message management and real-time chat operations")
public class MessageController {

	private static final Logger logger = LoggerFactory.getLogger(MessageController.class);
	private static final long SLOW_OPERATION_THRESHOLD_MS = 2000;

	@Autowired
	private MessagingService messagingService;

	@Autowired
	private RateLimiter rateLimiter;


	@PostMapping
	@Operation(summary = "Send a message", description = "Send a text message to a specific room")
	@ApiResponse(responseCode = "201", description = "Message sent successfully")
	@ApiResponse(responseCode = "400", description = "Invalid input data")
	@ApiResponse(responseCode = "404", description = "Room or user not found")
	@ApiResponse(responseCode = "429", description = "Rate limit exceeded - max 1000 characters per 10 seconds")
	public ResponseEntity<MessageResponse> sendMessage(
			@RequestHeader("X-User-ID") UUID userId,
			@Valid @RequestBody SendMessageRequest request) {

			if (!rateLimiter.isAllowed(userId.toString(), request.content().length())) {
				logger.warn("Rate limit exceeded for user: {} (tried to send {} chars)",
						userId, request.content().length());
				return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
			}

			long startTime = System.currentTimeMillis();
			logger.info("SEND_MESSAGE request - userId: {}, roomCode: {}, contentLength: {}",
					userId, request.roomCode(), request.content().length());

			try {
				MessageResponse message = messagingService.sendMessage(
						request.roomCode(),
						userId,
						request.content()
						);

				logger.info("SEND_MESSAGE success - messageId: {}, roomCode: {}, userId: {}",
						message.id(), request.roomCode(), userId);
				return ResponseEntity.status(201).body(message);
			} finally {
				long duration = System.currentTimeMillis() - startTime;
				logger.info("SEND_MESSAGE API call completed in {}ms", duration);
				if (duration > SLOW_OPERATION_THRESHOLD_MS) {
					logger.warn("SLOW API: SEND_MESSAGE took {}ms", duration);
				}
			}
			}

	@GetMapping("/rooms/{roomCode}")
	@Operation(summary = "Get room messages", description = "Retrieve all messages from a specific room")
	@ApiResponse(responseCode = "200", description = "Messages retrieved successfully")
	@ApiResponse(responseCode = "404", description = "Room not found")
	public ResponseEntity<List<MessageResponse>> getMessages(@PathVariable String roomCode) {
		long startTime = System.currentTimeMillis();
		logger.debug("GET_MESSAGES request - roomCode: {}", roomCode);

		try {
			List<MessageResponse> messages = messagingService.getMessages(roomCode);

			logger.debug("GET_MESSAGES success - roomCode: {}, messageCount: {}", roomCode, messages.size());
			return ResponseEntity.ok(messages);
		} finally {
			long duration = System.currentTimeMillis() - startTime;
			logger.debug("GET_MESSAGES API call completed in {}ms", duration);
			if (duration > SLOW_OPERATION_THRESHOLD_MS) {
				logger.warn("SLOW API: GET_MESSAGES took {}ms", duration);
			}
		}
	}
}
