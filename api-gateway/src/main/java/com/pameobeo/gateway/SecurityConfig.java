package com.pameobeo.gateway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

/**
 * First line of defense. The JWT signature is verified locally with the public key downloaded from
 * auth-service's JWKS endpoint (cached), so auth-service is not called on every request.
 * <ul>
 *   <li>no / invalid / expired token -> 401</li>
 *   <li>valid token but wrong role    -> 403</li>
 * </ul>
 * Non-API paths serve the Angular frontend from {@code classpath:/static} (built by {@code frontend/}).
 */
@Configuration
@EnableWebFluxSecurity
class SecurityConfig {

    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(ex -> ex
                        .pathMatchers("/api/auth/register", "/api/auth/login", "/api/auth/refresh",
                                "/api/auth/logout", "/.well-known/jwks.json").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/kitchen/menu", "/api/kitchen/menu/highlights",
                                "/api/kitchen/menu/*/images/*", "/api/kitchen/availability").permitAll()
                        .pathMatchers("/actuator/health/**").permitAll()
                        .pathMatchers("/api/*/admin/**", "/api/system/**").hasRole("ADMIN")
                        .pathMatchers("/api/**").authenticated()
                        // everything else is the Angular app (static files bundled in this gateway)
                        .anyExchange().permitAll())
                .oauth2ResourceServer(rs -> rs.jwt(jwt -> jwt.jwtAuthenticationConverter(rolesConverter())))
                .build();
    }

    private static Converter<Jwt, Mono<AbstractAuthenticationToken>> rolesConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return new ReactiveJwtAuthenticationConverterAdapter(converter);
    }
}
