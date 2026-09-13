package com.mm_mk.Messaging.config;

import com.mm_mk.Messaging.repository.LocalRoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.UUID;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	private static final Logger logger = LoggerFactory.getLogger(WebSocketConfig.class);
	private final TaskScheduler taskScheduler;
	private final JwtDecoder jwtDecoder;
	private final LocalRoomRepository localRoomRepository;

	public WebSocketConfig(JwtDecoder jwtDecoder, LocalRoomRepository localRoomRepository, TaskScheduler taskScheduler) {
		this.jwtDecoder = jwtDecoder;
		this.localRoomRepository = localRoomRepository;
		this.taskScheduler = taskScheduler;
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry config) {
		config.enableSimpleBroker("/topic")
			.setHeartbeatValue(new long[]{10000, 10000})
			.setTaskScheduler(taskScheduler);

		config.setApplicationDestinationPrefixes("/app");
		config.setUserDestinationPrefix("/user");
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/ws")
			.setAllowedOrigins(
					"http://localhost:5173",
					"http://127.0.0.1:5173",
					"https://shimmering-tapioca-0c6bfd.netlify.app",
					"https://duopiano.masemharuspex.com"
					)
			.withSockJS();
	}

	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(new ChannelInterceptor() {
			@Override
			public Message<?> preSend(Message<?> message, MessageChannel channel) {
				StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

				if (accessor == null) {
					return message;
				}

				if (StompCommand.CONNECT.equals(accessor.getCommand())) {
					String token = accessor.getFirstNativeHeader("Authorization");
					String xUserId = accessor.getFirstNativeHeader("X-User-ID");
					String roomCode = accessor.getFirstNativeHeader("roomCode");

					if (token != null && token.startsWith("Bearer ")) {
						token = token.substring(7);

						try {
							Jwt jwt = jwtDecoder.decode(token);
							String subject = jwt.getSubject();
							UUID userId = UUID.fromString(subject);

							accessor.getSessionAttributes().put("jwt_token", token);
							accessor.getSessionAttributes().put("userId", subject);
							accessor.getSessionAttributes().put("isGuest", false);

							if (roomCode != null && !roomCode.isBlank()) {
								accessor.getSessionAttributes().put("roomCode", roomCode);
								localRoomRepository.findByCode(roomCode).ifPresent(room -> {
									accessor.getSessionAttributes().put("roomId", room.getId().toString());
								});
							}

							String username = jwt.getClaimAsString("username");
							if (username != null) {
								accessor.getSessionAttributes().put("username", username);
							}

							logger.debug("WebSocket CONNECT validated with JWT - userId: {}", userId);

						} catch (JwtException e) {
							logger.warn("WebSocket CONNECT rejected - invalid JWT: {}", e.getMessage());
							return null; 
						} catch (IllegalArgumentException e) {
							logger.warn("WebSocket CONNECT rejected - JWT subject not UUID: {}", e.getMessage());
							return null; 
						}
					}
					else if (xUserId != null && !xUserId.isBlank()) {
						try {
							UUID guestId = UUID.fromString(xUserId);

							accessor.getSessionAttributes().put("userId", xUserId);
							accessor.getSessionAttributes().put("isGuest", true);

							logger.debug("WebSocket CONNECT accepted as guest - userId: {}", guestId);

						} catch (IllegalArgumentException e) {
							logger.warn("WebSocket CONNECT rejected - invalid X-User-ID format: {}", xUserId);
							return null; 
						}
					}
					else {
						logger.warn("WebSocket CONNECT rejected - no Authorization or X-User-ID header");
						return null; 
					}
				}

				if (!StompCommand.CONNECT.equals(accessor.getCommand()) &&
						!StompCommand.DISCONNECT.equals(accessor.getCommand())) {

					String sessionUserId = (String) accessor.getSessionAttributes().get("userId");

					if (sessionUserId == null) {
						logger.warn("WebSocket {} rejected - no validated session", accessor.getCommand());
						return null; 
					}

					Boolean isGuest = (Boolean) accessor.getSessionAttributes().get("isGuest");
					if (Boolean.FALSE.equals(isGuest)) {
						String headerUserId = accessor.getFirstNativeHeader("X-User-ID");
						if (headerUserId != null && !headerUserId.equals(sessionUserId)) {
							logger.warn("WebSocket {} rejected - X-User-ID mismatch for auth user: header={}, session={}",
									accessor.getCommand(), headerUserId, sessionUserId);
							return null; 
						}
					}
					// For guests, we trust the X-User-ID from CONNECT is carried through
						}

				return message;
			}
		});
	}
}
