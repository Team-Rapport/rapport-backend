package com.rapport.domain.booking.controller;

import com.rapport.domain.booking.entity.CounselorSessionTypeRepository;
import com.rapport.domain.booking.entity.SessionType;
import com.rapport.domain.booking.entity.SessionTypeRepository;
import com.rapport.domain.user.entity.User;
import com.rapport.domain.user.entity.UserRepository;
import com.rapport.global.config.UserPrincipal;
import com.rapport.global.exception.BusinessException;
import com.rapport.global.exception.ErrorCode;
import com.rapport.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Counselor Session Type", description = "상담사 상담 방식별 가격 API")
@RestController
@RequestMapping("/api/v1/counselors/{counselorId}/session-types")
@RequiredArgsConstructor
public class CounselorSessionTypeController {

    private final CounselorSessionTypeRepository repository;
    private final SessionTypeRepository sessionTypeRepository;
    private final UserRepository userRepository;

    @Operation(summary = "상담사 상담 유형/가격 목록 조회")
    @GetMapping
    @Transactional(readOnly = true)
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

    @Operation(summary = "상담 유형 가격 단건 수정 (상담사)")
    @PatchMapping("/api/v1/counselor/session-types/{id}")
    @PreAuthorize("hasRole('COUNSELOR')")
    @Transactional
    public ResponseEntity<ApiResponse<SessionTypePrice>> updateMySessionTypePrice(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdatePriceRequest request) {
        var sessionType = repository.findByIdAndCounselorId(id, principal.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        sessionType.updatePrice(request.getPrice());
        SessionTypePrice result = new SessionTypePrice(
                sessionType.getId(),
                sessionType.getSessionType().getId(),
                sessionType.getSessionType().getName(),
                sessionType.getPrice()
        );
        return ResponseEntity.ok(ApiResponse.ok("가격이 수정되었습니다.", result));
    }

    @Operation(summary = "상담 유형 가격 일괄 저장 (상담사)",
               description = "sessionTypeId 기준으로 upsert 처리합니다. 기존 항목은 가격 갱신, 없으면 신규 생성합니다.")
    @PutMapping("/api/v1/counselor/session-types")
    @PreAuthorize("hasRole('COUNSELOR')")
    @Transactional
    public ResponseEntity<ApiResponse<List<SessionTypePrice>>> upsertMySessionTypePrices(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpsertSessionTypePricesRequest request) {
        User counselor = userRepository.findById(principal.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        for (UpsertSessionTypePriceItem item : request.getItems()) {
            SessionType sessionType = sessionTypeRepository.findById(item.getSessionTypeId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
            repository.findByCounselorIdAndSessionTypeId(principal.getId(), item.getSessionTypeId())
                    .ifPresentOrElse(
                            existing -> existing.updatePrice(item.getPrice()),
                            () -> repository.save(com.rapport.domain.booking.entity.CounselorSessionType.of(
                                    counselor, sessionType, item.getPrice()))
                    );
        }

        List<SessionTypePrice> result = repository.findAllByCounselorId(principal.getId())
                .stream()
                .map(c -> new SessionTypePrice(
                        c.getId(),
                        c.getSessionType().getId(),
                        c.getSessionType().getName(),
                        c.getPrice()))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok("상담 유형 가격이 저장되었습니다.", result));
    }

    @lombok.Getter
    public static class UpdatePriceRequest {
        @NotNull(message = "price는 필수입니다.")
        @Min(value = 1000, message = "price는 1000원 이상이어야 합니다.")
        private Integer price;
    }

    @lombok.Getter
    public static class UpsertSessionTypePricesRequest {
        @jakarta.validation.constraints.NotEmpty(message = "items는 비어있을 수 없습니다.")
        private List<@Valid UpsertSessionTypePriceItem> items;
    }

    @lombok.Getter
    public static class UpsertSessionTypePriceItem {
        @NotNull(message = "sessionTypeId는 필수입니다.")
        private Long sessionTypeId;
        @NotNull(message = "price는 필수입니다.")
        @Min(value = 1000, message = "price는 1000원 이상이어야 합니다.")
        private Integer price;
    }
}
