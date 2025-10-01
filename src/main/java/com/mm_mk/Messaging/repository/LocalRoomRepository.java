package com.mm_mk.Messaging.repository;
import com.mm_mk.Messaging.model.LocalRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LocalRoomRepository extends JpaRepository<LocalRoom, UUID> {
    Optional<LocalRoom> findByCode(String code);
}
