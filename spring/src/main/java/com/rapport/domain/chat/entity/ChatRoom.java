package com.rapport.domain.chat.entity;

import com.rapport.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity(name = "AiChatRoom")
@Table(name = "chat_rooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id")
    private Long bookingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private User client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "counselor_id", nullable = false)
    private User counselor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Status status = Status.OPEN;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public static ChatRoom create(User client, User counselor, Long bookingId) {
        ChatRoom room = new ChatRoom();
        room.client = client;
        room.counselor = counselor;
        room.bookingId = bookingId;
        room.status = Status.OPEN;
        return room;
    }

    public void close() {
        this.status = Status.CLOSED;
    }

    public boolean isParticipant(Long userId) {
        return client.getId().equals(userId) || counselor.getId().equals(userId);
    }

    public enum Status {
        OPEN, CLOSED
    }
}
