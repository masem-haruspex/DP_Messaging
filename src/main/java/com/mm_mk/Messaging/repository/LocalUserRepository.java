package com.mm_mk.Messaging.repository;
import com.mm_mk.Messaging.model.LocalUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LocalUserRepository extends JpaRepository<LocalUser, UUID> {

}
