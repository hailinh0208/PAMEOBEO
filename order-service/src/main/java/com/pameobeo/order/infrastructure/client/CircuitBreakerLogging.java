package com.pameobeo.order.infrastructure.client;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Logs every state change of the kitchen breaker, e.g. "Circuit breaker 'kitchen': CLOSED -> OPEN". */
@Component
class CircuitBreakerLogging {

    private static final Logger log = LoggerFactory.getLogger(CircuitBreakerLogging.class);

    private final CircuitBreakerRegistry registry;

    CircuitBreakerLogging(CircuitBreakerRegistry registry) {
        this.registry = registry;
    }

    @PostConstruct
    void register() {
        registry.circuitBreaker("kitchen").getEventPublisher()
                .onStateTransition(e -> log.warn("Circuit breaker '{}': {} -> {}",
                        e.getCircuitBreakerName(),
                        e.getStateTransition().getFromState(),
                        e.getStateTransition().getToState()));
    }
}
