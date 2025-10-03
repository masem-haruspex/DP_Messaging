package com.mm_mk.Messaging.response;

import java.util.UUID;

public record MessageResponse(
    UUID id,
    UUID roomId,
    UUID userId,
    String content,
    java.time.LocalDateTime sentAt
) {}
