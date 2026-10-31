package com.pameobeo.auth.application.port.out;

import com.pameobeo.auth.domain.model.User;
import java.time.Instant;

public interface AccessTokenIssuer {

    IssuedToken issue(User user, Instant now);

    record IssuedToken(String value, Instant expiresAt) {}
}
