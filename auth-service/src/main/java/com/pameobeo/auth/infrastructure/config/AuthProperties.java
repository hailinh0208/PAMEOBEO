package com.pameobeo.auth.infrastructure.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** All numbers from UC01 live in application.yml so they can be shortened for the demo. */
@ConfigurationProperties(prefix = "auth")
public record AuthProperties(Jwt jwt, Session session, Login login, Admin admin) {

    public record Jwt(String issuer, String keyId, String keyDir) {}

    public record Session(Duration accessTokenTtl, Duration idleTimeout, Duration absoluteTimeout) {}

    public record Login(int maxFailedAttempts, Duration lockDuration) {}

    public record Admin(String email, String password, String fullName) {}
}
