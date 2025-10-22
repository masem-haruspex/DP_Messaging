package com.mm_mk.Messaging.listener;
import com.mm_mk.Messaging.event.UserCreatedEvent;
import com.mm_mk.Messaging.event.UserUpdatedEvent;
import com.mm_mk.Messaging.model.LocalUser;
import com.mm_mk.Messaging.repository.LocalUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserEventListener {
    private final LocalUserRepository localUserRepository;

    @RabbitListener(queues = "messaging.user.created.queue")
    @Transactional
    public void handleUserEvent(UserCreatedEvent event) {
        // Only insert if not already exists
        if (!localUserRepository.existsById(event.id())) {
            LocalUser localUser = LocalUser.builder()
                    .id(event.id())
                    .username(event.username())
                    .build();

            localUserRepository.save(localUser);
        }
    }

    @RabbitListener(queues = "messaging.user.updated.queue")
    @Transactional
    public void handleUserUpdated(UserUpdatedEvent event) {
        localUserRepository.findById(event.id()).ifPresent(existingUser -> {
            existingUser.setUsername(event.username());
            existingUser.setPreferredKeyboard(event.preferredKeyboard());
            localUserRepository.save(existingUser);
        });
    }
}