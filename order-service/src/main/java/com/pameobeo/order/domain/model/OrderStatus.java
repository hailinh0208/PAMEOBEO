package com.pameobeo.order.domain.model;

import java.util.Map;
import java.util.Set;

/**
 * Order state machine.
 * <pre>
 * PENDING ──SlotReserved──► CONFIRMED ──18:00 cut-off──► COOKING ──► DELIVERING ──► COMPLETED
 *    │                         │
 *    ├──SlotFull──► CANCELLED ◄┘ (customer cancels before cut-off)
 *    └──customer cancels──► CANCELLED
 * </pre>
 */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    COOKING,
    DELIVERING,
    COMPLETED,
    CANCELLED;

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = Map.of(
            PENDING, Set.of(CONFIRMED, CANCELLED),
            CONFIRMED, Set.of(COOKING, CANCELLED),
            COOKING, Set.of(DELIVERING),
            DELIVERING, Set.of(COMPLETED),
            COMPLETED, Set.of(),
            CANCELLED, Set.of());

    public boolean canMoveTo(OrderStatus target) {
        return ALLOWED.get(this).contains(target);
    }
}
