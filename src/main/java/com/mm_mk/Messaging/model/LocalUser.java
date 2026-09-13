package com.mm_mk.Messaging.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "local_users", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocalUser {

    @Id
    private UUID id; 

    @Column(name = "username", nullable = false, length = 50)
    @NotBlank(message = "Username is required")
    @Size(min = 4, max = 50, message = "Username must be between 4 and 50 characters")
    private String username;

    @Column(name = "preferred_keyboard", length = 20)
    @Builder.Default
    private String preferredKeyboard = "Casio";

    @Column(name = "last_synced_at", nullable = false)
    @Builder.Default
    private LocalDateTime lastSyncedAt = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        if (lastSyncedAt == null) {
            lastSyncedAt = LocalDateTime.now();
        }
        if (preferredKeyboard == null) {
            preferredKeyboard = "Casio";
        }
    }
}
