package com.pameobeo.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.pameobeo.auth.domain.model.RefreshToken;
import com.pameobeo.auth.domain.model.SessionPolicy;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RefreshTokenTest {

    private static final SessionPolicy POLICY =
            new SessionPolicy(Duration.ofMinutes(15), Duration.ofMinutes(30), Duration.ofDays(7));
    private static final Instant LOGIN = Instant.parse("2026-10-28T03:00:00Z");

    @Test
    void sessionEndsAfter30MinutesIdle() {
        RefreshToken token = RefreshToken.startSession(UUID.randomUUID(), "h1", LOGIN, POLICY);

        assertThat(token.isUsable(LOGIN.plus(Duration.ofMinutes(29)))).isTrue();
        assertThat(token.isUsable(LOGIN.plus(Duration.ofMinutes(30)))).isFalse();
    }

    @Test
    void rotationRevokesOldTokenAndRestartsIdleWindow() {
        RefreshToken first = RefreshToken.startSession(UUID.randomUUID(), "h1", LOGIN, POLICY);
        Instant refreshAt = LOGIN.plus(Duration.ofMinutes(20));

        RefreshToken second = first.rotate("h2", refreshAt, POLICY);

        assertThat(first.isRevoked()).isTrue();
        assertThat(second.isUsable(refreshAt.plus(Duration.ofMinutes(29)))).isTrue();
        assertThat(second.getSessionStartedAt()).isEqualTo(LOGIN);
    }

    @Test
    void activeSessionStillEndsAfter7Days() {
        RefreshToken token = RefreshToken.startSession(UUID.randomUUID(), "h1", LOGIN, POLICY);
        Instant now = LOGIN;
        // refresh every 20 minutes for almost 7 days
        while (now.isBefore(LOGIN.plus(Duration.ofDays(7)).minus(Duration.ofMinutes(20)))) {
            now = now.plus(Duration.ofMinutes(20));
            token = token.rotate("h" + now, now, POLICY);
        }

        assertThat(token.isUsable(LOGIN.plus(Duration.ofDays(7)))).isFalse();
    }
}
