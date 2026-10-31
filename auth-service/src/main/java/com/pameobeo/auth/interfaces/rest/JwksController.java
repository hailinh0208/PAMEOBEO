package com.pameobeo.auth.interfaces.rest;

import com.nimbusds.jose.jwk.JWKSet;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Publishes the public key. Gateway and services download it once and verify every JWT locally,
 * without calling auth-service on each request.
 */
@RestController
class JwksController {

    private final JWKSet publicJwkSet;

    JwksController(JWKSet publicJwkSet) {
        this.publicJwkSet = publicJwkSet;
    }

    @GetMapping("/.well-known/jwks.json")
    Map<String, Object> jwks() {
        return publicJwkSet.toJSONObject();
    }
}
