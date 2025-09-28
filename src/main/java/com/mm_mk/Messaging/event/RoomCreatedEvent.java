// src/main/java/com/mm_mk/Messaging/event/RoomCreatedEvent.java
package com.mm_mk.Messaging.event;

import java.util.UUID;

public record RoomCreatedEvent(
    UUID id,
    String code,
    String name,
    UUID ownerId,
    Boolean isPrivate,
    Integer maxParticipants,
    java.time.LocalDateTime createdAt
) {}
