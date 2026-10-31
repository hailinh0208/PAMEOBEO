package com.pameobeo.order.infrastructure.messaging;

public final class Topics {

    /** Published by order-service: OrderCreated, OrderCancelled. Key = orderId. */
    public static final String ORDER_EVENTS = "order.events";
    /** Published by kitchen-service: SlotReserved, SlotFull, SlotReleased. Key = orderId. */
    public static final String KITCHEN_EVENTS = "kitchen.events";
    /** Published by order-service for notification-service: OrderStatusChanged. Key = orderId. */
    public static final String ORDER_STATUS_CHANGED = "order.status-changed";

    private Topics() {
    }
}
