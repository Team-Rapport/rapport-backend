package com.rapport.global;

import com.rapport.global.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 서버 상태 확인 컨트롤러
 * Docker 헬스체크 및 로드밸런서에서 사용됩니다.
 */
@RestController
@RequestMapping("/api")
public class HealthCheckController {

    @GetMapping("/health")
    public ApiResponse<Void> healthCheck() {
        return ApiResponse.success("서버가 정상 동작 중입니다.");
    }
}
