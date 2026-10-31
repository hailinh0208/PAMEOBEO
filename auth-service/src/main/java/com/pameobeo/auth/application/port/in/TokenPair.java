package com.pameobeo.auth.application.port.in;

import java.time.Instant;

public record TokenPair(String accessToken, Instant accessTokenExpiresAt,
                        String refreshToken, Instant refreshTokenExpiresAt) {}
