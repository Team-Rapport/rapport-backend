package com.rapport.domain.directchat;

import com.rapport.domain.directchat.entity.ChatRoom;
import com.rapport.domain.directchat.entity.ChatRoomRepository;
import com.rapport.domain.directchat.entity.DirectMessage;
import com.rapport.domain.directchat.entity.DirectMessageRepository;
import com.rapport.domain.user.entity.User;
import com.rapport.domain.user.entity.UserRepository;
import com.rapport.global.config.UserPrincipal;
import com.rapport.global.exception.BusinessException;
import com.rapport.global.exception.ErrorCode;
import com.rapport.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

// ══════════════════════════════════════════════════════════════
// STOMP 메시지 핸들러 (실시간 채팅)
// ══════════════════════════════════════════════════════════════
@Slf4j
@Controller
@RequiredArgsConstructor
class StompChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatRoomRepository    chatRoomRepository;
    private final DirectMessageRepository directMessageRepository;
    private final UserRepository        userRepository;

    /**
     * 메시지 전송
     * 클라이언트 발행: /app/chat/{roomId}
     * 구독 경로:      /topic/chat/{roomId}
     */
    @MessageMapping("/chat/{roomId}")
    @Transactional
    public void sendMessage(@DestinationVariable Long roomId,
                             @Payload ChatMessage payload,
                             Principal principal) {
        if (principal == null) return;

        UserPrincipal userPrincipal = (UserPrincipal) ((org.springframework.security.authentication
                .UsernamePasswordAuthenticationToken) principal).getPrincipal();

        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        // 채팅방 참여자 검증
        Long senderId = userPrincipal.getId();
        boolean isParticipant = room.getClient().getId().equals(senderId)
                || room.getCounselor().getId().equals(senderId);
        if (!isParticipant) return;

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // DB 저장
        DirectMessage message = DirectMessage.of(
                room, sender, payload.getContent(),
                DirectMessage.MessageType.valueOf(
                        payload.getType() != null ? payload.getType() : "TEXT"));
        directMessageRepository.save(message);
        room.updateLastMessageAt();

        // 구독자에게 브로드캐스트
        ChatMessageResponse response = ChatMessageResponse.builder()
                .messageId(message.getId())
                .roomId(roomId)
                .senderId(senderId)
                .senderName(sender.getName())
                .content(message.getContent())
                .type(message.getMessageType().name())
                .createdAt(message.getCreatedAt())
                .build();

        messagingTemplate.convertAndSend("/topic/chat/" + roomId, response);
        log.debug("Message sent: roomId={}, senderId={}", roomId, senderId);
    }

    // ===== DTO =====
    @Getter
    static class ChatMessage {
        private String content;
        private String type; // TEXT | IMAGE | FILE
    }

    @Getter @Builder
    static class ChatMessageResponse {
        private Long          messageId;
        private Long          roomId;
        private Long          senderId;
        private String        senderName;
        private String        content;
        private String        type;
        private LocalDateTime createdAt;
    }
}

// ══════════════════════════════════════════════════════════════
// REST Controller (채팅방 생성/목록/이력)
// ══════════════════════════════════════════════════════════════
@Tag(name = "Direct Chat", description = "상담사-내담자 1:1 채팅 API")
@RestController
@RequestMapping("/api/v1/direct/rooms")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
class ChatRoomController {

    private final ChatRoomRepository      chatRoomRepository;
    private final DirectMessageRepository directMessageRepository;
    private final UserRepository          userRepository;

    @Operation(summary = "채팅방 생성 또는 조회",
               description = "내담자-상담사 간 채팅방이 없으면 생성, 있으면 기존 방 반환")
    @PostMapping
    @Transactional
    public ResponseEntity<ApiResponse<ChatRoomResponse>> getOrCreateRoom(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody CreateRoomRequest request) {

        Long clientId   = principal.getId();
        Long counselorId = request.getCounselorId();

        ChatRoom room = chatRoomRepository
                .findByClientIdAndCounselorId(clientId, counselorId)
                .orElseGet(() -> {
                    User client   = userRepository.findById(clientId)
                            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
                    User counselor = userRepository.findById(counselorId)
                            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
                    return chatRoomRepository.save(ChatRoom.create(client, counselor));
                });

        return ResponseEntity.ok(ApiResponse.ok(toChatRoomResponse(room, principal.getId())));
    }

    @Operation(summary = "내 채팅방 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ChatRoomResponse>>> getMyChatRooms(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<ChatRoomResponse> rooms = chatRoomRepository
                .findAllByUserId(principal.getId())
                .stream()
                .map(r -> toChatRoomResponse(r, principal.getId()))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(rooms));
    }

    @Operation(summary = "채팅 이력 조회")
    @GetMapping("/{roomId}/messages")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getMessages(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long roomId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        // 참여자 검증
        Long myId = principal.getId();
        if (!room.getClient().getId().equals(myId) && !room.getCounselor().getId().equals(myId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        List<MessageResponse> messages = directMessageRepository
                .findAllByRoomIdOrderByCreatedAtAsc(roomId)
                .stream()
                .map(m -> MessageResponse.builder()
                        .messageId(m.getId())
                        .senderId(m.getSender().getId())
                        .senderName(m.getSender().getName())
                        .content(m.getContent())
                        .type(m.getMessageType().name())
                        .isRead(m.isRead())
                        .createdAt(m.getCreatedAt())
                        .build())
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(messages));
    }

    // ===== DTO =====
    @Getter
    static class CreateRoomRequest {
        private Long counselorId;
    }

    @Getter @Builder
    static class ChatRoomResponse {
        private Long   roomId;
        private Long   clientId;
        private String clientName;
        private Long   counselorId;
        private String counselorName;
        private long   unreadCount;
        private LocalDateTime lastMessageAt;
        private LocalDateTime createdAt;
    }

    @Getter @Builder
    static class MessageResponse {
        private Long          messageId;
        private Long          senderId;
        private String        senderName;
        private String        content;
        private String        type;
        private boolean       isRead;
        private LocalDateTime createdAt;
    }

    private ChatRoomResponse toChatRoomResponse(ChatRoom r, Long myId) {
        long unread = directMessageRepository
                .countByRoomIdAndIsReadFalseAndSenderIdNot(r.getId(), myId);
        return ChatRoomResponse.builder()
                .roomId(r.getId())
                .clientId(r.getClient().getId())
                .clientName(r.getClient().getName())
                .counselorId(r.getCounselor().getId())
                .counselorName(r.getCounselor().getName())
                .unreadCount(unread)
                .lastMessageAt(r.getLastMessageAt())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
