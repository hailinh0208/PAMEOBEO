package com.pameobeo.order.application.service;

import com.pameobeo.order.application.port.in.CancelOrderUseCase;
import com.pameobeo.order.application.port.in.OrderAdminUseCase;
import com.pameobeo.order.application.port.in.OrderQuery;
import com.pameobeo.order.application.port.out.OrderEventPublisher;
import com.pameobeo.order.application.port.out.OrderRepository;
import com.pameobeo.order.domain.exception.AccessDeniedToOrderException;
import com.pameobeo.order.domain.exception.InvalidOrderException;
import com.pameobeo.order.domain.exception.OrderNotFoundException;
import com.pameobeo.order.domain.model.Order;
import com.pameobeo.order.domain.model.OrderStatus;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

public class OrderManagementService implements CancelOrderUseCase, OrderQuery, OrderAdminUseCase {

    private static final Logger log = LoggerFactory.getLogger(OrderManagementService.class);

    private final OrderRepository orders;
    private final OrderEventPublisher events;
    private final Clock clock;

    public OrderManagementService(OrderRepository orders, OrderEventPublisher events, Clock clock) {
        this.orders = orders;
        this.events = events;
        this.clock = clock;
    }

    /** UC03. OrderCancelled tells the kitchen to give the jars back. */
    @Override
    @Transactional
    public Order cancel(UUID orderId, UUID customerId) {
        Order order = load(orderId);
        if (!order.belongsTo(customerId)) {
            throw new AccessDeniedToOrderException();
        }
        OrderStatus previous = order.getStatus();
        order.cancelByCustomer(clock.instant());
        Order saved = orders.save(order);
        events.orderCancelled(saved);
        events.statusChanged(saved, previous);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> ordersOf(UUID customerId) {
        return orders.findByCustomer(customerId);
    }

    @Override
    @Transactional(readOnly = true)
    public Order orderOf(UUID orderId, UUID requesterId, boolean requesterIsAdmin) {
        Order order = load(orderId);
        if (!requesterIsAdmin && !order.belongsTo(requesterId)) {
            throw new AccessDeniedToOrderException();
        }
        return order;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> allOrders(LocalDate cookingDate) {
        return orders.findAll(cookingDate);
    }

    @Override
    @Transactional
    public int startCooking(LocalDate cookingDate) {
        List<Order> confirmed = orders.findByCookingDateAndStatus(cookingDate, OrderStatus.CONFIRMED);
        for (Order order : confirmed) {
            order.startCooking(clock.instant());
            events.statusChanged(orders.save(order), OrderStatus.CONFIRMED);
        }
        log.info("Cut-off for cooking day {}: {} orders moved to COOKING", cookingDate, confirmed.size());
        return confirmed.size();
    }

    @Override
    @Transactional
    public Order advance(UUID orderId, OrderStatus target) {
        if (target != OrderStatus.DELIVERING && target != OrderStatus.COMPLETED) {
            throw new InvalidOrderException("INVALID_TARGET_STATUS", "Admin can only set DELIVERING or COMPLETED");
        }
        Order order = load(orderId);
        OrderStatus previous = order.getStatus();
        order.advanceTo(target, clock.instant());
        Order saved = orders.save(order);
        events.statusChanged(saved, previous);
        return saved;
    }

    private Order load(UUID orderId) {
        return orders.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
    }
}
