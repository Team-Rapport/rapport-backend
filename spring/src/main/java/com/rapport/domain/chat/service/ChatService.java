package com.rapport.domain.chat.service;

import com.rapport.domain.chat.dto.ChatMessageDto;
import com.rapport.domain.chat.entity.*;
import com.rapport.domain.user.entity.User;
import com.rapport.domain.user.entity.UserRepository;
import com.rapport.global.exception.BusinessException;
import com.rapport.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final int MESSAGE_PAGE_SIZE = 50;

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    // ===== 채팅방 생성 =====

    @Transactional
    public ChatMessageDto.RoomResponse createRoom(Long clientId, Long counselorId, Long bookingId) {
        User client = getUser(clientId);
        User counselor = getUser(counselorId);
        ChatRoom room = ChatRoom.create(client, counselor, bookingId);
        return new ChatMessageDto.RoomResponse(chatRoomRepository.save(room));
    }

    // ===== 메시지 저장 =====

    @Transactional
    public ChatMessageDto.Response saveMessage(Long roomId, Long senderId,
                                               String content, ChatMessage.MessageType type) {
        ChatRoom room = getRoom(roomId);
        if (room.getStatus() == ChatRoom.Status.CLOSED) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        if (!room.isParticipant(senderId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        User sender = getUser(senderId);
        ChatMessage message = ChatMessage.create(room, sender, content, type);
        return new ChatMessageDto.Response(chatMessageRepository.save(message));
    }

    // ===== 메시지 히스토리 조회 =====

    @Transactional(readOnly = true)
    public ChatMessageDto.HistoryResponse getHistory(Long roomId, Long requesterId) {
        ChatRoom room = getRoom(roomId);
        if (!room.isParticipant(requesterId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        List<ChatMessageDto.Response> messages = chatMessageRepository
                .findByRoomIdOrderBySentAtAsc(roomId, PageRequest.of(0, MESSAGE_PAGE_SIZE))
                .stream()
                .map(ChatMessageDto.Response::new)
                .toList();
        return new ChatMessageDto.HistoryResponse(roomId, messages);
    }

    // ===== 내 채팅방 목록 조회 =====

    @Transactional(readOnly = true)
    public List<ChatMessageDto.RoomResponse> getMyRooms(Long userId) {
        return chatRoomRepository.findAllByParticipant(userId)
                .stream()
                .map(ChatMessageDto.RoomResponse::new)
                .toList();
    }

    // ===== 내부 유틸 =====

    private ChatRoom getRoom(Long roomId) {
        return chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }
}
