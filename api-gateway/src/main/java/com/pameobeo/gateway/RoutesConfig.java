package com.pameobeo.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Path prefix decides which service handles the request. The Authorization header is forwarded as-is. */
@Configuration
class RoutesConfig {

    @Bean
    RouteLocator routes(RouteLocatorBuilder builder,
                        @Value("${services.auth}") String auth,
                        @Value("${services.order}") String order,
                        @Value("${services.kitchen}") String kitchen,
                        @Value("${services.notification}") String notification) {
        return builder.routes()
                .route("auth", r -> r.path("/api/auth/**", "/.well-known/jwks.json").uri(auth))
                .route("order", r -> r.path("/api/orders/**").uri(order))
                .route("kitchen", r -> r.path("/api/kitchen/**").uri(kitchen))
                .route("notification", r -> r.path("/api/notifications/**").uri(notification))
                // admin dashboard: live state of order-service's circuit breaker (ADMIN only, see SecurityConfig)
                .route("circuit-breakers", r -> r.path("/api/system/circuit-breakers")
                        .filters(f -> f.setPath("/actuator/circuitbreakers"))
                        .uri(order))
                .build();
    }
}
