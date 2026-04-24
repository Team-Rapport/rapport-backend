package com.rapport.domain.chat.entity;

import com.rapport.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoom room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 10)
    private MessageType messageType = MessageType.TEXT;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Column(name = "sent_at", nullable = false, updatable = false)
    private LocalDateTime sentAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        sentAt = LocalDateTime.now();
        createdAt = LocalDateTime.now();
    }

    public static ChatMessage create(ChatRoom room, User sender, String content, MessageType type) {
        ChatMessage msg = new ChatMessage();
        msg.room = room;
        msg.sender = sender;
        msg.content = content;
        msg.messageType = type;
        return msg;
    }

    public void markAsRead() {
        this.isRead = true;
    }

    public enum MessageType {
        TEXT, IMAGE, SYSTEM
    }
}
