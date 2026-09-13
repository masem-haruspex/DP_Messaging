package com.mm_mk.Messaging.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI messagingOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Messaging Service API")
                        .description("""
                                        Microservice for real-time messaging and WebSocket communication.
                                        
                                        ## WebSocket Endpoints
                                        - Connect: `ws://localhost:8080/ws`
                                        - Send message: `/app/rooms/{roomCode}/sendMessage`
                                        - Keyboard events: `/app/rooms/{roomCode}/keyEvent`
                                        - User join: `/app/rooms/{roomCode}/join`
                                        - User leave: `/app/rooms/{roomCode}/leave`
                                        
                                        ## Subscribe to Topics
                                        - Chat messages: `/topic/rooms/{roomCode}/chat`
                                        - Keyboard events: `/topic/rooms/{roomCode}/keyEvents`
                                        - Participant updates: `/topic/rooms/{roomCode}/participants`
                                        """)
                        .version("v1.0"))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .name("bearerAuth")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
