package com.pameobeo.auth.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A refresh token stored server-side (only its hash). Because JWT is stateless, this is where the
 * session idle timeout and absolute timeout are enforced (UC01 - Ex6, Ex7, BR6).
 */
public class RefreshToken {

    private final UUID id;
    private final UUID userId;
    private final String tokenHash;
    private final Instant sessionStartedAt;
    private final Instant idleExpiresAt;
    private final Instant absoluteExpiresAt;
    private boolean revoked;

    public RefreshToken(UUID id, UUID userId, String tokenHash, Instant sessionStartedAt,
                        Instant idleExpiresAt, Instant absoluteExpiresAt, boolean revoked) {
        this.id = Objects.requireNonNull(id);
        this.userId = Objects.requireNonNull(userId);
        this.tokenHash = Objects.requireNonNull(tokenHash);
        this.sessionStartedAt = Objects.requireNonNull(sessionStartedAt);
        this.idleExpiresAt = Objects.requireNonNull(idleExpiresAt);
        this.absoluteExpiresAt = Objects.requireNonNull(absoluteExpiresAt);
        this.revoked = revoked;
    }

    /** First token of a new session (at login). */
    public static RefreshToken startSession(UUID userId, String tokenHash, Instant now, SessionPolicy policy) {
        Instant absolute = now.plus(policy.absoluteTimeout());
        return new RefreshToken(UUID.randomUUID(), userId, tokenHash, now,
                min(now.plus(policy.idleTimeout()), absolute), absolute, false);
    }

    /**
     * Refresh token rotation: the current token is revoked and a new one is issued for the same session.
     * The idle window restarts, the absolute deadline does not move.
     */
    public RefreshToken rotate(String newTokenHash, Instant now, SessionPolicy policy) {
        this.revoked = true;
        return new RefreshToken(UUID.randomUUID(), userId, newTokenHash, sessionStartedAt,
                min(now.plus(policy.idleTimeout()), absoluteExpiresAt), absoluteExpiresAt, false);
    }

    public boolean isUsable(Instant now) {
        return !revoked && now.isBefore(idleExpiresAt) && now.isBefore(absoluteExpiresAt);
    }

    public void revoke() {
        this.revoked = true;
    }

    private static Instant min(Instant a, Instant b) {
        return a.isBefore(b) ? a : b;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getTokenHash() { return tokenHash; }
    public Instant getSessionStartedAt() { return sessionStartedAt; }
    public Instant getIdleExpiresAt() { return idleExpiresAt; }
    public Instant getAbsoluteExpiresAt() { return absoluteExpiresAt; }
    public boolean isRevoked() { return revoked; }
}
