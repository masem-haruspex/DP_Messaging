package com.mm_mk.Messaging.listener;

import com.mm_mk.Messaging.model.LocalRoom;
import com.mm_mk.Messaging.model.LocalUser;
import com.mm_mk.Messaging.repository.LocalRoomRepository;
import com.mm_mk.Messaging.repository.LocalUserRepository;
import com.mm_mk.Messaging.service.MessagingService;
import com.mm_mk.Messaging.util.CorrelationIdUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Component
public class RoomEventListener {

    private static final Logger logger = LoggerFactory.getLogger(RoomEventListener.class);
    private static final long SLOW_OPERATION_THRESHOLD_MS = 500;

    private final MessagingService messagingService;
    private final LocalRoomRepository localRoomRepository;
    private final LocalUserRepository localUserRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public RoomEventListener(MessagingService messagingService,
                             LocalRoomRepository localRoomRepository,
                             LocalUserRepository localUserRepository,
                             SimpMessagingTemplate messagingTemplate) {
        this.localUserRepository = localUserRepository;
        logger.info("RoomEventListener initialized successfully");
        this.messagingService = messagingService;
        this.localRoomRepository = localRoomRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @RabbitListener(queues = "messaging.room.created.queue")
    public void handleRoomCreated(Map<String, Object> event,
                                  @Header(name = "X-Correlation-ID", required = false) String correlationId,
                                  @Header(name = AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) {

        String eventCorrelationId = correlationId != null
                ? correlationId
                : "EVENT-" + CorrelationIdUtil.generateCorrelationId();
        CorrelationIdUtil.setCorrelationId(eventCorrelationId);

        long startTime = System.currentTimeMillis();
        logger.info("Processing ROOM_CREATED event - routingKey: {}, event: {}", routingKey, event);

        try {
            UUID roomId = UUID.fromString((String) event.get("id"));
            String code = (String) event.get("code");

            logger.info("Initializing chat for room: {} (roomId:{})", code, roomId);

            localRoomRepository.findById(roomId).ifPresentOrElse(
                    r -> logger.debug("Room {} already exists locally", roomId),
                    () -> {
                        LocalRoom localRoom = LocalRoom.builder()
                                .id(roomId)
                                .code(code)
                                .lastSyncedAt(LocalDateTime.now())
                                .build();
                        localRoomRepository.save(localRoom);
                        logger.info("Stored local copy of room {} ({})", code, roomId);
                    }
            );
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logger.debug("ROOM_CREATED event processing completed in {}ms", duration);
            CorrelationIdUtil.clear();
            if (duration > SLOW_OPERATION_THRESHOLD_MS) {
                logger.warn("SLOW EVENT: ROOM_CREATED processing took {}ms", duration);
            }
        }
    }

    @RabbitListener(queues = "messaging.room.deleted.queue")
    public void handleRoomDeleted(Map<String, Object> event,
                                  @Header(name = "X-Correlation-ID", required = false) String correlationId,
                                  @Header(name = AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) {

        String eventCorrelationId = correlationId != null ? correlationId :
                "EVENT-" + CorrelationIdUtil.generateCorrelationId();
        CorrelationIdUtil.setCorrelationId(eventCorrelationId);

        long startTime = System.currentTimeMillis();
        logger.info("Processing ROOM_DELETED event - routingKey: {}, event: {}", routingKey, event);

        try {
            UUID roomId = UUID.fromString((String) event.get("roomId"));
            logger.info("Cleaning up messages for deleted room: {}", roomId);

            messagingService.cleanupRoomMessages(roomId);

            localRoomRepository.findById(roomId).ifPresent(room -> {
                localRoomRepository.delete(room);
                logger.info("Removed local room entry for {}", roomId);
            });

            messagingTemplate.convertAndSend(
                    "/topic/rooms/" + roomId + "/system",
                    Map.of(
                            "type", "ROOM_DELETED",
                            "roomId", roomId.toString(),
                            "timestamp", System.currentTimeMillis()
                    )
            );
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logger.debug("ROOM_DELETED event processing completed in {}ms", duration);
            CorrelationIdUtil.clear();
            if (duration > SLOW_OPERATION_THRESHOLD_MS) {
                logger.warn("SLOW EVENT: ROOM_DELETED processing took {}ms", duration);
            }
        }
    }

    @RabbitListener(queues = "messaging.user.left.queue")
    public void handleUserLeft(Map<String, Object> event,
                               @Header(name = "X-Correlation-ID", required = false) String correlationId,
                               @Header(name = AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) {

        String eventCorrelationId = correlationId != null ? correlationId
                : "EVENT-" + CorrelationIdUtil.generateCorrelationId();
        CorrelationIdUtil.setCorrelationId(eventCorrelationId);

        long startTime = System.currentTimeMillis();
        logger.info("Processing USER_LEFT event - routingKey: {}, event: {}", routingKey, event);

        try {
            UUID roomId   = UUID.fromString((String) event.get("roomId"));
            UUID userId   = UUID.fromString((String) event.get("userId"));

            localRoomRepository.findById(roomId).ifPresent(room -> {
                String roomCode = room.getCode();

                String username = localUserRepository.findById(userId)
                        .map(LocalUser::getUsername)
                        .orElse("guest-" + userId.toString().substring(0, 8));

                logger.info("User {} ({}) left room {}", username, userId, roomCode);

                messagingTemplate.convertAndSend(
                        "/topic/rooms/" + roomCode + "/participants",
                        Map.of(
                                "type",      "USER_LEFT",
                                "userId",    userId.toString(),
                                "username",  username,          
                                "roomCode",  roomCode,
                                "timestamp", System.currentTimeMillis()
                        )
                );
            });
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logger.debug("USER_LEFT event processing completed in {}ms", duration);
            CorrelationIdUtil.clear();
            if (duration > SLOW_OPERATION_THRESHOLD_MS) {
                logger.warn("SLOW EVENT: USER_LEFT processing took {}ms", duration);
            }
        }
    }

    @RabbitListener(queues = "messaging.user.joined.queue")
    public void handleUserJoined(Map<String, Object> event,
                                 @Header(name = "X-Correlation-ID", required = false) String correlationId,
                                 @Header(name = AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) {

        String eventCorrelationId = correlationId != null ? correlationId
                : "EVENT-" + CorrelationIdUtil.generateCorrelationId();
        CorrelationIdUtil.setCorrelationId(eventCorrelationId);

        long startTime = System.currentTimeMillis();
        logger.info("Processing USER_JOINED event - routingKey: {}, event: {}", routingKey, event);

        try {
            UUID roomId   = UUID.fromString((String) event.get("roomId"));
            UUID userId   = UUID.fromString((String) event.get("userId"));

            localRoomRepository.findById(roomId).ifPresent(room -> {
                String roomCode = room.getCode();

                /* same username resolution */
                String username = localUserRepository.findById(userId)
                        .map(LocalUser::getUsername)
                        .orElse("guest-" + userId.toString().substring(0, 8));

                logger.info("Broadcasting USER_JOINED to room {} for user {} ({})", roomCode, username, userId);

                messagingTemplate.convertAndSend(
                        "/topic/rooms/" + roomCode + "/participants",
                        Map.of(
                                "type",      "USER_JOINED",
                                "userId",    userId.toString(),
                                "username",  username,
                                "roomCode",  roomCode,
                                "timestamp", System.currentTimeMillis()
                        )
                );
            });
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logger.debug("USER_JOINED event processing completed in {}ms", duration);
            CorrelationIdUtil.clear();
            if (duration > SLOW_OPERATION_THRESHOLD_MS) {
                logger.warn("SLOW EVENT: USER_JOINED processing took {}ms", duration);
            }
        }
    }


    @RabbitListener(queues = "messaging.user.kicked.queue")
    public void handleUserKicked(Map<String, Object> event,
                                 @Header(name = "X-Correlation-ID", required = false) String correlationId,
                                 @Header(name = AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) {

        String eventCorrelationId = correlationId != null ? correlationId :
                "EVENT-" + CorrelationIdUtil.generateCorrelationId();
        CorrelationIdUtil.setCorrelationId(eventCorrelationId);

        long startTime = System.currentTimeMillis();
        logger.info("Processing USER_KICKED event - routingKey: {}, event: {}", routingKey, event);

        try {
            UUID roomId = UUID.fromString((String) event.get("roomId"));
            UUID userId = UUID.fromString((String) event.get("userId"));
            UUID kickedBy = UUID.fromString((String) event.get("kickedBy"));

            localRoomRepository.findById(roomId).ifPresent(room -> {
                String roomCode = room.getCode();

                logger.info("User {} kicked from room {} by {}", userId, roomCode, kickedBy);

                messagingTemplate.convertAndSend(
                        "/topic/rooms/" + roomCode + "/participants",
                        Map.of(
                                "type", "USER_KICKED",
                                "userId", userId.toString(),
                                "kickedBy", kickedBy.toString(),
                                "roomCode", roomCode,
                                "timestamp", System.currentTimeMillis()
                        )
                );

                messagingTemplate.convertAndSendToUser(
                        userId.toString(),
                        "/queue/notifications",
                        Map.of(
                                "type", "YOU_WERE_KICKED",
                                "roomCode", roomCode,
                                "kickedBy", kickedBy.toString(),
                                "timestamp", System.currentTimeMillis()
                        )
                );
            });
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logger.debug("USER_KICKED event processing completed in {}ms", duration);
            CorrelationIdUtil.clear();
            if (duration > SLOW_OPERATION_THRESHOLD_MS) {
                logger.warn("SLOW EVENT: USER_KICKED processing took {}ms", duration);
            }
        }
    }

    @RabbitListener(queues = "messaging.user.muted.queue")
    public void handleUserMuted(Map<String, Object> event,
                                @Header(name = "X-Correlation-ID", required = false) String correlationId,
                                @Header(name = AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) {

        String eventCorrelationId = correlationId != null ? correlationId :
                "EVENT-" + CorrelationIdUtil.generateCorrelationId();
        CorrelationIdUtil.setCorrelationId(eventCorrelationId);

        long startTime = System.currentTimeMillis();
        logger.info("Processing USER_MUTED event - routingKey: {}, event: {}", routingKey, event);

        try {
            UUID roomId = UUID.fromString((String) event.get("roomId"));
            UUID userId = UUID.fromString((String) event.get("userId"));
            UUID mutedBy = UUID.fromString((String) event.get("mutedBy"));

            localRoomRepository.findById(roomId).ifPresent(room -> {
                String roomCode = room.getCode();

                logger.info("User {} muted in room {} by {}", userId, roomCode, mutedBy);

                messagingTemplate.convertAndSend(
                        "/topic/rooms/" + roomCode + "/participants",
                        Map.of(
                                "type", "USER_MUTED",
                                "userId", userId.toString(),
                                "mutedBy", mutedBy.toString(),
                                "roomCode", roomCode,
                                "timestamp", System.currentTimeMillis()
                        )
                );
            });
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logger.debug("USER_MUTED event processing completed in {}ms", duration);
            CorrelationIdUtil.clear();
            if (duration > SLOW_OPERATION_THRESHOLD_MS) {
                logger.warn("SLOW EVENT: USER_MUTED processing took {}ms", duration);
            }
        }
    }
}
