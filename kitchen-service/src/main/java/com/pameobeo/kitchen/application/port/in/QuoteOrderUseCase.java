package com.pameobeo.kitchen.application.port.in;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Synchronous check that order-service calls (REST, protected by a Circuit Breaker) before creating
 * an order: which cooking day, how many jars are left, current prices. Nothing is reserved here.
 */
public interface QuoteOrderUseCase {

    Quote quote(Instant orderedAt, List<UUID> menuItemIds);

    /** @param todayCutoffPassed true when ordering after 18:00, so the order goes to D+2 instead of D+1 */
    record Quote(LocalDate cookingDate, Instant orderCutoffAt, boolean todayCutoffPassed, int remainingSlots,
                 List<QuotedItem> items) {}

    record QuotedItem(UUID menuItemId, String name, long unitPriceVnd, boolean available) {}
}
