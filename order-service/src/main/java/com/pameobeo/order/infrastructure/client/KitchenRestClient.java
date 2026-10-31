package com.pameobeo.order.infrastructure.client;

import com.pameobeo.order.application.port.out.KitchenGateway;
import com.pameobeo.order.domain.exception.InvalidOrderException;
import com.pameobeo.order.domain.exception.KitchenUnavailableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * REST adapter towards kitchen-service, protected by a Resilience4j Circuit Breaker named {@code kitchen}.
 * <ul>
 *   <li>timeouts are set on the HTTP client itself (see {@code KitchenClientConfig}); Resilience4j's
 *       TimeLimiter only works with async return types, this call is synchronous;</li>
 *   <li>slow calls (&gt; 1s) and failures (5xx, timeouts, connection refused) count towards opening the breaker;</li>
 *   <li>4xx answers are business errors and are ignored by the breaker (configured in application.yml);</li>
 *   <li>no automatic retry on purpose: retrying a kitchen that is already overloaded multiplies its load.</li>
 * </ul>
 */
public class KitchenRestClient implements KitchenGateway {

    private static final Logger log = LoggerFactory.getLogger(KitchenRestClient.class);

    private final RestClient restClient;

    public KitchenRestClient(RestClient restClient) {
        this.restClient = restClient;
    }

    record QuoteRequest(Instant orderedAt, List<UUID> menuItemIds) {}

    @Override
    @CircuitBreaker(name = "kitchen", fallbackMethod = "quoteFallback")
    public KitchenQuote quote(Instant orderedAt, List<UUID> menuItemIds) {
        return restClient.post()
                .uri("/api/kitchen/quotes")
                .body(new QuoteRequest(orderedAt, menuItemIds))
                .retrieve()
                .body(KitchenQuote.class);
    }

    /**
     * Called for every exception, and immediately (without calling the kitchen) when the breaker is OPEN.
     * Public on purpose: Resilience4j invokes it by reflection and a private method can fail with
     * IllegalAccessException under load.
     */
    public KitchenQuote quoteFallback(Instant orderedAt, List<UUID> menuItemIds, Throwable t) {
        if (t instanceof HttpClientErrorException.NotFound) {
            throw new InvalidOrderException("MENU_ITEM_NOT_FOUND", "One of the menu items does not exist");
        }
        if (t instanceof HttpClientErrorException e) {
            throw new InvalidOrderException("KITCHEN_REJECTED_REQUEST", e.getStatusText());
        }
        if (t instanceof CallNotPermittedException) {
            log.debug("Circuit breaker OPEN, kitchen not called");
            throw new KitchenUnavailableException("circuit breaker open");
        }
        log.warn("Kitchen call failed: {}", t.toString());
        throw new KitchenUnavailableException(t.getClass().getSimpleName());
    }
}
