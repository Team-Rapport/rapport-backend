package com.rapport.domain.review.service;

import com.rapport.domain.booking.entity.Booking;
import com.rapport.domain.booking.entity.BookingRepository;
import com.rapport.domain.review.dto.ReviewDto;
import com.rapport.domain.review.entity.Review;
import com.rapport.domain.review.entity.ReviewRepository;
import com.rapport.global.exception.BusinessException;
import com.rapport.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;

    // ===== 리뷰 작성 =====

    @Transactional
    public ReviewDto.ReviewResponse createReview(Long clientId, Long bookingId,
                                                  ReviewDto.CreateRequest req) {
        Booking booking = bookingRepository.findByIdAndClientId(bookingId, clientId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOKING_NOT_FOUND));

        if (booking.getStatus() != Booking.BookingStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.BOOKING_NOT_COMPLETED);
        }
        if (reviewRepository.countByBookingIdIncludingDeleted(bookingId) > 0) {
            throw new BusinessException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        Review review = Review.create(booking, booking.getClient(), booking.getCounselor(),
                req.getRating(), req.getContent());
        reviewRepository.save(review);

        log.info("Review created: reviewId={}, bookingId={}, clientId={}", review.getId(), bookingId, clientId);
        return toResponse(review);
    }

    // ===== 리뷰 수정 =====

    @Transactional
    public ReviewDto.ReviewResponse updateReview(Long clientId, Long reviewId,
                                                  ReviewDto.UpdateRequest req) {
        Review review = reviewRepository.findByIdAndClientId(reviewId, clientId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));

        review.update(req.getRating(), req.getContent());
        log.info("Review updated: reviewId={}, clientId={}", reviewId, clientId);
        return toResponse(review);
    }

    // ===== 리뷰 삭제 (Soft Delete) =====

    @Transactional
    public void deleteReview(Long clientId, Long reviewId) {
        Review review = reviewRepository.findByIdAndClientId(reviewId, clientId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));

        review.softDelete();
        log.info("Review deleted: reviewId={}, clientId={}", reviewId, clientId);
    }

    // ===== 상담사 리뷰 목록 (공개) =====

    @Transactional(readOnly = true)
    public Page<ReviewDto.ReviewResponse> getReviewsByCounselor(Long counselorId, Pageable pageable) {
        return reviewRepository.findByCounselorIdOrderByCreatedAtDesc(counselorId, pageable)
                .map(this::toResponse);
    }

    // ===== 내부 유틸 =====

    private ReviewDto.ReviewResponse toResponse(Review r) {
        return ReviewDto.ReviewResponse.builder()
                .reviewId(r.getId())
                .rating(r.getRating())
                .content(r.getContent())
                .clientName(maskName(r.getClient().getName()))
                .createdAt(r.getCreatedAt())
                .build();
    }

    private String maskName(String name) {
        if (name == null || name.isEmpty()) return "**";
        return name.charAt(0) + "**";
    }
}
