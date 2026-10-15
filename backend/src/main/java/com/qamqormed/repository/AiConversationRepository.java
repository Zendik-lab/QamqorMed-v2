package com.qamqormed.repository;

import com.qamqormed.entity.AiConversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiConversationRepository extends JpaRepository<AiConversation, Long> {
    List<AiConversation> findAllByOwnerIdOrderByUpdatedAtDesc(Long ownerId);
    Optional<AiConversation> findByIdAndOwnerId(Long id, Long ownerId);
}
