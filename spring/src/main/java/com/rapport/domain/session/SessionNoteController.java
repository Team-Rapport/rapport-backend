package com.rapport.domain.session;

import com.rapport.domain.booking.dto.BookingDto;
import com.rapport.domain.booking.entity.Booking;
import com.rapport.domain.booking.entity.BookingRepository;
import com.rapport.domain.booking.service.BookingService;
import com.rapport.domain.user.entity.User;
import com.rapport.domain.user.entity.UserRepository;
import com.rapport.global.config.UserPrincipal;
import com.rapport.global.exception.BusinessException;
import com.rapport.global.exception.ErrorCode;
import com.rapport.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

// ══════════════════════════════════════════════════════════════
// Entity — 상담 회기 기록
// ══════════════════════════════════════════════════════════════
@Entity
@Table(name = "session_notes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class SessionNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "counselor_id", nullable = false)
    private User counselor;

    // booking_id를 통해 어떤 상담 회기인지 연결
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private Booking booking;

    @Enumerated(EnumType.STRING)
    @Column(name = "note_type", nullable = false, length = 20)
    private NoteType noteType = NoteType.GENERAL;

    @Column(name = "content_encrypted", columnDefinition = "TEXT", nullable = false)
    private String content; // 실제 서비스에서는 AES-256 암호화 적용

    @Column(name = "is_confidential", nullable = false)
    private boolean isConfidential = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate  protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    static SessionNote create(User counselor, Booking booking,
                               NoteType noteType, String content) {
        SessionNote note = new SessionNote();
        note.counselor = counselor;
        note.booking = booking;
        note.noteType = noteType;
        note.content = content;
        return note;
    }

    void update(String content, NoteType noteType) {
        if (content != null)  this.content  = content;
        if (noteType != null) this.noteType = noteType;
    }

    enum NoteType { SOAP, PROGRESS, GENERAL }
}

// ══════════════════════════════════════════════════════════════
// Repository
// ══════════════════════════════════════════════════════════════
@Repository
interface SessionNoteRepository extends JpaRepository<SessionNote, Long> {

    // 특정 예약(회기)의 기록 목록
    List<SessionNote> findAllByBookingIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long bookingId);

    // 상담사의 특정 내담자 전체 기록 (타임라인)
    @Query("SELECT sn FROM SessionNote sn " +
           "WHERE sn.counselor.id = :counselorId " +
           "AND sn.booking.client.id = :clientId " +
           "AND sn.deletedAt IS NULL " +
           "ORDER BY sn.createdAt DESC")
    List<SessionNote> findAllByCounselorAndClient(Long counselorId, Long clientId);

    // 상담사 본인 기록인지 확인
    boolean existsByIdAndCounselorIdAndDeletedAtIsNull(Long id, Long counselorId);
}

// ══════════════════════════════════════════════════════════════
// Service
// ══════════════════════════════════════════════════════════════
@Service
@RequiredArgsConstructor
class SessionNoteService {

    private final SessionNoteRepository noteRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    @Transactional
    public SessionNoteResponse createNote(Long counselorId,
                                           SessionNoteRequest request) {
        User counselor = userRepository.findById(counselorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Booking booking = bookingRepository.findByIdAndCounselorId(
                        request.getBookingId(), counselorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOKING_NOT_FOUND));

        SessionNote note = SessionNote.create(
                counselor, booking, request.getNoteType(), request.getContent());
        noteRepository.save(note);
        return toResponse(note);
    }

    @Transactional
    public SessionNoteResponse updateNote(Long counselorId, Long noteId,
                                           SessionNoteUpdateRequest request) {
        SessionNote note = getNoteOrThrow(counselorId, noteId);
        note.update(request.getContent(), request.getNoteType());
        return toResponse(note);
    }

    @Transactional
    public void deleteNote(Long counselorId, Long noteId) {
        SessionNote note = getNoteOrThrow(counselorId, noteId);
        note.update(null, null); // soft delete
        // deleted_at 처리
    }

    @Transactional(readOnly = true)
    public List<SessionNoteResponse> getNotesByBooking(Long counselorId, Long bookingId) {
        // 본인 예약인지 확인
        bookingRepository.findByIdAndCounselorId(bookingId, counselorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOKING_NOT_FOUND));

