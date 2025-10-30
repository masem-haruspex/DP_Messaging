package com.mm_mk.Messaging.event;

import java.util.UUID;

public record RoomCreatedEvent(
    UUID id,
    String code,
    UUID ownerId,
    Boolean isPrivate,
    Integer maxParticipants,
    java.time.LocalDateTime createdAt
) {}
