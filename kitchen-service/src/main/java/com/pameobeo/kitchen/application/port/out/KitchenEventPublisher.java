package com.pameobeo.kitchen.application.port.out;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Outgoing integration events. The adapter writes them to the outbox table in the same transaction
 * as the business change (Transactional Outbox), a relay then sends them to Kafka.
 */
public interface KitchenEventPublisher {

    void slotReserved(UUID orderId, LocalDate cookingDate, int quantity, int remainingSlots);

    void slotFull(UUID orderId, LocalDate cookingDate, int requestedQuantity, int remainingSlots, String reason);

    void slotReleased(UUID orderId, LocalDate cookingDate, int quantity, int remainingSlots);
}
