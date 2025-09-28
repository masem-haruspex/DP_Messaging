// src/main/java/com/mm_mk/Messaging/response/MessageResponse.java
package com.mm_mk.Messaging.response;

import java.util.UUID;

public record MessageResponse(
    UUID id,
    UUID roomId,
    UUID userId,
    String content,
    java.time.LocalDateTime sentAt
) {}
