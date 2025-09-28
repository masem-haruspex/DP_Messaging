// src/main/java/com/mm_mk/Messaging/service/MessagingService.java
package com.mm_mk.Messaging.service;

import com.mm_mk.Messaging.model.Message;
import com.mm_mk.Messaging.repository.MessageRepository;
import com.mm_mk.Messaging.response.MessageResponse;
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

    private final MessageRepository messageRepository;
    private final RabbitTemplate rabbitTemplate;
    private final RoomServiceClient roomServiceClient;

    @Value("${rabbitmq.exchange.rooms}")
    private String roomsExchange;

    @Value("${rabbitmq.routingkey.message.sent}")
    private String messageSentRoutingKey;

    public MessagingService(MessageRepository messageRepository,
                            RabbitTemplate rabbitTemplate,
                            RoomServiceClient roomServiceClient) {
        this.messageRepository = messageRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.roomServiceClient = roomServiceClient;
    }

    @Transactional
    public MessageResponse sendMessage(String roomCode, UUID userId, String content) {
        UUID roomId = roomServiceClient.getRoomByCode(roomCode).getId();

        Message message = Message.builder()
                .id(UUID.randomUUID())
                .roomId(roomId)
                .userId(userId)
                .content(content)
                .sentAt(LocalDateTime.now())
                .build();

        message = messageRepository.save(message);

        rabbitTemplate.convertAndSend(
                roomsExchange,
                messageSentRoutingKey,
                Map.of(
                        "messageId", message.getId().toString(),
                        "roomId", roomId.toString(),
                        "userId", userId.toString(),
                        "sentAt", message.getSentAt()
                )
        );

        return new MessageResponse(
                message.getId(),
                message.getRoomId(),
                message.getUserId(),
                message.getContent(),
                message.getSentAt()
        );
    }

    public List<MessageResponse> getMessages(String roomCode) {
        UUID roomId = roomServiceClient.getRoomByCode(roomCode).getId();
        return messageRepository.findByRoomIdOrderBySentAtAsc(roomId)
                .stream()
                .map(m -> new MessageResponse(
                        m.getId(),
                        m.getRoomId(),
                        m.getUserId(),
                        m.getContent(),
                        m.getSentAt()
                ))
                .collect(Collectors.toList());
    }

    public void cleanupRoomMessages(UUID roomId) {
        messageRepository.deleteByRoomId(roomId);
    }
}