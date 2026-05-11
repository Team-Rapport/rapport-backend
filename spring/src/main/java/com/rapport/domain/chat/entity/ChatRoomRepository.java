package com.rapport.domain.chat.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository("aiChatRoomRepository")
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    @Query("SELECT r FROM AiChatRoom r WHERE r.client.id = :userId OR r.counselor.id = :userId")
    List<ChatRoom> findAllByParticipant(@Param("userId") Long userId);

    Optional<ChatRoom> findByBookingId(Long bookingId);
}
