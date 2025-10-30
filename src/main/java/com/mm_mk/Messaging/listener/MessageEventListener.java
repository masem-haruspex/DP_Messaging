package com.mm_mk.Messaging.listener;

import com.mm_mk.Messaging.response.MessageResponse;
import com.mm_mk.Messaging.util.CorrelationIdUtil;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@AllArgsConstructor
public class MessageEventListener {

    private static final Logger logger = LoggerFactory.getLogger(MessageEventListener.class);
    private static final long SLOW_OPERATION_THRESHOLD_MS = 500;

    private final SimpMessagingTemplate simpMessagingTemplate;

    @RabbitListener(queues = "messaging.message.sent.queue")
    public void onMessage(Map<String, String> payload,
                          @Header(name = "X-Correlation-ID", required = false) String correlationId,
                          @Header(name = AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) {

        String eventCorrelationId = correlationId != null ? correlationId :
                "EVENT-" + CorrelationIdUtil.generateCorrelationId();
        CorrelationIdUtil.setCorrelationId(eventCorrelationId);

        long startTime = System.currentTimeMillis();
        logger.info("Processing MESSAGE_SENT event - routingKey: {}, roomCode: {}, username: {}",
                routingKey, payload.get("roomCode"), payload.get("username"));

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
                logger.error("Room code not found in message payload: {}", payload);
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

            logger.debug("MESSAGE_SENT event processed successfully - roomCode: {}, username: {}",
                    roomCode, payload.get("username"));
        } catch (Exception e) {
            logger.error("Error processing MESSAGE_SENT event: {}", e.getMessage(), e);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logger.debug("MESSAGE_SENT event processing completed in {}ms", duration);
            CorrelationIdUtil.clear();
            if (duration > SLOW_OPERATION_THRESHOLD_MS) {
                logger.warn("SLOW EVENT: MESSAGE_SENT processing took {}ms", duration);
            }
        }
    }
}