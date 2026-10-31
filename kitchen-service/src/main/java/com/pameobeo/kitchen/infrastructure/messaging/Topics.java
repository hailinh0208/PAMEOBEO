package com.pameobeo.kitchen.infrastructure.messaging;

public final class Topics {

    /** Published by order-service: OrderCreated, OrderCancelled. Key = orderId. */
    public static final String ORDER_EVENTS = "order.events";
    /** Published by kitchen-service: SlotReserved, SlotFull, SlotReleased. Key = orderId. */
    public static final String KITCHEN_EVENTS = "kitchen.events";

    private Topics() {
    }
}
