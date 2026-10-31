package com.pameobeo.order.application.port.out;

import com.pameobeo.order.domain.model.Order;
import com.pameobeo.order.domain.model.OrderStatus;

/** Writes integration events to the outbox, in the caller's transaction. */
public interface OrderEventPublisher {

    void orderCreated(Order order);

    void orderCancelled(Order order);

    void statusChanged(Order order, OrderStatus previousStatus);
}
