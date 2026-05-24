package com.rapport.domain.booking.controller;

import com.rapport.domain.booking.entity.CounselorSessionTypeRepository;
import com.rapport.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Counselor Session Type", description = "상담사 상담 방식별 가격 API")
@RestController
@RequestMapping("/api/v1/counselors/{counselorId}/session-types")
@RequiredArgsConstructor
public class CounselorSessionTypeController {

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
