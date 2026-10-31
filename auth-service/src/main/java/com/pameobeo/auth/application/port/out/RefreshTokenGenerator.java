package com.pameobeo.auth.application.port.out;

public interface RefreshTokenGenerator {

    /** A new random, opaque token value (what the client keeps). */
    String generate();

    /** One-way hash stored in the database, so a DB leak does not leak usable tokens. */
    String hash(String token);
}
