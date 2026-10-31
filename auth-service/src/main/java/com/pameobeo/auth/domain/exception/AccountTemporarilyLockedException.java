package com.pameobeo.auth.domain.exception;

import java.time.Instant;

public class AccountTemporarilyLockedException extends DomainException {

    private final Instant lockedUntil;

    public AccountTemporarilyLockedException(Instant lockedUntil) {
        super("ACCOUNT_TEMPORARILY_LOCKED", "Too many failed login attempts. Try again after " + lockedUntil);
        this.lockedUntil = lockedUntil;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }
}
