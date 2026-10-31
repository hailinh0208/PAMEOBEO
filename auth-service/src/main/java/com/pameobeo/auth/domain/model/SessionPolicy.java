package com.pameobeo.auth.domain.model;

import java.time.Duration;

/**
 * Token lifetimes (UC01 - BR5).
 *
 * @param accessTokenTtl  short-lived JWT, so a leaked token quickly becomes useless
 * @param idleTimeout     no refresh within this window ends the session
 * @param absoluteTimeout a session can never live longer than this, even when active
 */
public record SessionPolicy(Duration accessTokenTtl, Duration idleTimeout, Duration absoluteTimeout) {

    public SessionPolicy {
        if (idleTimeout.compareTo(absoluteTimeout) > 0) {
            throw new IllegalArgumentException("idleTimeout must not exceed absoluteTimeout");
        }
    }
}
