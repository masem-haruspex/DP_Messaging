// src/main/java/com/mm_mk/Messaging/listener/RoomEventListener.java
package com.mm_mk.Messaging.listener;

import com.mm_mk.Messaging.model.LocalRoom;
import com.mm_mk.Messaging.repository.LocalRoomRepository;
import com.mm_mk.Messaging.service.MessagingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Component
public class RoomEventListener {

    private static final Logger logger = LoggerFactory.getLogger(RoomEventListener.class);

    private final MessagingService messagingService;
    private final LocalRoomRepository localRoomRepository;

    public RoomEventListener(MessagingService messagingService,
                             LocalRoomRepository localRoomRepository) {
        logger.info("RoomEventListener bean created!");
        this.messagingService = messagingService;
        this.localRoomRepository = localRoomRepository;
    }

    @RabbitListener(queues = "messaging.room.created.queue")
    public void handleRoomCreated(Map<String, Object> event) {
        UUID roomId = UUID.fromString((String) event.get("id"));
        String code = (String) event.get("code");
        String name = (String) event.get("name");

        logger.info("Initialized chat for room: {} (roomId:{})", code, roomId);

        // Insert into local_rooms if not already present
        localRoomRepository.findById(roomId).ifPresentOrElse(
                r -> logger.debug("Room {} already exists locally", roomId),
                () -> {
                    LocalRoom localRoom = LocalRoom.builder()
                            .id(roomId)
                            .code(code)
                            .name(name)
                            .lastSyncedAt(LocalDateTime.now())
                            .build();
                    localRoomRepository.save(localRoom);
                    logger.info("Stored local copy of room {} ({})", code, roomId);
                }
        );
    }

    @RabbitListener(queues = "messaging.room.deleted.queue")
    public void handleRoomDeleted(Map<String, Object> event) {
        UUID roomId = UUID.fromString((String) event.get("roomId"));
        logger.info("Cleaning up messages for deleted room: {}", roomId);

        // Delete related messages
        messagingService.cleanupRoomMessages(roomId);

        // Delete local copy of room
        localRoomRepository.findById(roomId).ifPresent(room -> {
            localRoomRepository.delete(room);
            logger.info("Removed local room entry for {}", roomId);
        });
    }

    @RabbitListener(queues = "messaging.user.joined.queue")
    public void handleUserJoined(Map<String, Object> event) {
        UUID roomId = UUID.fromString((String) event.get("roomId"));
        UUID userId = UUID.fromString((String) event.get("userId"));
        logger.info("User {} joined room {}", userId, roomId);

        // optional: could log to analytics or store participants later
    }

    @RabbitListener(queues = "messaging.user.left.queue")
    public void handleUserLeft(Map<String, Object> event) {
        UUID roomId = UUID.fromString((String) event.get("roomId"));
        UUID userId = UUID.fromString((String) event.get("userId"));
        logger.info("User {} left room {}", userId, roomId);

        // optional: same here, no persistence needed unless you track participants
    }
}
