package com.mm_mk.Messaging.listener;

import com.mm_mk.Messaging.response.MessageResponse;
import lombok.AllArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@AllArgsConstructor
public class MessageEventListener {

    private final SimpMessagingTemplate simpMessagingTemplate;

    @RabbitListener(queues = "messaging.message.sent.queue")
    public void onMessage(Map<String, String> payload) {
        try {
            MessageResponse response = new MessageResponse(
                    UUID.fromString(payload.get("messageId")),
                    UUID.fromString(payload.get("roomId")),
                    UUID.fromString(payload.get("userId")),
                    payload.get("username"),
                    payload.get("content"),
                    LocalDateTime.parse(payload.get("sentAt"))
            );

            String roomCode = payload.get("roomCode");

            if (roomCode == null) {
                System.err.println("Room code not found in message payload");
                return;
            }

            simpMessagingTemplate.convertAndSend(
                    "/topic/rooms/" + roomCode + "/chat",
                    Map.of(
                            "type", "CHAT_MESSAGE",
                            "payload", response,
                            "timestamp", System.currentTimeMillis()
                    )
            );

            System.out.println("Broadcasted message to room: " + roomCode + " from user: " + payload.get("username"));

        } catch (Exception e) {
            System.err.println("Error processing message event: " + e.getMessage());
            e.printStackTrace();
        }
    }
}