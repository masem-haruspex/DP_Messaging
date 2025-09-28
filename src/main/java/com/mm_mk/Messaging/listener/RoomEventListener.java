// src/main/java/com/mm_mk/Messaging/listener/RoomEventListener.java
package com.mm_mk.Messaging.listener;

import com.mm_mk.Messaging.service.MessagingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class RoomEventListener {

    private static final Logger logger = LoggerFactory.getLogger(RoomEventListener.class);
    private final MessagingService messagingService;

    public RoomEventListener(MessagingService messagingService) {
        logger.info("RoomEventListener bean created!");
        this.messagingService = messagingService;
    }

    @RabbitListener(queues = "messaging.room.created.queue")
    public void handleRoomCreated(Map<String, Object> event) {
        UUID roomId = UUID.fromString((String) event.get("id"));
        String code = (String) event.get("code");
        logger.info("Initialized chat for room: {} (roomId:{})", code, roomId);
    }

    @RabbitListener(queues = "messaging.room.deleted.queue")
    public void handleRoomDeleted(Map<String, Object> event) {
        UUID roomId = UUID.fromString((String) event.get("roomId"));
        logger.info("Cleaning up messages for deleted room: {}", roomId);
        messagingService.cleanupRoomMessages(roomId);
    }

    @RabbitListener(queues = "messaging.user.joined.queue")
    public void handleUserJoined(Map<String, Object> event) {
        UUID roomId = UUID.fromString((String) event.get("roomId"));
        UUID userId = UUID.fromString((String) event.get("userId"));
        logger.info("User {} joined room {}", userId, roomId);
    }

    @RabbitListener(queues = "messaging.user.left.queue")
    public void handleUserLeft(Map<String, Object> event) {
        UUID roomId = UUID.fromString((String) event.get("roomId"));
        UUID userId = UUID.fromString((String) event.get("userId"));
        logger.info("User {} left room {}", userId, roomId);
    }

}
