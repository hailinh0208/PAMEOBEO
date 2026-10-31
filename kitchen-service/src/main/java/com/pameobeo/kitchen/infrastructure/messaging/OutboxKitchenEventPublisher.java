package com.pameobeo.kitchen.infrastructure.messaging;

import com.pameobeo.kitchen.application.port.out.KitchenEventPublisher;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class OutboxKitchenEventPublisher implements KitchenEventPublisher {

    record SlotReserved(UUID orderId, LocalDate cookingDate, int quantity, int remainingSlots) {}

    record SlotFull(UUID orderId, LocalDate cookingDate, int requestedQuantity, int remainingSlots, String reason) {}

    record SlotReleased(UUID orderId, LocalDate cookingDate, int quantity, int remainingSlots) {}

    private final OutboxWriter outbox;

    OutboxKitchenEventPublisher(OutboxWriter outbox) {
        this.outbox = outbox;
    }

    @Override
    public void slotReserved(UUID orderId, LocalDate cookingDate, int quantity, int remainingSlots) {
        outbox.write(Topics.KITCHEN_EVENTS, orderId.toString(), "SlotReserved",
                new SlotReserved(orderId, cookingDate, quantity, remainingSlots));
    }

    @Override
    public void slotFull(UUID orderId, LocalDate cookingDate, int requestedQuantity, int remainingSlots, String reason) {
        outbox.write(Topics.KITCHEN_EVENTS, orderId.toString(), "SlotFull",
                new SlotFull(orderId, cookingDate, requestedQuantity, remainingSlots, reason));
    }

    @Override
    public void slotReleased(UUID orderId, LocalDate cookingDate, int quantity, int remainingSlots) {
        outbox.write(Topics.KITCHEN_EVENTS, orderId.toString(), "SlotReleased",
                new SlotReleased(orderId, cookingDate, quantity, remainingSlots));
    }
}
