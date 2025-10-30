package com.mm_mk.Messaging.listener;

import com.mm_mk.Messaging.util.CorrelationIdUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class StompDisconnectListener {

    private static final Logger logger = LoggerFactory.getLogger(StompDisconnectListener.class);

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.rooms}")
    private String roomsExchange;

    @Value("${rabbitmq.routingkey.user.left}")
    private String userLeftRoutingKey;

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor headers = StompHeaderAccessor.wrap(event.getMessage());

        String roomCode = (String) headers.getSessionAttributes().get("roomCode");
        String userIdStr = (String) headers.getSessionAttributes().get("userId");
        String roomIdStr = (String) headers.getSessionAttributes().get("roomId");

        if (roomCode == null || userIdStr == null || roomIdStr == null) {
            logger.debug("Disconnect ignored – no room/user in session");
            return;
        }

        logger.info("STOMP disconnect – publishing USER_LEFT for room {} user {}", roomCode, userIdStr);

        CorrelationIdUtil.setCorrelationId(CorrelationIdUtil.generateCorrelationId());

        rabbitTemplate.convertAndSend(
                roomsExchange,
                userLeftRoutingKey,
                Map.of("roomId", roomIdStr, "userId", userIdStr),
                m -> {
                    m.getMessageProperties().setHeader("X-Correlation-ID", CorrelationIdUtil.getCorrelationId());
                    return m;
                });

        CorrelationIdUtil.clear();
    }
}