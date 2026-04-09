package com.rapport.domain.auth.service;

import com.rapport.domain.auth.entity.OAuthAccount;
import com.rapport.domain.auth.entity.OAuthAccountRepository;
import com.rapport.domain.user.entity.User;
import com.rapport.domain.user.entity.UserRepository;
import com.rapport.global.config.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final OAuthAccountRepository oAuthAccountRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuthUserInfo userInfo = OAuthUserInfoFactory.getOAuthUserInfo(
                registrationId, oAuth2User.getAttributes());

        OAuthAccount.Provider provider = OAuthAccount.Provider.valueOf(registrationId.toUpperCase());
        String providerId = userInfo.getProviderId();

        // 1. 기존 OAuth 계정 조회
        User user = oAuthAccountRepository
                .findByProviderAndProviderId(provider, providerId)
                .map(OAuthAccount::getUser)
                // 2. 없으면 이메일로 기존 User 조회 후 OAuth 계정 연결, 또는 신규 생성
                .orElseGet(() -> processNewOAuthUser(userInfo, provider, providerId));

        user.updateLastLoginAt();
        log.info("OAuth2 login success: userId={}, provider={}", user.getId(), provider);

        return new UserPrincipal(user, oAuth2User.getAttributes());
    }

    private User processNewOAuthUser(OAuthUserInfo userInfo,
                                     OAuthAccount.Provider provider,
                                     String providerId) {
        User user = userRepository.findByEmail(userInfo.getEmail())
                .orElseGet(() -> {
                    // 신규 유저 생성
                    User newUser = User.createOAuthUser(
                            userInfo.getEmail(),
                            userInfo.getName(),
                            userInfo.getProfileImageUrl(),
                            User.Role.CLIENT
                    );
                    return userRepository.save(newUser);
                });

        // OAuth 계정 연결
        OAuthAccount oauthAccount = OAuthAccount.of(user, provider, providerId);
        oAuthAccountRepository.save(oauthAccount);
        return user;
    }
}
