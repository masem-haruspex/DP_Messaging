// src/main/java/com/mm_mk/Messaging/service/RoomServiceClient.java
package com.mm_mk.Messaging.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class RoomServiceClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String roomsServiceUrl;

    public RoomServiceClient(RestTemplate restTemplate, ObjectMapper objectMapper,
                             @Value("${rooms.service.url:http://localhost:8082}") String roomsServiceUrl) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.roomsServiceUrl = roomsServiceUrl;
    }

    public RoomResponse getRoomByCode(String code) {
        String url = roomsServiceUrl + "/api/rooms/" + code;
        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Room not found: " + code);
        }

        Map<String, Object> body = response.getBody();
        return new RoomResponse(
                UUID.fromString((String) body.get("id")),
                (String) body.get("code"),
                (String) body.get("name"),
                UUID.fromString((String) body.get("ownerId")),
                (Boolean) body.get("isPrivate"),
                (Integer) body.get("maxParticipants"),
                objectMapper.convertValue(body.get("createdAt"), java.time.LocalDateTime.class)
        );
    }

    // DTO class (static nested)
    public static class RoomResponse {
        private final UUID id;
        private final String code;
        private final String name;
        private final UUID ownerId;
        private final Boolean isPrivate;
        private final Integer maxParticipants;
        private final java.time.LocalDateTime createdAt;

        public RoomResponse(UUID id, String code, String name, UUID ownerId,
                            Boolean isPrivate, Integer maxParticipants, LocalDateTime createdAt) {
            this.id = id;
            this.code = code;
            this.name = name;
            this.ownerId = ownerId;
            this.isPrivate = isPrivate;
            this.maxParticipants = maxParticipants;
            this.createdAt = createdAt;
        }

        // Getters
        public UUID getId() { return id; }
        public String getCode() { return code; }
        public String getName() { return name; }
        public UUID getOwnerId() { return ownerId; }
        public Boolean getIsPrivate() { return isPrivate; }
        public Integer getMaxParticipants() { return maxParticipants; }
        public java.time.LocalDateTime getCreatedAt() { return createdAt; }
    }
}