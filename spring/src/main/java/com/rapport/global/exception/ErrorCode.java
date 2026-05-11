package com.rapport.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    // Auth
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 Refresh Token입니다."),
    OAUTH2_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "소셜 로그인에 실패했습니다."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "비밀번호가 올바르지 않습니다."),
    ACCOUNT_DISABLED(HttpStatus.FORBIDDEN, "비활성화된 계정입니다."),
    ACCOUNT_ALREADY_DEACTIVATED(HttpStatus.CONFLICT, "이미 비활성화된 계정입니다."),
    ACCOUNT_ALREADY_ACTIVE(HttpStatus.CONFLICT, "이미 활성화된 계정입니다."),
    OAUTH_USER_NO_PASSWORD(HttpStatus.BAD_REQUEST, "소셜 로그인 계정은 비밀번호가 없습니다."),

    // Email Verification
    EMAIL_VERIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "이메일 인증 요청을 찾을 수 없습니다."),
    EMAIL_VERIFICATION_EXPIRED(HttpStatus.BAD_REQUEST, "인증 코드가 만료되었습니다."),
    EMAIL_VERIFICATION_INVALID(HttpStatus.BAD_REQUEST, "인증 코드가 올바르지 않습니다."),
    EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "이메일 인증이 완료되지 않았습니다."),

    // Counselor
    COUNSELOR_NOT_FOUND(HttpStatus.NOT_FOUND, "상담사를 찾을 수 없습니다."),
    COUNSELOR_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 상담사 프로필이 존재합니다."),
    COUNSELOR_NOT_APPROVED(HttpStatus.FORBIDDEN, "승인된 상담사만 이용 가능한 기능입니다."),
    COUNSELOR_PENDING(HttpStatus.FORBIDDEN, "심사 대기 중입니다."),
    COUNSELOR_REJECTED(HttpStatus.FORBIDDEN, "심사가 반려되었습니다. 재신청을 진행해주세요."),
    COUNSELOR_APPROVAL_ALREADY_PROCESSED(HttpStatus.CONFLICT, "이미 처리된 심사입니다."),

    // Booking
    BOOKING_NOT_FOUND(HttpStatus.NOT_FOUND, "예약을 찾을 수 없습니다."),
    SCHEDULE_NOT_AVAILABLE(HttpStatus.CONFLICT, "해당 시간대는 예약이 불가합니다."),

    // Review
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "리뷰를 찾을 수 없습니다."),
    REVIEW_ACCESS_DENIED(HttpStatus.FORBIDDEN, "본인의 리뷰만 수정·삭제할 수 있습니다."),
    REVIEW_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 해당 예약에 대한 리뷰가 존재합니다."),
    BOOKING_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "완료된 예약에만 리뷰를 작성할 수 있습니다."),

    // Schedule Management
    SCHEDULE_SETTINGS_ALREADY_EXISTS(HttpStatus.CONFLICT, "슬롯 설정이 이미 존재합니다. 최초 1회만 설정 가능합니다."),
    SCHEDULE_SETTINGS_NOT_FOUND(HttpStatus.NOT_FOUND, "슬롯 설정이 없습니다. 먼저 슬롯 단위를 설정해주세요."),
    SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 일정을 찾을 수 없습니다."),
    PAST_DATE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "과거 날짜에는 일정을 등록할 수 없습니다."),
    SCHEDULE_CANNOT_DELETE_ACTIVE_BOOKING(HttpStatus.CONFLICT, "PENDING 또는 ACCEPTED 예약이 있는 슬롯은 삭제할 수 없습니다."),
    SCHEDULE_CANNOT_CLOSE_ACTIVE_BOOKING(HttpStatus.CONFLICT, "PENDING 또는 ACCEPTED 예약이 있는 슬롯이 포함되어 해당 날짜를 닫을 수 없습니다."),

    // File
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다."),
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다.");
    private final HttpStatus status;
    private final String message;
}