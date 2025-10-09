package com.mm_mk.Messaging.controller;

import com.mm_mk.Messaging.request.SendMessageRequest;
import com.mm_mk.Messaging.response.MessageResponse;
import com.mm_mk.Messaging.service.MessagingService;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
public class WebSocketMessageController {

    private final MessagingService messagingService;
    private final SimpMessagingTemplate simpMessagingTemplate;

    public WebSocketMessageController(MessagingService messagingService,
                                      SimpMessagingTemplate simpMessagingTemplate) {
        this.messagingService = messagingService;
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    @MessageMapping("/sendMessage") // client sends to /app/sendMessage
    public void handleMessage(SendMessageRequest request,
                              @Header("user-id") UUID userId) {
        MessageResponse saved = messagingService.sendMessage(
                request.roomCode(),
                userId,
                request.content()
        );

        // Push real-time message to subscribers of the room
        simpMessagingTemplate.convertAndSend(
                "/topic/rooms/" + request.roomCode(),
                saved
        );
    }
}
