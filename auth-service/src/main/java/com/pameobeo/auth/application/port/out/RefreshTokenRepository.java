package com.pameobeo.auth.application.port.out;

import com.pameobeo.auth.domain.model.RefreshToken;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    RefreshToken save(RefreshToken token);

    void revokeAllForUser(UUID userId);
}
