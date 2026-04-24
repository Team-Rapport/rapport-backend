package com.rapport.domain.chat.dto;

import com.rapport.domain.chat.entity.ChatMessage;
import com.rapport.domain.chat.entity.ChatRoom;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

public class ChatMessageDto {

    /** 클라이언트 → 서버: 메시지 전송 */
    @Getter
    public static class Request {
        private String content;
        private ChatMessage.MessageType messageType = ChatMessage.MessageType.TEXT;
    }

    /** 서버 → 클라이언트: 브로드캐스트 메시지 */
    @Getter
    public static class Response {
        private final Long id;
        private final Long roomId;
        private final Long senderId;
        private final String senderName;
        private final String content;
        private final ChatMessage.MessageType messageType;
        private final LocalDateTime sentAt;

        public Response(ChatMessage message) {
            this.id = message.getId();
            this.roomId = message.getRoom().getId();
            this.senderId = message.getSender().getId();
            this.senderName = message.getSender().getName();
            this.content = message.getContent();
            this.messageType = message.getMessageType();
            this.sentAt = message.getSentAt();
        }
    }

    /** 채팅방 목록 조회 응답 */
    @Getter
    public static class RoomResponse {
        private final Long roomId;
        private final Long clientId;
        private final String clientName;
        private final Long counselorId;
        private final String counselorName;
        private final ChatRoom.Status status;
        private final LocalDateTime createdAt;

        public RoomResponse(ChatRoom room) {
            this.roomId = room.getId();
            this.clientId = room.getClient().getId();
            this.clientName = room.getClient().getName();
            this.counselorId = room.getCounselor().getId();
            this.counselorName = room.getCounselor().getName();
            this.status = room.getStatus();
            this.createdAt = room.getCreatedAt();
        }
    }

    /** 메시지 히스토리 조회 응답 */
    @Getter
    public static class HistoryResponse {
        private final Long roomId;
        private final List<Response> messages;

        public HistoryResponse(Long roomId, List<Response> messages) {
            this.roomId = roomId;
            this.messages = messages;
        }
    }
}
