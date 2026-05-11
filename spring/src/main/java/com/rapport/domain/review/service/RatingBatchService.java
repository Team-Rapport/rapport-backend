package com.rapport.domain.review.service;

import com.rapport.domain.counselor.entity.CounselorProfileRepository;
import com.rapport.domain.review.entity.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RatingBatchService {

    private final ReviewRepository reviewRepository;
    private final CounselorProfileRepository counselorProfileRepository;

    /**
     * 매일 새벽 3시: 모든 상담사 average_rating / review_count 재계산
     * - 1단계: 전체 초기화 (삭제된 리뷰만 남은 상담사 처리)
     * - 2단계: 실제 리뷰 통계로 업데이트
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void updateAverageRatings() {
        log.info("[Batch] Average rating update started");

        counselorProfileRepository.resetAllRatings();

        List<Object[]> stats = reviewRepository.findRatingStatsByCounselor();
        for (Object[] row : stats) {
            Long counselorId = (Long) row[0];
            Double avg       = (Double) row[1];
            Long count       = (Long) row[2];

            BigDecimal avgRating = BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP);

            counselorProfileRepository.findByUserId(counselorId)
                    .ifPresent(profile -> profile.updateRating(avgRating, count.intValue()));
        }

        log.info("[Batch] Average rating update completed: {} counselors updated", stats.size());
    }
}
