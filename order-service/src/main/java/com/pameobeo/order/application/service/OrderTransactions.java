package com.pameobeo.order.application.service;

import com.pameobeo.order.application.port.out.OrderEventPublisher;
import com.pameobeo.order.application.port.out.OrderRepository;
import com.pameobeo.order.domain.model.Order;
import org.springframework.transaction.annotation.Transactional;

/**
 * Short transactions used by {@link PlaceOrderService}. Kept in a separate bean on purpose:
 * <ul>
 *   <li>the REST call to the kitchen happens <b>outside</b> any DB transaction, so a slow kitchen
 *       never holds database connections;</li>
 *   <li>calling a {@code @Transactional} method on {@code this} would bypass the Spring proxy
 *       (self-invocation) and run without a transaction.</li>
 * </ul>
 */
public class OrderTransactions {

    private final OrderRepository orders;
    private final OrderEventPublisher events;

    public OrderTransactions(OrderRepository orders, OrderEventPublisher events) {
        this.orders = orders;
        this.events = events;
    }

    /** Order row + OrderCreated outbox row commit together, or not at all (Transactional Outbox). */
    @Transactional
    public Order createOrder(Order order) {
        Order saved = orders.save(order);
        events.orderCreated(saved);
        return saved;
    }
}
