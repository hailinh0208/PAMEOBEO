package com.pameobeo.order.application.port.in;

import java.util.UUID;

/** Saga: order-service reacts to the kitchen's answer. */
public interface KitchenReplyUseCase {

    void onSlotReserved(UUID eventId, UUID orderId);

    void onSlotFull(UUID eventId, UUID orderId, String reason);
}
