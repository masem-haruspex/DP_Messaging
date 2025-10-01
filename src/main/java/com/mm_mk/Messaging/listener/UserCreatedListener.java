package com.mm_mk.Messaging.listener;
import com.mm_mk.Messaging.model.LocalUser;
import com.mm_mk.Messaging.repository.LocalUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserCreatedListener {
    private final LocalUserRepository repo;

    @RabbitListener(queues = "messaging.user.queue")
    public void handleUserCreated(UserCreatedEvent event) {
        if (!repo.existsById(event.id())) {
            repo.save(LocalUser.builder()
                    .id(event.id())
                    .username(event.username())
                    .build());
        }
    }

    public record UserCreatedEvent(UUID id, String username) {}
}