package com.pameobeo.auth.infrastructure.persistence;

import com.pameobeo.auth.domain.model.Role;
import com.pameobeo.auth.domain.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Persistence model. Kept separate from the domain {@link User} so the domain has no JPA dependency. */
@Entity
@Table(name = "users")
public class UserJpaEntity {

    @Id
    private UUID id;
    @Column(nullable = false, unique = true)
    private String email;
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    @Column(name = "full_name", nullable = false)
    private String fullName;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;
    @Column(name = "locked_until")
    private Instant lockedUntil;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected UserJpaEntity() {
    }

    static UserJpaEntity fromDomain(User user) {
        UserJpaEntity e = new UserJpaEntity();
        e.id = user.getId();
        e.email = user.getEmail();
        e.passwordHash = user.getPasswordHash();
        e.fullName = user.getFullName();
        e.role = user.getRole();
        e.failedLoginAttempts = user.getFailedLoginAttempts();
        e.lockedUntil = user.getLockedUntil();
        e.createdAt = user.getCreatedAt();
        return e;
    }

    User toDomain() {
        return new User(id, email, passwordHash, fullName, role, failedLoginAttempts, lockedUntil, createdAt);
    }
}
