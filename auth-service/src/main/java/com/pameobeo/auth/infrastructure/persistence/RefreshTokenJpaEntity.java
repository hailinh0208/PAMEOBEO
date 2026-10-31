package com.pameobeo.auth.infrastructure.persistence;

import com.pameobeo.auth.domain.model.RefreshToken;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
public class RefreshTokenJpaEntity {

    @Id
    private UUID id;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;
    @Column(name = "session_started_at", nullable = false)
    private Instant sessionStartedAt;
    @Column(name = "idle_expires_at", nullable = false)
    private Instant idleExpiresAt;
    @Column(name = "absolute_expires_at", nullable = false)
    private Instant absoluteExpiresAt;
    @Column(nullable = false)
    private boolean revoked;

    protected RefreshTokenJpaEntity() {
    }

    static RefreshTokenJpaEntity fromDomain(RefreshToken token) {
        RefreshTokenJpaEntity e = new RefreshTokenJpaEntity();
        e.id = token.getId();
        e.userId = token.getUserId();
        e.tokenHash = token.getTokenHash();
        e.sessionStartedAt = token.getSessionStartedAt();
        e.idleExpiresAt = token.getIdleExpiresAt();
        e.absoluteExpiresAt = token.getAbsoluteExpiresAt();
        e.revoked = token.isRevoked();
        return e;
    }

    RefreshToken toDomain() {
        return new RefreshToken(id, userId, tokenHash, sessionStartedAt, idleExpiresAt, absoluteExpiresAt, revoked);
    }
}
