package com.pameobeo.auth.domain.model;

import com.pameobeo.auth.domain.exception.AccountTemporarilyLockedException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * User aggregate. Owns the login-lockout rules so they cannot be bypassed by application code.
 */
public class User {

    private final UUID id;
    private final String email;
    private final String passwordHash;
    private final String fullName;
    private final Role role;
    private int failedLoginAttempts;
    private Instant lockedUntil;
    private final Instant createdAt;

    public User(UUID id, String email, String passwordHash, String fullName, Role role,
                int failedLoginAttempts, Instant lockedUntil, Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.email = Objects.requireNonNull(email);
        this.passwordHash = Objects.requireNonNull(passwordHash);
        this.fullName = Objects.requireNonNull(fullName);
        this.role = Objects.requireNonNull(role);
        this.failedLoginAttempts = failedLoginAttempts;
        this.lockedUntil = lockedUntil;
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    /** New accounts are always CUSTOMER (UC01 - BR3). */
    public static User registerCustomer(String email, String passwordHash, String fullName, Instant now) {
        return new User(UUID.randomUUID(), normalizeEmail(email), passwordHash, fullName, Role.CUSTOMER, 0, null, now);
    }

    public static User createAdmin(String email, String passwordHash, String fullName, Instant now) {
        return new User(UUID.randomUUID(), normalizeEmail(email), passwordHash, fullName, Role.ADMIN, 0, null, now);
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    public boolean isLocked(Instant now) {
        return lockedUntil != null && now.isBefore(lockedUntil);
    }

    /** Called before checking the password: a locked account is rejected even with the right password. */
    public void ensureNotLocked(Instant now) {
        if (isLocked(now)) {
            throw new AccountTemporarilyLockedException(lockedUntil);
        }
    }

    public void recordFailedLogin(Instant now, LoginPolicy policy) {
        failedLoginAttempts++;
        if (failedLoginAttempts >= policy.maxFailedAttempts()) {
            lockedUntil = now.plus(policy.lockDuration());
            failedLoginAttempts = 0;
        }
    }

    public void recordSuccessfulLogin() {
        failedLoginAttempts = 0;
        lockedUntil = null;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getFullName() { return fullName; }
    public Role getRole() { return role; }
    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public Instant getLockedUntil() { return lockedUntil; }
    public Instant getCreatedAt() { return createdAt; }
}
