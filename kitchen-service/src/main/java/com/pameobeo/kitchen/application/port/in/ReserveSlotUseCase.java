package com.pameobeo.kitchen.application.port.in;

import com.pameobeo.kitchen.domain.model.SlotReservation;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Saga step: reacts to OrderCreated by reserving jars, then emits SlotReserved or SlotFull. */
public interface ReserveSlotUseCase {

    void reserve(ReserveSlotCommand command);

    /** @param lines menu items of the order, kept to plan the ingredients (empty for events sent before they existed) */
    record ReserveSlotCommand(UUID eventId, UUID orderId, LocalDate cookingDate, int quantity, Instant orderedAt,
                              List<SlotReservation.Line> lines) {}
}
