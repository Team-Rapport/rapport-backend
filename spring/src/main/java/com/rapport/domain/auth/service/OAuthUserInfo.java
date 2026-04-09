package com.rapport.domain.auth.service;

import java.util.Map;

/**
 * 소셜 제공자별 사용자 정보 추출 인터페이스
 */
public abstract class OAuthUserInfo {

    protected Map<String, Object> attributes;

    public OAuthUserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public abstract String getProviderId();
    public abstract String getEmail();
    public abstract String getName();
    public abstract String getProfileImageUrl();
}

// ────────────────────────────────────────────────────────────
// Google
// ────────────────────────────────────────────────────────────
class GoogleOAuthUserInfo extends OAuthUserInfo {

    public GoogleOAuthUserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public String getProviderId() {
        return (String) attributes.get("sub");
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getName() {
        return (String) attributes.get("name");
    }

    @Override
    public String getProfileImageUrl() {
        return (String) attributes.get("picture");
    }
}

// ────────────────────────────────────────────────────────────
// Kakao
// ────────────────────────────────────────────────────────────
class KakaoOAuthUserInfo extends OAuthUserInfo {

    public KakaoOAuthUserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public String getProviderId() {
        return String.valueOf(attributes.get("id"));
    }

    @SuppressWarnings("unchecked")
    @Override
    public String getEmail() {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        if (kakaoAccount == null) return null;
        return (String) kakaoAccount.get("email");
    }

    @SuppressWarnings("unchecked")
    @Override
    public String getName() {
        Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");
        if (properties == null) return "카카오사용자";
        return (String) properties.get("nickname");
    }

    @SuppressWarnings("unchecked")
    @Override
    public String getProfileImageUrl() {
        Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");
        if (properties == null) return null;
        return (String) properties.get("profile_image");
    }
}
