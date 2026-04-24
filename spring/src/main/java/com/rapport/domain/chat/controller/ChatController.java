package com.rapport.domain.chat.controller;

import com.rapport.domain.chat.dto.ChatMessageDto;
import com.rapport.domain.chat.entity.ChatMessage;
import com.rapport.domain.chat.service.ChatService;
import com.rapport.global.config.UserPrincipal;
import com.rapport.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    // ──────────────────────────────────────────────
    // STOMP: 메시지 전송
    // - 구독: /topic/chat.{roomId}
    // - 전송: /app/chat.{roomId}
    // ──────────────────────────────────────────────

    @MessageMapping("/chat.{roomId}")
    public void sendMessage(@DestinationVariable Long roomId,
                            @Payload ChatMessageDto.Request request,
                            Principal principal) {
        UserPrincipal userPrincipal = (UserPrincipal) ((org.springframework.security.authentication.UsernamePasswordAuthenticationToken) principal).getPrincipal();
        Long senderId = userPrincipal.getId();

        ChatMessageDto.Response response = chatService.saveMessage(
                roomId, senderId, request.getContent(),
                request.getMessageType() != null ? request.getMessageType() : ChatMessage.MessageType.TEXT
        );

        messagingTemplate.convertAndSend("/topic/chat." + roomId, response);
    }

    // ──────────────────────────────────────────────
    // REST: 채팅방 목록 조회
    // GET /api/v1/chat/rooms
    // ──────────────────────────────────────────────

    @GetMapping("/api/v1/chat/rooms")
    public ApiResponse<List<ChatMessageDto.RoomResponse>> getMyRooms(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(chatService.getMyRooms(principal.getId()));
    }

    // ──────────────────────────────────────────────
    // REST: 메시지 히스토리 조회
    // GET /api/v1/chat/rooms/{roomId}/messages
    // ──────────────────────────────────────────────

    @GetMapping("/api/v1/chat/rooms/{roomId}/messages")
    public ApiResponse<ChatMessageDto.HistoryResponse> getHistory(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(chatService.getHistory(roomId, principal.getId()));
    }
}
