package com.rapport.domain.booking.service;

import com.rapport.domain.booking.dto.BookingDto;
import com.rapport.domain.booking.entity.Booking;
import com.rapport.domain.booking.entity.BookingRepository;
import com.rapport.domain.chat.dto.ChatMessageDto;
import com.rapport.domain.chat.service.ChatService;
import com.rapport.domain.user.entity.User;
import com.rapport.domain.user.entity.UserRepository;
import com.rapport.global.exception.BusinessException;
import com.rapport.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ChatService chatService;

    // ===== 예약 생성 (CLIENT) =====
    @Transactional
    public BookingDto.BookingResponse createBooking(Long clientId, BookingDto.CreateRequest request) {
        User client = getUser(clientId);
        User counselor = getUser(request.getCounselorId());

        Booking booking = Booking.create(
                client, counselor,
                request.getSessionTypeId(),
                request.getBookedDate(),
                request.getBookedStartTime(),
                request.getBookedEndTime(),
                request.getConcern()
        );
        bookingRepository.save(booking);

        log.info("Booking created: bookingId={}, clientId={}, counselorId={}",
                booking.getId(), clientId, counselor.getId());
        return toResponse(booking);
    }

    // ===== 예약 확정 (COUNSELOR) → 채팅방 자동 생성 =====
    @Transactional
    public BookingDto.ConfirmResponse confirmBooking(Long bookingId, Long counselorId) {
        Booking booking = bookingRepository.findByIdAndCounselorId(bookingId, counselorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOKING_NOT_FOUND));

        if (booking.getStatus() != Booking.BookingStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                    "PENDING 상태의 예약만 확정할 수 있습니다.");
        }

        booking.confirm();

        ChatMessageDto.RoomResponse room = chatService.createRoom(
                booking.getClient().getId(),
                booking.getCounselor().getId(),
                booking.getId()
        );

        log.info("Booking confirmed and chat room created: bookingId={}, roomId={}",
                bookingId, room.getRoomId());

        return BookingDto.ConfirmResponse.builder()
                .bookingId(booking.getId())
                .status(booking.getStatus())
                .roomId(room.getRoomId())
                .build();
    }

    // ===== 내 예약 목록 =====
    @Transactional(readOnly = true)
    public List<BookingDto.BookingResponse> getMyBookings(Long userId) {
        User user = getUser(userId);
        List<Booking> bookings = user.getRole() == User.Role.COUNSELOR
                ? bookingRepository.findAllByCounselorIdOrderByCreatedAtDesc(userId)
                : bookingRepository.findAllByClientIdOrderByCreatedAtDesc(userId);
        return bookings.stream().map(this::toResponse).toList();
    }

    private BookingDto.BookingResponse toResponse(Booking b) {
        return BookingDto.BookingResponse.builder()
                .bookingId(b.getId())
                .clientId(b.getClient().getId())
                .counselorId(b.getCounselor().getId())
                .status(b.getStatus())
                .bookedDate(b.getBookedDate())
                .bookedStartTime(b.getBookedStartTime())
                .bookedEndTime(b.getBookedEndTime())
                .concern(b.getConcern())
                .createdAt(b.getCreatedAt())
                .build();
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
