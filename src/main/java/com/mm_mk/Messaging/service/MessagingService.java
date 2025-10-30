package com.mm_mk.Messaging.service;

import com.mm_mk.Messaging.model.LocalRoom;
import com.mm_mk.Messaging.model.LocalUser;
import com.mm_mk.Messaging.model.Message;
import com.mm_mk.Messaging.repository.LocalRoomRepository;
import com.mm_mk.Messaging.repository.LocalUserRepository;
import com.mm_mk.Messaging.repository.MessageRepository;
import com.mm_mk.Messaging.response.MessageResponse;
import com.mm_mk.Messaging.util.CorrelationIdUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MessagingService {

    private static final Logger logger = LoggerFactory.getLogger(MessagingService.class);
    private static final long SLOW_OPERATION_THRESHOLD_MS = 1000;

    private final MessageRepository messageRepository;
    private final LocalRoomRepository localRoomRepository;
    private final LocalUserRepository localUserRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.rooms}")
    private String roomsExchange;

    @Value("${rabbitmq.routingkey.message.sent}")
    private String messageSentRoutingKey;

    public MessagingService(MessageRepository messageRepository,
                            LocalRoomRepository localRoomRepository,
                            LocalUserRepository localUserRepository,
                            RabbitTemplate rabbitTemplate) {
        this.messageRepository = messageRepository;
        this.localRoomRepository = localRoomRepository;
        this.localUserRepository = localUserRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Transactional
    public MessageResponse sendMessage(String roomCode, UUID userId, String content) {
        long startTime = System.currentTimeMillis();
        logger.info("Sending message - roomCode: {}, userId: {}, contentLength: {}",
                roomCode, userId, content.length());

        try {
            LocalRoom room = localRoomRepository.findByCode(roomCode)
                    .orElseThrow(() -> {
                        logger.error("Room not found in local_rooms - roomCode: {}", roomCode);
                        return new RuntimeException("Room not found in local_rooms: " + roomCode);
                    });

            // Check if user exists, if not create a guest user
            LocalUser user = localUserRepository.findById(userId).orElseGet(() -> {
                logger.info("Creating guest user for messaging - userId: {}", userId);
                LocalUser guestUser = LocalUser.builder()
                        .id(userId)
                        .username("Guest_" + userId.toString().substring(0, 8))
                        .preferredKeyboard("Casio")
                        .lastSyncedAt(LocalDateTime.now())
                        .build();
                return localUserRepository.save(guestUser);
            });

            Message message = Message.builder()
                    .id(UUID.randomUUID())
                    .room(room)
                    .sender(user)
                    .content(content)
                    .sentAt(LocalDateTime.now())
                    .build();

            message = messageRepository.save(message);
            logger.debug("Message saved successfully - messageId: {}, roomCode: {}", message.getId(), roomCode);

            Map<String, Object> eventPayload = Map.of(
                    "messageId", message.getId().toString(),
                    "roomId", room.getId().toString(),
                    "roomCode", roomCode,
                    "userId", user.getId().toString(),
                    "username", user.getUsername(),
                    "content", message.getContent(),
                    "sentAt", message.getSentAt().toString()
            );

            rabbitTemplate.convertAndSend(roomsExchange, messageSentRoutingKey, eventPayload, m -> {
                m.getMessageProperties().setHeader("X-Correlation-ID", CorrelationIdUtil.getCorrelationId());
                return m;
            });

            logger.debug("Message event sent to RabbitMQ - messageId: {}, roomCode: {}", message.getId(), roomCode);

            return new MessageResponse(
                    message.getId(),
                    message.getRoom().getId(),
                    message.getSender().getId(),
                    user.getUsername(),
                    message.getContent(),
                    message.getSentAt()
            );
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Message sending completed in {}ms", duration);
            if (duration > SLOW_OPERATION_THRESHOLD_MS) {
                logger.warn("SLOW OPERATION: Message sending took {}ms", duration);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> getMessages(String roomCode) {
        long startTime = System.currentTimeMillis();
        logger.debug("Retrieving messages - roomCode: {}", roomCode);

        try {
            LocalRoom room = localRoomRepository.findByCode(roomCode)
                    .orElseThrow(() -> {
                        logger.error("Room not found in local_rooms - roomCode: {}", roomCode);
                        return new RuntimeException("Room not found in local_rooms: " + roomCode);
                    });

            List<MessageResponse> messages = messageRepository.findByRoomOrderBySentAtAsc(room)
                    .stream()
                    .map(message -> {
                        return new MessageResponse(
                                message.getId(),
                                message.getRoom().getId(),
                                message.getSender().getId(),
                                message.getSender().getUsername(),
                                message.getContent(),
                                message.getSentAt()
                        );
                    })
                    .collect(Collectors.toList());

            logger.debug("Retrieved {} messages for room: {}", messages.size(), roomCode);
            return messages;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logger.debug("Message retrieval completed in {}ms", duration);
            if (duration > SLOW_OPERATION_THRESHOLD_MS) {
                logger.warn("SLOW OPERATION: Message retrieval took {}ms", duration);
            }
        }
    }

    @Transactional
    public void cleanupRoomMessages(UUID roomId) {
        long startTime = System.currentTimeMillis();
        logger.debug("Cleaning up room messages - roomId: {}", roomId);

        try {
            localRoomRepository.findById(roomId).ifPresent(room -> {
                messageRepository.deleteByRoom(room);
                logger.info("Cleaned up messages for room: {}", roomId);
            });
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logger.debug("Room cleanup completed in {}ms", duration);
            if (duration > SLOW_OPERATION_THRESHOLD_MS) {
                logger.warn("SLOW OPERATION: Room cleanup took {}ms", duration);
            }
        }
    }
}