package com.mm_mk.Messaging.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
        @NotBlank String roomCode,
        @NotBlank @Size(max = 1000) String content
) {}