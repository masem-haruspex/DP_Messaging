package com.mm_mk.Messaging.listener;

import com.mm_mk.Messaging.model.LocalRoom;
import com.mm_mk.Messaging.repository.LocalRoomRepository;
import com.mm_mk.Messaging.service.MessagingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Component
public class RoomEventListener {

	private static final Logger logger = LoggerFactory.getLogger(RoomEventListener.class);

	private final MessagingService messagingService;
	private final LocalRoomRepository localRoomRepository;
	private final SimpMessagingTemplate messagingTemplate;

	public RoomEventListener(MessagingService messagingService,
			LocalRoomRepository localRoomRepository,
			SimpMessagingTemplate messagingTemplate) {
		logger.info("RoomEventListener bean created!");
		this.messagingService = messagingService;
		this.localRoomRepository = localRoomRepository;
		this.messagingTemplate = messagingTemplate;
	}

	@RabbitListener(queues = "messaging.room.created.queue")
	public void handleRoomCreated(Map<String, Object> event) {
		UUID roomId = UUID.fromString((String) event.get("id"));
		String code = (String) event.get("code");
		String name = (String) event.get("name");

		logger.info("Initialized chat for room: {} (roomId:{})", code, roomId);

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
	}

	@RabbitListener(queues = "messaging.user.joined.queue")
	public void handleUserJoined(Map<String, Object> event) {
		logger.info("Received user joined event: {}", event);

		UUID roomId = UUID.fromString((String) event.get("roomId"));
		UUID userId = UUID.fromString((String) event.get("userId"));

		logger.info("Looking up room {} for user {}", roomId, userId);

		Optional<LocalRoom> roomOpt = localRoomRepository.findById(roomId);

		if (roomOpt.isPresent()) {
			LocalRoom room = roomOpt.get();
			String roomCode = room.getCode();

			logger.info("Broadcasting USER_JOINED to room {} for user {}", roomCode, userId);

			messagingTemplate.convertAndSend(
					"/topic/rooms/" + roomCode + "/participants",
					Map.of(
						"type", "USER_JOINED",
						"userId", userId.toString(),
						"roomCode", roomCode,
						"timestamp", System.currentTimeMillis()
						)
					);
			logger.info("USER_JOINED broadcast successful");
		} else {
			logger.error("Room {} not found in local repository!", roomId);
		}
	}

	@RabbitListener(queues = "messaging.user.left.queue")
	public void handleUserLeft(Map<String, Object> event) {
		UUID roomId = UUID.fromString((String) event.get("roomId"));
		UUID userId = UUID.fromString((String) event.get("userId"));

		localRoomRepository.findById(roomId).ifPresent(room -> {
			String roomCode = room.getCode();

			logger.info("User {} left room {}", userId, roomCode);

			messagingTemplate.convertAndSend(
					"/topic/rooms/" + roomCode + "/participants",
					Map.of(
						"type", "USER_LEFT",
						"userId", userId.toString(),
						"roomCode", roomCode,
						"timestamp", System.currentTimeMillis()
						)
					);
		});
	}

	@RabbitListener(queues = "messaging.user.kicked.queue")
	public void handleUserKicked(Map<String, Object> event) {
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
	}

	@RabbitListener(queues = "messaging.user.muted.queue")
	public void handleUserMuted(Map<String, Object> event) {
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
	}
}
