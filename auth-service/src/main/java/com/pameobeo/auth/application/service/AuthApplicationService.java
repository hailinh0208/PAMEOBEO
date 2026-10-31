package com.pameobeo.auth.application.service;

import com.pameobeo.auth.application.port.in.LoginUseCase;
import com.pameobeo.auth.application.port.in.LogoutUseCase;
import com.pameobeo.auth.application.port.in.RefreshTokenUseCase;
import com.pameobeo.auth.application.port.in.RegisterUseCase;
import com.pameobeo.auth.application.port.in.TokenPair;
import com.pameobeo.auth.application.port.out.AccessTokenIssuer;
import com.pameobeo.auth.application.port.out.PasswordHasher;
import com.pameobeo.auth.application.port.out.RefreshTokenGenerator;
import com.pameobeo.auth.application.port.out.RefreshTokenRepository;
import com.pameobeo.auth.application.port.out.UserRepository;
import com.pameobeo.auth.domain.exception.AccountTemporarilyLockedException;
import com.pameobeo.auth.domain.exception.EmailAlreadyExistsException;
import com.pameobeo.auth.domain.exception.InvalidCredentialsException;
import com.pameobeo.auth.domain.exception.InvalidRefreshTokenException;
import com.pameobeo.auth.domain.model.LoginPolicy;
import com.pameobeo.auth.domain.model.RefreshToken;
import com.pameobeo.auth.domain.model.SessionPolicy;
import com.pameobeo.auth.domain.model.User;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use cases of UC01. Framework-free except for {@code @Transactional}; beans are wired in
 * {@code infrastructure.config.BeanConfig}.
 */
public class AuthApplicationService implements RegisterUseCase, LoginUseCase, RefreshTokenUseCase, LogoutUseCase {

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordHasher passwordHasher;
    private final AccessTokenIssuer accessTokenIssuer;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final LoginPolicy loginPolicy;
    private final SessionPolicy sessionPolicy;
    private final Clock clock;

    public AuthApplicationService(UserRepository users, RefreshTokenRepository refreshTokens,
                                  PasswordHasher passwordHasher, AccessTokenIssuer accessTokenIssuer,
                                  RefreshTokenGenerator refreshTokenGenerator, LoginPolicy loginPolicy,
                                  SessionPolicy sessionPolicy, Clock clock) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordHasher = passwordHasher;
        this.accessTokenIssuer = accessTokenIssuer;
        this.refreshTokenGenerator = refreshTokenGenerator;
        this.loginPolicy = loginPolicy;
        this.sessionPolicy = sessionPolicy;
        this.clock = clock;
    }

    @Override
    @Transactional
    public UUID register(RegisterCommand command) {
        String email = User.normalizeEmail(command.email());
        if (users.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }
        User user = User.registerCustomer(email, passwordHasher.hash(command.password()),
                command.fullName(), clock.instant());
        return users.save(user).getId();
    }

    /**
     * Failed attempts must be persisted even though we throw, hence {@code noRollbackFor}.
     */
    @Override
    @Transactional(noRollbackFor = {InvalidCredentialsException.class, AccountTemporarilyLockedException.class})
    public TokenPair login(LoginCommand command) {
        Instant now = clock.instant();
        User user = users.findByEmail(User.normalizeEmail(command.email()))
                .orElseThrow(InvalidCredentialsException::new);

        user.ensureNotLocked(now);

        if (!passwordHasher.matches(command.password(), user.getPasswordHash())) {
            user.recordFailedLogin(now, loginPolicy);
            users.save(user);
            if (user.isLocked(now)) {
                throw new AccountTemporarilyLockedException(user.getLockedUntil());
            }
            throw new InvalidCredentialsException();
        }

        user.recordSuccessfulLogin();
        users.save(user);

        String rawRefreshToken = refreshTokenGenerator.generate();
        RefreshToken refreshToken = RefreshToken.startSession(user.getId(),
                refreshTokenGenerator.hash(rawRefreshToken), now, sessionPolicy);
        refreshTokens.save(refreshToken);

        return issueTokens(user, rawRefreshToken, refreshToken, now);
    }

    @Override
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public TokenPair refresh(String rawRefreshToken) {
        Instant now = clock.instant();
        RefreshToken current = refreshTokens.findByTokenHash(refreshTokenGenerator.hash(rawRefreshToken))
                .orElseThrow(InvalidRefreshTokenException::new);

        if (current.isRevoked()) {
            // A rotated (old) token is being reused: it was probably stolen. Kill every session of this user.
            refreshTokens.revokeAllForUser(current.getUserId());
            throw new InvalidRefreshTokenException();
        }
        if (!current.isUsable(now)) {
            throw new InvalidRefreshTokenException();
        }

        User user = users.findById(current.getUserId()).orElseThrow(InvalidRefreshTokenException::new);

        String newRawToken = refreshTokenGenerator.generate();
        RefreshToken next = current.rotate(refreshTokenGenerator.hash(newRawToken), now, sessionPolicy);
        refreshTokens.save(current);
        refreshTokens.save(next);

        return issueTokens(user, newRawToken, next, now);
    }

    @Override
    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokens.findByTokenHash(refreshTokenGenerator.hash(rawRefreshToken)).ifPresent(token -> {
            token.revoke();
            refreshTokens.save(token);
        });
    }

    private TokenPair issueTokens(User user, String rawRefreshToken, RefreshToken refreshToken, Instant now) {
        AccessTokenIssuer.IssuedToken access = accessTokenIssuer.issue(user, now);
        return new TokenPair(access.value(), access.expiresAt(), rawRefreshToken, refreshToken.getIdleExpiresAt());
    }
}
