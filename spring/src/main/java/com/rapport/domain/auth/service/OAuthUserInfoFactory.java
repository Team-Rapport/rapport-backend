package com.rapport.domain.auth.service;

import com.rapport.global.exception.BusinessException;
import com.rapport.global.exception.ErrorCode;

import java.util.Map;

public class OAuthUserInfoFactory {

    public static OAuthUserInfo getOAuthUserInfo(String registrationId,
                                                  Map<String, Object> attributes) {
        return switch (registrationId.toLowerCase()) {
            case "google" -> new GoogleOAuthUserInfo(attributes);
            case "kakao"  -> new KakaoOAuthUserInfo(attributes);
            default -> throw new BusinessException(ErrorCode.OAUTH2_LOGIN_FAILED,
                    "지원하지 않는 소셜 로그인 제공자입니다: " + registrationId);
        };
    }
}
