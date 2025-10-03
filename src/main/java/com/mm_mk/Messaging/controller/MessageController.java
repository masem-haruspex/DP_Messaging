package com.mm_mk.Messaging.controller;

import com.mm_mk.Messaging.request.SendMessageRequest;
import com.mm_mk.Messaging.response.MessageResponse;
import com.mm_mk.Messaging.service.MessagingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private MessagingService messagingService;

    @PostMapping
    public ResponseEntity<MessageResponse> sendMessage(
            @RequestHeader("X-User-ID") UUID userId,
            @Valid @RequestBody SendMessageRequest request) {
        MessageResponse message = messagingService.sendMessage(
                request.roomCode(),
                userId,
                request.content()
        );
        return ResponseEntity.status(201).body(message);
    }

    @GetMapping("/rooms/{roomCode}")
    public ResponseEntity<List<MessageResponse>> getMessages(@PathVariable String roomCode) {
        List<MessageResponse> messages = messagingService.getMessages(roomCode);
        return ResponseEntity.ok(messages);
    }
}