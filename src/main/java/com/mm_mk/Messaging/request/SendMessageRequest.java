package com.mm_mk.Messaging.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
        @Schema(description = "Room access code", example = "ABC123")
        @NotBlank
        @Pattern(regexp = "^[A-Z0-9]{12}$", message = "Room code must be 12 alphanumeric characters")
        String roomCode,

        @Schema(description = "Message content (max 1000 characters)", example = "Hello everyone!", maxLength = 1000)
        @NotBlank
        @Size(max = 1000, message = "Message too long (max 1000 characters)")
        String content
) {}
