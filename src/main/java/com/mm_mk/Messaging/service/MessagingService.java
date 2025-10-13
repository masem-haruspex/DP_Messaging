package com.mm_mk.Messaging.service;

import com.mm_mk.Messaging.model.LocalRoom;
import com.mm_mk.Messaging.model.LocalUser;
import com.mm_mk.Messaging.model.Message;
import com.mm_mk.Messaging.repository.LocalRoomRepository;
import com.mm_mk.Messaging.repository.LocalUserRepository;
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
        LocalRoom room = localRoomRepository.findByCode(roomCode)
                .orElseThrow(() -> new RuntimeException("Room not found in local_rooms: " + roomCode));

        LocalUser user = localUserRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found in local_users: " + userId));

        Message message = Message.builder()
                .id(UUID.randomUUID())
                .room(room)
                .sender(user)
                .content(content)
                .sentAt(LocalDateTime.now())
                .build();

        message = messageRepository.save(message);

        rabbitTemplate.convertAndSend(
                roomsExchange,
                messageSentRoutingKey,
                Map.of(
                        "messageId", message.getId().toString(),
                        "roomId", room.getId().toString(),
                        "userId", user.getId().toString(),
                        "content", message.getContent(),
                        "sentAt", message.getSentAt().toString()
                )
        );

        return new MessageResponse(
                message.getId(),
                message.getRoom().getId(),
                message.getSender().getId(),
                message.getContent(),
                message.getSentAt()
        );
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> getMessages(String roomCode) {
        LocalRoom room = localRoomRepository.findByCode(roomCode)
                .orElseThrow(() -> new RuntimeException("Room not found in local_rooms: " + roomCode));

        return messageRepository.findByRoomOrderBySentAtAsc(room)
                .stream()
                .map(m -> new MessageResponse(
                        m.getId(),
                        m.getRoom().getId(),
                        m.getSender().getId(),
                        m.getContent(),
                        m.getSentAt()
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public void cleanupRoomMessages(UUID roomId) {
        localRoomRepository.findById(roomId).ifPresent(room -> {
            messageRepository.deleteByRoom(room);
        });
    }
}
