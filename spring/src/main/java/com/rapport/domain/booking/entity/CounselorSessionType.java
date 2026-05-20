package com.rapport.domain.booking.entity;

import com.rapport.domain.user.entity.User;
import com.rapport.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// ══════════════════════════════════════════════════════════════
// Entity
// ══════════════════════════════════════════════════════════════
@Entity
@Table(name = "counselor_session_types")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CounselorSessionType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "counselor_id", nullable = false)
    private User counselor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_type_id", nullable = false)
    private SessionType sessionType;

    @Column(nullable = false)
    private int price;

    public static CounselorSessionType of(User counselor, SessionType sessionType, int price) {
        CounselorSessionType c = new CounselorSessionType();
        c.counselor = counselor;
        c.sessionType = sessionType;
        c.price = price;
        return c;
    }

    public void updatePrice(int price) { this.price = price; }
}

// ══════════════════════════════════════════════════════════════
// Controller
// ══════════════════════════════════════════════════════════════
@Tag(name = "Counselor Session Type", description = "상담사 상담 방식별 가격 API")
@RestController
@RequestMapping("/api/v1/counselors/{counselorId}/session-types")
@RequiredArgsConstructor
class CounselorSessionTypeController {

    private final CounselorSessionTypeRepository repository;

    @Operation(summary = "상담사 상담 유형/가격 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<SessionTypePrice>>> getSessionTypes(
            @PathVariable Long counselorId) {
        List<SessionTypePrice> result = repository.findAllByCounselorId(counselorId)
                .stream()
                .map(c -> new SessionTypePrice(
                        c.getId(),
                        c.getSessionType().getId(),
                        c.getSessionType().getName(),
                        c.getPrice()))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    record SessionTypePrice(Long id, Long sessionTypeId, String name, int price) {}
}
