package com.mm_mk.Messaging.controller;

import com.mm_mk.Messaging.listener.RoomEventListener;
import com.mm_mk.Messaging.request.SendMessageRequest;
import com.mm_mk.Messaging.response.MessageResponse;
import com.mm_mk.Messaging.service.MessagingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.UUID;

@Controller
public class WebSocketController {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketController.class);

    private final MessagingService messagingService;
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketController(MessagingService messagingService,
                               SimpMessagingTemplate messagingTemplate) {
        this.messagingService = messagingService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/rooms/{roomCode}/sendMessage")
    public void handleChatMessage(@DestinationVariable String roomCode,
                                  SendMessageRequest request,
                                  @Header("X-User-ID") UUID userId) {

        try{
        MessageResponse savedMessage = messagingService.sendMessage(roomCode, userId, request.content());

        messagingTemplate.convertAndSend(
                "/topic/rooms/" + roomCode + "/chat",
                Map.of(
                        "type", "CHAT_MESSAGE",
                        "payload", savedMessage,
                        "timestamp", System.currentTimeMillis()
                )
        );
        } catch (Exception e) {
            logger.error("Failed to handle chat message for room {}: {}", roomCode, e.getMessage());
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/errors",
                    Map.of(
                            "type", "MESSAGE_SEND_ERROR",
                            "error", "Failed to send message",
                            "timestamp", System.currentTimeMillis()
                    )
            );
        }

    }

    @MessageMapping("/rooms/{roomCode}/keyEvent")
    public void handleKeyEvent(@DestinationVariable String roomCode,
                               Map<String, Object> keyEvent,
                               @Header("X-User-ID") UUID userId) {

       logger.info("Received key event for room {} from user {}: {}", roomCode, userId, keyEvent);

        messagingTemplate.convertAndSend(
                "/topic/rooms/" + roomCode + "/keyEvents",
                Map.of(
                        "type", "KEY_EVENT",
                        "userId", userId.toString(),
                        "payload", keyEvent,
                        "timestamp", System.currentTimeMillis()
                )
        );

    logger.info("Broadcasted key event to /topic/rooms/{}/keyEvents", roomCode);

    }

    @MessageMapping("/rooms/{roomCode}/join")
    public void handleUserJoin(@DestinationVariable String roomCode,
                               @Header("X-User-ID") UUID userId) {

        messagingTemplate.convertAndSend(
                "/topic/rooms/" + roomCode + "/participants",
                Map.of(
                        "type", "USER_JOINED",
                        "userId", userId.toString(),
                        "roomCode", roomCode,
                        "timestamp", System.currentTimeMillis()
                )
        );
    }

    @MessageMapping("/rooms/{roomCode}/leave")
    public void handleUserLeave(@DestinationVariable String roomCode,
                                @Header("X-User-ID") UUID userId) {

        messagingTemplate.convertAndSend(
                "/topic/rooms/" + roomCode + "/participants",
                Map.of(
                        "type", "USER_LEFT",
                        "userId", userId.toString(),
                        "roomCode", roomCode,
                        "timestamp", System.currentTimeMillis()
                )
        );
    }
}
//package com.mm_mk.Messaging.controller;
//
//import com.mm_mk.Messaging.request.SendMessageRequest;
//import com.mm_mk.Messaging.response.MessageResponse;
//import com.mm_mk.Messaging.service.MessagingService;
//import org.springframework.messaging.handler.annotation.Header;
//import org.springframework.messaging.handler.annotation.MessageMapping;
//import org.springframework.messaging.simp.SimpMessagingTemplate;
//import org.springframework.stereotype.Controller;
//
//import java.util.UUID;
//
//@Controller
//public class WebSocketController {
//
//    private final MessagingService messagingService;
//    private final SimpMessagingTemplate simpMessagingTemplate;
//
//    public WebSocketController(MessagingService messagingService,
//                               SimpMessagingTemplate simpMessagingTemplate) {
//        this.messagingService = messagingService;
//        this.simpMessagingTemplate = simpMessagingTemplate;
//    }
//
//    @MessageMapping("/sendMessage") // client sends to /app/sendMessage
//    public void handleMessage(SendMessageRequest request,
//                              @Header("user-id") UUID userId) {
//        MessageResponse saved = messagingService.sendMessage(
//                request.roomCode(),
//                userId,
//                request.content()
//        );
//
//        // Push real-time message to subscribers of the room
//        simpMessagingTemplate.convertAndSend(
//                "/topic/rooms/" + request.roomCode(),
//                saved
//        );
//    }
//}
//
