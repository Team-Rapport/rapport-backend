package com.rapport.domain.counselor.controller;

import com.rapport.domain.counselor.dto.CounselorProfileDto;
import com.rapport.domain.counselor.service.CounselorSearchService;
import com.rapport.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Counselor Search", description = "상담사 다중 필터 검색 API")
@RestController
@RequestMapping("/api/v1/counselors/search")
@RequiredArgsConstructor
public class CounselorSearchController {

    private final CounselorSearchService counselorSearchService;

    @Operation(
            summary = "상담사 다중 필터 검색",
            description = """
            각 필터 내 다중값 선택 시 OR 조건, 필터 간에는 AND 조건 적용
 
            specializations: 우울, 불안, 트라우마, 직장스트레스, 관계갈등, 가족문제
            sessionTypes: CHAT, CALL, VIDEOCALL, MEETING
            genders: MALE, FEMALE, ANY
            approaches: CBT, 정신분석, 인지행동치료, EMDR
            """
    )
    @GetMapping
    public ResponseEntity<ApiResponse<List<CounselorProfileDto.PublicProfileResponse>>> search(

            @Parameter(description = "전문분야 다중선택 (예: 우울,불안)")
            @RequestParam(required = false) List<String> specializations,

            @Parameter(description = "상담 방식 다중선택 (CHAT/CALL/VIDEOCALL/MEETING)")
            @RequestParam(required = false) List<String> sessionTypes,

            @Parameter(description = "상담사 성별 다중선택 (MALE/FEMALE/ANY)")
            @RequestParam(required = false) List<String> genders,

            @Parameter(description = "상담 접근법 다중선택 (예: CBT)")
            @RequestParam(required = false) List<String> approaches,

            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "페이지 크기", example = "12")
            @RequestParam(defaultValue = "12") int size
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                counselorSearchService.search(
                        specializations, sessionTypes, genders,
                        approaches, page, size)));
    }
}