package com.mm_mk.Messaging.event;

import java.util.UUID;

public record UserCreatedEvent(
        UUID id,
        String username,
        String preferredKeyboard
) {}
