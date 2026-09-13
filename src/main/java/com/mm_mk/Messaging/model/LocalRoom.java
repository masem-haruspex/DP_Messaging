package com.mm_mk.Messaging.model;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "local_rooms", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocalRoom {
    @Id
    private UUID id;

    @Column(name = "code", nullable = false, unique = true, length = 12)
    @Pattern(regexp = "^[A-Z0-9]{12}$", message = "Code must be 12 uppercase alphanumeric characters")
    private String code;

    @Column(name = "last_synced_at", nullable = false)
    @Builder.Default
    private LocalDateTime lastSyncedAt = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        if (lastSyncedAt == null) {
            lastSyncedAt = LocalDateTime.now();
        }
    }
}
