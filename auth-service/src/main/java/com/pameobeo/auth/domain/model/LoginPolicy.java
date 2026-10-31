package com.pameobeo.auth.domain.model;

import java.time.Duration;

/**
 * Brute-force protection rule (UC01 - BR4): after {@code maxFailedAttempts} consecutive wrong passwords
 * the account is locked for {@code lockDuration}.
 */
public record LoginPolicy(int maxFailedAttempts, Duration lockDuration) {

    public LoginPolicy {
        if (maxFailedAttempts < 1) {
            throw new IllegalArgumentException("maxFailedAttempts must be >= 1");
        }
        if (lockDuration == null || lockDuration.isNegative() || lockDuration.isZero()) {
            throw new IllegalArgumentException("lockDuration must be positive");
        }
    }
}
