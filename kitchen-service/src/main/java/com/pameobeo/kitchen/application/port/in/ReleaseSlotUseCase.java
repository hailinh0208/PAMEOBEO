package com.pameobeo.kitchen.application.port.in;

import java.util.UUID;

/** Reacts to OrderCancelled by giving the jars back to the cooking day. */
public interface ReleaseSlotUseCase {

    void release(UUID eventId, UUID orderId);
}
