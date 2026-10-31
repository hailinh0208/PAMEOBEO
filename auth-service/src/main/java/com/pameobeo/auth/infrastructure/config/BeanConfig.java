package com.pameobeo.auth.infrastructure.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.pameobeo.auth.application.port.out.PasswordHasher;
import com.pameobeo.auth.application.port.out.RefreshTokenRepository;
import com.pameobeo.auth.application.port.out.UserRepository;
import com.pameobeo.auth.application.service.AuthApplicationService;
import com.pameobeo.auth.domain.model.LoginPolicy;
import com.pameobeo.auth.domain.model.SessionPolicy;
import com.pameobeo.auth.infrastructure.security.BCryptPasswordHasher;
import com.pameobeo.auth.infrastructure.security.JwtAccessTokenIssuer;
import com.pameobeo.auth.infrastructure.security.RsaKeyProvider;
import com.pameobeo.auth.infrastructure.security.SecureRandomRefreshTokenGenerator;
import java.nio.file.Path;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/** Composition root: wires the framework-free application layer to its adapters. */
@Configuration
class BeanConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    RsaKeyProvider rsaKeyProvider(AuthProperties props) {
        return new RsaKeyProvider(Path.of(props.jwt().keyDir()), props.jwt().keyId());
    }

    @Bean
    JWKSet publicJwkSet(RsaKeyProvider keys) {
        return new JWKSet(keys.rsaKey().toPublicJWK());
    }

    @Bean
    JwtEncoder jwtEncoder(RsaKeyProvider keys) {
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(keys.rsaKey())));
    }

    /** auth-service validates its own tokens (for /me and /logout) with the public key. */
    @Bean
    JwtDecoder jwtDecoder(RsaKeyProvider keys) {
        return NimbusJwtDecoder.withPublicKey(keys.publicKey()).build();
    }

    @Bean
    PasswordHasher passwordHasher() {
        return new BCryptPasswordHasher();
    }

    @Bean
    AuthApplicationService authApplicationService(UserRepository users, RefreshTokenRepository refreshTokens,
                                                  PasswordHasher passwordHasher, JwtEncoder jwtEncoder,
                                                  AuthProperties props, Clock clock) {
        AuthProperties.Session s = props.session();
        return new AuthApplicationService(
                users,
                refreshTokens,
                passwordHasher,
                new JwtAccessTokenIssuer(jwtEncoder, props.jwt().issuer(), s.accessTokenTtl()),
                new SecureRandomRefreshTokenGenerator(),
                new LoginPolicy(props.login().maxFailedAttempts(), props.login().lockDuration()),
                new SessionPolicy(s.accessTokenTtl(), s.idleTimeout(), s.absoluteTimeout()),
                clock);
    }
}
