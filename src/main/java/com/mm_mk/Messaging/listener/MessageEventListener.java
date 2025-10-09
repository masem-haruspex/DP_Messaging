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

    @RabbitListener(queues = "${rabbitmq.queue.message.sent}")
    public void onMessage(Map<String, String> payload) {
        MessageResponse response = new MessageResponse(
                UUID.fromString(payload.get("messageId")),
                UUID.fromString(payload.get("roomId")),
                UUID.fromString(payload.get("userId")),
                payload.get("content"),
                LocalDateTime.parse(payload.get("sentAt"))
        );

        // broadcast to WebSocket subscribers
        simpMessagingTemplate.convertAndSend(
                "/topic/rooms/" + payload.get("roomId"),
                response
        );
    }
}
