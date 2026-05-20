package com.rapport.domain.review.controller;

import com.rapport.domain.review.dto.ReviewDto;
import com.rapport.domain.review.service.ReviewService;
import com.rapport.global.config.UserPrincipal;
import com.rapport.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Review", description = "리뷰 작성·수정·삭제·조회 API")
@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // ===== 리뷰 작성 (내담자 — 완료된 예약에 한해) =====

    @Operation(summary = "리뷰 작성",
               description = "booking.status = COMPLETED인 예약에만 작성 가능. 예약 1건당 1개.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/api/v1/bookings/{bookingId}/review")
    public ResponseEntity<ApiResponse<ReviewDto.ReviewResponse>> createReview(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long bookingId,
            @Valid @RequestBody ReviewDto.CreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("리뷰가 등록되었습니다.",
                reviewService.createReview(principal.getId(), bookingId, request)));
    }

    // ===== 리뷰 수정 =====

    @Operation(summary = "리뷰 수정", description = "본인 리뷰만 수정 가능.")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/api/v1/reviews/{reviewId}")
    public ResponseEntity<ApiResponse<ReviewDto.ReviewResponse>> updateReview(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewDto.UpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("리뷰가 수정되었습니다.",
                reviewService.updateReview(principal.getId(), reviewId, request)));
    }

    // ===== 리뷰 삭제 (Soft Delete) =====

    @Operation(summary = "리뷰 삭제", description = "본인 리뷰만 삭제 가능 (Soft Delete).")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/api/v1/reviews/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long reviewId) {
        reviewService.deleteReview(principal.getId(), reviewId);
        return ResponseEntity.ok(ApiResponse.ok("리뷰가 삭제되었습니다."));
    }

    // ===== 상담사 리뷰 목록 조회 (공개 — 토큰 불필요) =====

    @Operation(summary = "상담사 리뷰 목록 조회",
               description = "삭제된 리뷰 제외, 최신순. 작성자 이름 앞 1글자 + ** 익명 처리. 인증 불필요.")
    @GetMapping("/api/v1/counselors/{counselorId}/reviews")
    public ResponseEntity<ApiResponse<Page<ReviewDto.ReviewResponse>>> getReviews(
            @PathVariable Long counselorId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                reviewService.getReviewsByCounselor(counselorId, pageable)));
    }
}
