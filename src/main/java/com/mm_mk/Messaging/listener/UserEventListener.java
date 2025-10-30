package com.mm_mk.Messaging.listener;

import com.mm_mk.Messaging.event.UserCreatedEvent;
import com.mm_mk.Messaging.event.UserUpdatedEvent;
import com.mm_mk.Messaging.model.LocalUser;
import com.mm_mk.Messaging.repository.LocalUserRepository;
import com.mm_mk.Messaging.util.CorrelationIdUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UserEventListener {
    private static final Logger logger = LoggerFactory.getLogger(UserEventListener.class);
    private static final long SLOW_OPERATION_THRESHOLD_MS = 500;

    private final LocalUserRepository localUserRepository;

    @RabbitListener(queues = "messaging.user.created.queue")
    @Transactional
    public void handleUserEvent(UserCreatedEvent event,
                                @Header(name = "X-Correlation-ID", required = false) String correlationId,
                                @Header(name = AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) {

        String eventCorrelationId = correlationId != null ? correlationId :
                "EVENT-" + CorrelationIdUtil.generateCorrelationId();
        CorrelationIdUtil.setCorrelationId(eventCorrelationId);

        long startTime = System.currentTimeMillis();
        logger.info("Processing USER_CREATED event - userId: {}, username: {}, routingKey: {}",
                event.id(), event.username(), routingKey);

        try {
            if (!localUserRepository.existsById(event.id())) {
                LocalUser localUser = LocalUser.builder()
                        .id(event.id())
                        .username(event.username())
                        .build();

                localUserRepository.save(localUser);
                logger.info("User created successfully - userId: {}, username: {}", event.id(), event.username());
            } else {
                logger.debug("User already exists, skipping creation - userId: {}", event.id());
            }
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logger.debug("USER_CREATED event processing completed in {}ms", duration);
            CorrelationIdUtil.clear();
            if (duration > SLOW_OPERATION_THRESHOLD_MS) {
                logger.warn("SLOW EVENT: USER_CREATED processing took {}ms", duration);
            }
        }
    }

    @RabbitListener(queues = "messaging.user.updated.queue")
    @Transactional
    public void handleUserUpdated(UserUpdatedEvent event,
                                  @Header(name = "X-Correlation-ID", required = false) String correlationId,
                                  @Header(name = AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) {

        String eventCorrelationId = correlationId != null ? correlationId :
                "EVENT-" + CorrelationIdUtil.generateCorrelationId();
        CorrelationIdUtil.setCorrelationId(eventCorrelationId);

        long startTime = System.currentTimeMillis();
        logger.info("Processing USER_UPDATED event - userId: {}, username: {}, preferredKeyboard: {}, routingKey: {}",
                event.id(), event.username(), event.preferredKeyboard(), routingKey);

        try {
            localUserRepository.findById(event.id()).ifPresent(existingUser -> {
                existingUser.setUsername(event.username());
                existingUser.setPreferredKeyboard(event.preferredKeyboard());
                localUserRepository.save(existingUser);
                logger.info("User updated successfully - userId: {}, newUsername: {}, newKeyboard: {}",
                        event.id(), event.username(), event.preferredKeyboard());
            });
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logger.debug("USER_UPDATED event processing completed in {}ms", duration);
            CorrelationIdUtil.clear();
            if (duration > SLOW_OPERATION_THRESHOLD_MS) {
                logger.warn("SLOW EVENT: USER_UPDATED processing took {}ms", duration);
            }
        }
    }
}