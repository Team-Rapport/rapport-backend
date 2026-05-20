package com.rapport.domain.directchat.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findByClientIdAndCounselorId(Long clientId, Long counselorId);

    @Query("SELECT r FROM DirectChatRoom r WHERE r.client.id = :userId OR r.counselor.id = :userId " +
           "ORDER BY r.lastMessageAt DESC NULLS LAST")
    List<ChatRoom> findAllByUserId(Long userId);
}
