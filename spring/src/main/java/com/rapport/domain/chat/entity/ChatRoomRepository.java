package com.rapport.domain.chat.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    @Query("SELECT r FROM ChatRoom r WHERE r.client.id = :userId OR r.counselor.id = :userId")
    List<ChatRoom> findAllByParticipant(@Param("userId") Long userId);

    Optional<ChatRoom> findByBookingId(Long bookingId);
}
