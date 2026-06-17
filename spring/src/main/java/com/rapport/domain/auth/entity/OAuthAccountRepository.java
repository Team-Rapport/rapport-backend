package com.rapport.domain.auth.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OAuthAccountRepository extends JpaRepository<OAuthAccount, Long> {
    Optional<OAuthAccount> findByProviderAndProviderId(OAuthAccount.Provider provider, String providerId);
    boolean existsByProviderAndProviderId(OAuthAccount.Provider provider, String providerId);
}
