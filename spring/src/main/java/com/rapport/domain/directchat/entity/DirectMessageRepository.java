package com.rapport.domain.directchat.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DirectMessageRepository extends JpaRepository<DirectMessage, Long> {
    List<DirectMessage> findAllByRoomIdOrderByCreatedAtAsc(Long roomId);
    long countByRoomIdAndIsReadFalseAndSenderIdNot(Long roomId, Long myId);
}
