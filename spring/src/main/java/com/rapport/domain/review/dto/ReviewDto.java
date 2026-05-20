package com.rapport.domain.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class ReviewDto {

    // ===== 요청 =====

    @Getter
    public static class CreateRequest {
        @NotNull(message = "평점을 입력해주세요.")
        @Min(value = 1, message = "평점은 최소 1점입니다.")
        @Max(value = 5, message = "평점은 최대 5점입니다.")
        private Integer rating;

        private String content;
    }

    @Getter
    public static class UpdateRequest {
        @NotNull(message = "평점을 입력해주세요.")
        @Min(value = 1, message = "평점은 최소 1점입니다.")
        @Max(value = 5, message = "평점은 최대 5점입니다.")
        private Integer rating;

        private String content;
    }

    // ===== 응답 =====

    @Getter
    @Builder
    public static class ReviewResponse {
        private Long reviewId;
        private int rating;
        private String content;
        private String clientName;      // 앞 1글자 + ** (예: 홍**)
        private LocalDateTime createdAt;
    }
}
