package com.mm_mk.Messaging.event;

import java.util.UUID;

public record UserJoinedEvent(
    UUID roomId,
    UUID userId,
    java.time.LocalDateTime joinedAt
) {}
