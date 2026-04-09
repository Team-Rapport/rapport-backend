package com.rapport.domain.chat.entity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AiChatSessionRepository extends JpaRepository<AiChatSession, Long> {
    Page<AiChatSession> findAllByClientIdOrderByCreatedAtDesc(Long clientId, Pageable pageable);
    Optional<AiChatSession> findByIdAndClientId(Long sessionId, Long clientId);
    Optional<AiChatSession> findTopByClientIdAndStatusOrderByCreatedAtDesc(
            Long clientId, AiChatSession.SessionStatus status);
}
