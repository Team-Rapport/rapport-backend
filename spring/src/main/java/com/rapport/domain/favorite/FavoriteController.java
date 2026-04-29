package com.rapport.domain.favorite;

import com.rapport.domain.counselor.dto.CounselorProfileDto;
import com.rapport.domain.counselor.entity.CounselorProfile;
import com.rapport.domain.counselor.entity.CounselorProfileRepository;
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
import lombok.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// ══════════════════════════════════════════════════════════════
// Entity
// ══════════════════════════════════════════════════════════════
@Entity
@Table(name = "favorites")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private User client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "counselor_id", nullable = false)
    private User counselor;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    static Favorite of(User client, User counselor) {
        Favorite f = new Favorite();
        f.client = client;
        f.counselor = counselor;
        return f;
    }
}

// ══════════════════════════════════════════════════════════════
// Repository
// ══════════════════════════════════════════════════════════════
@Repository
interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    Optional<Favorite> findByClientIdAndCounselorId(Long clientId, Long counselorId);
    List<Favorite> findAllByClientIdOrderByCreatedAtDesc(Long clientId);
    boolean existsByClientIdAndCounselorId(Long clientId, Long counselorId);
    long countByClientId(Long clientId);
}

// ══════════════════════════════════════════════════════════════
// Service
// ══════════════════════════════════════════════════════════════
@Service
@RequiredArgsConstructor
class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final CounselorProfileRepository counselorProfileRepository;

    @Transactional
    public boolean toggle(Long clientId, Long counselorId) {
        Optional<Favorite> existing =
                favoriteRepository.findByClientIdAndCounselorId(clientId, counselorId);

        if (existing.isPresent()) {
            favoriteRepository.delete(existing.get());
            return false; // 찜 해제
        }

        User client   = userRepository.findById(clientId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        User counselor = userRepository.findById(counselorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUNSELOR_NOT_FOUND));

        favoriteRepository.save(Favorite.of(client, counselor));
        return true; // 찜 추가
    }

    @Transactional(readOnly = true)
    public List<CounselorProfileDto.PublicProfileResponse> getMyFavorites(Long clientId) {
        return favoriteRepository.findAllByClientIdOrderByCreatedAtDesc(clientId)
                .stream()
                .map(f -> counselorProfileRepository.findByUserId(f.getCounselor().getId())
                        .map(this::toPublic).orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean isFavorite(Long clientId, Long counselorId) {
        return favoriteRepository.existsByClientIdAndCounselorId(clientId, counselorId);
    }

    @Transactional(readOnly = true)
    public long countMyFavorites(Long clientId) {
        return favoriteRepository.countByClientId(clientId);
    }

    private CounselorProfileDto.PublicProfileResponse toPublic(CounselorProfile p) {
        return CounselorProfileDto.PublicProfileResponse.builder()
                .userId(p.getUser().getId())
                .name(p.getUser().getName())
                .profileImageUrl(p.getUser().getProfileImageUrl())
                .licenseType(p.getLicenseType())
                .counselorGender(p.getCounselorGender())
                .specializations(p.getSpecializations())
                .approaches(p.getApproaches())
                .bio(p.getBio())
                .experienceYears(p.getExperienceYears())
                .averageRating(p.getAverageRating())
                .reviewCount(p.getReviewCount())
                .approvalStatus(p.getApprovalStatus())
                .approvedAt(p.getApprovedAt())
                .build();
    }
}

// ══════════════════════════════════════════════════════════════
// Controller
// ══════════════════════════════════════════════════════════════
@Tag(name = "Favorite", description = "찜한 상담사 API")
@RestController
@RequestMapping("/api/v1/favorites")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
class FavoriteController {

    private final FavoriteService favoriteService;

    @Operation(summary = "찜 추가/해제 (토글)", description = "이미 찜한 상담사면 해제, 아니면 추가")
    @PostMapping("/{counselorId}")
    public ResponseEntity<ApiResponse<Boolean>> toggle(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long counselorId) {
        boolean added = favoriteService.toggle(principal.getId(), counselorId);
        String msg = added ? "찜한 상담사에 추가했습니다." : "찜한 상담사에서 제거했습니다.";
        return ResponseEntity.ok(ApiResponse.ok(msg, added));
    }

    @Operation(summary = "찜한 상담사 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CounselorProfileDto.PublicProfileResponse>>> getMyFavorites(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                favoriteService.getMyFavorites(principal.getId())));
    }

    @Operation(summary = "찜 여부 확인")
    @GetMapping("/{counselorId}/check")
    public ResponseEntity<ApiResponse<Boolean>> checkFavorite(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long counselorId) {
        return ResponseEntity.ok(ApiResponse.ok(
                favoriteService.isFavorite(principal.getId(), counselorId)));
    }
}
