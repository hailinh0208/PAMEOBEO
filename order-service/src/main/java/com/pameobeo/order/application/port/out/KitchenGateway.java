package com.pameobeo.order.application.port.out;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Synchronous call to kitchen-service. The adapter protects it with a Circuit Breaker and throws
 * {@code KitchenUnavailableException} when the kitchen is slow, down, or the breaker is OPEN.
 */
public interface KitchenGateway {

    KitchenQuote quote(Instant orderedAt, List<UUID> menuItemIds);

    record KitchenQuote(LocalDate cookingDate, Instant orderCutoffAt, boolean todayCutoffPassed,
                        int remainingSlots, List<QuotedItem> items) {}

    record QuotedItem(UUID menuItemId, String name, long unitPriceVnd, boolean available) {}
}