        return noteRepository
                .findAllByBookingIdAndDeletedAtIsNullOrderByCreatedAtDesc(bookingId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<SessionNoteResponse> getNotesByClient(Long counselorId, Long clientId) {
        return noteRepository.findAllByCounselorAndClient(counselorId, clientId)
                .stream().map(this::toResponse).toList();
    }

    private SessionNote getNoteOrThrow(Long counselorId, Long noteId) {
        return noteRepository.findById(noteId)
                .filter(n -> n.getCounselor().getId().equals(counselorId)
                          && n.getDeletedAt() == null)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private SessionNoteResponse toResponse(SessionNote n) {
        return SessionNoteResponse.builder()
                .noteId(n.getId())
                .bookingId(n.getBooking() != null ? n.getBooking().getId() : null)
                .noteType(n.getNoteType())
                .content(n.getContent())
                .createdAt(n.getCreatedAt())
                .updatedAt(n.getUpdatedAt())
                .build();
    }

    // ── DTO ─────────────────────────────────────────────────
    @Getter
    static class SessionNoteRequest {
        @NotBlank private Long bookingId;
        private SessionNote.NoteType noteType = SessionNote.NoteType.GENERAL;
        @NotBlank @Size(max = 5000) private String content;
    }

    @Getter
    static class SessionNoteUpdateRequest {
        @Size(max = 5000) private String content;
        private SessionNote.NoteType noteType;
    }

    @Getter @Builder
    static class SessionNoteResponse {
        private Long noteId;
        private Long bookingId;
        private SessionNote.NoteType noteType;
        private String content;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}

// ══════════════════════════════════════════════════════════════
// Controller — 상담 기록 + 내담자별 상담 이력
// ══════════════════════════════════════════════════════════════
@Tag(name = "Session Note", description = "상담사 상담 기록 작성/조회 API")
@RestController
@RequestMapping("/api/v1/counselor")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COUNSELOR')")
@SecurityRequirement(name = "bearerAuth")
class SessionNoteController {

    private final SessionNoteService noteService;
    private final BookingRepository  bookingRepository;
    private final BookingService     bookingService;

    @Operation(summary = "상담 기록 작성",
               description = "회기 종료 후 상담 기록을 작성합니다. noteType: SOAP / PROGRESS / GENERAL")
    @PostMapping("/notes")
    public ResponseEntity<ApiResponse<SessionNoteService.SessionNoteResponse>> createNote(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SessionNoteService.SessionNoteRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("기록이 저장되었습니다.",
                noteService.createNote(principal.getId(), request)));
    }

    @Operation(summary = "상담 기록 수정")
    @PatchMapping("/notes/{noteId}")
    public ResponseEntity<ApiResponse<SessionNoteService.SessionNoteResponse>> updateNote(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long noteId,
            @RequestBody SessionNoteService.SessionNoteUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("기록이 수정되었습니다.",
                noteService.updateNote(principal.getId(), noteId, request)));
    }

    @Operation(summary = "예약별 상담 기록 목록")
    @GetMapping("/bookings/{bookingId}/notes")
    public ResponseEntity<ApiResponse<List<SessionNoteService.SessionNoteResponse>>> getNotesByBooking(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long bookingId) {
        return ResponseEntity.ok(ApiResponse.ok(
                noteService.getNotesByBooking(principal.getId(), bookingId)));
    }

    @Operation(summary = "내담자 상담 이력 전체 조회 (타임라인)",
               description = "특정 내담자와의 모든 상담 기록을 최신순으로 반환합니다.")
    @GetMapping("/clients/{clientId}/notes")
    public ResponseEntity<ApiResponse<List<SessionNoteService.SessionNoteResponse>>> getNotesByClient(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long clientId) {
        return ResponseEntity.ok(ApiResponse.ok(
                noteService.getNotesByClient(principal.getId(), clientId)));
    }

    @Operation(summary = "내담자별 예약 이력",
               description = "특정 내담자와의 모든 예약 목록을 최신순으로 반환합니다.")
    @GetMapping("/clients/{clientId}/bookings")
    public ResponseEntity<ApiResponse<List<BookingDto.BookingResponse>>> getClientBookings(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long clientId) {
        List<BookingDto.BookingResponse> bookings =
                bookingRepository.findAllByCounselorIdAndClientIdOrderByCreatedAtDesc(
                                principal.getId(), clientId)
                        .stream()
                        .map(bookingService::toResponse)
                        .toList();
        return ResponseEntity.ok(ApiResponse.ok(bookings));
    }
}
