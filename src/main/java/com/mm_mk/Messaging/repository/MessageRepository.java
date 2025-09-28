// src/main/java/com/mm_mk/Messaging/repository/MessageRepository.java
package com.mm_mk.Messaging.repository;

import com.mm_mk.Messaging.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {
    List<Message> findByRoomIdOrderBySentAtAsc(UUID roomId);
    void deleteByRoomId(UUID roomId);
}