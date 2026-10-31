package com.pameobeo.auth.infrastructure.persistence;

import com.pameobeo.auth.application.port.out.RefreshTokenRepository;
import com.pameobeo.auth.domain.model.RefreshToken;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final SpringDataRefreshTokenRepository jpa;

    RefreshTokenRepositoryAdapter(SpringDataRefreshTokenRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash).map(RefreshTokenJpaEntity::toDomain);
    }

    @Override
    public RefreshToken save(RefreshToken token) {
        return jpa.save(RefreshTokenJpaEntity.fromDomain(token)).toDomain();
    }

    @Override
    public void revokeAllForUser(UUID userId) {
        jpa.revokeAllByUserId(userId);
    }
}
