package com.pameobeo.order.application.service;

import com.pameobeo.order.application.port.in.KitchenReplyUseCase;
import com.pameobeo.order.application.port.out.OrderEventPublisher;
import com.pameobeo.order.application.port.out.OrderRepository;
import com.pameobeo.order.application.port.out.ProcessedEventStore;
import com.pameobeo.order.domain.model.Order;
import com.pameobeo.order.domain.model.OrderStatus;
import java.time.Clock;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * Last step of the Saga (choreography):
 * SlotReserved -> CONFIRMED, SlotFull -> CANCELLED (compensation). Then OrderStatusChanged is
 * published so notification-service can email the customer.
 */
public class KitchenReplyService implements KitchenReplyUseCase {

    private static final Logger log = LoggerFactory.getLogger(KitchenReplyService.class);

    private final OrderRepository orders;
    private final OrderEventPublisher events;
    private final ProcessedEventStore processedEvents;
    private final Clock clock;

    public KitchenReplyService(OrderRepository orders, OrderEventPublisher events,
                               ProcessedEventStore processedEvents, Clock clock) {
        this.orders = orders;
        this.events = events;
        this.processedEvents = processedEvents;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void onSlotReserved(UUID eventId, UUID orderId) {
        if (!processedEvents.markProcessed(eventId)) {
            return;
        }
        orders.findById(orderId).ifPresentOrElse(order -> {
            if (order.confirm(clock.instant())) {
                save(order, OrderStatus.PENDING);
                log.info("Order {} CONFIRMED", orderId);
            }
        }, () -> log.warn("SlotReserved for unknown order {}", orderId));
    }

    @Override
    @Transactional
    public void onSlotFull(UUID eventId, UUID orderId, String reason) {
        if (!processedEvents.markProcessed(eventId)) {
            return;
        }
        orders.findById(orderId).ifPresentOrElse(order -> {
            if (order.rejectBecauseNoSlot(reason, clock.instant())) {
                save(order, OrderStatus.PENDING);
                log.info("Order {} CANCELLED by saga compensation ({})", orderId, reason);
            }
        }, () -> log.warn("SlotFull for unknown order {}", orderId));
    }

    private void save(Order order, OrderStatus previous) {
        Order saved = orders.save(order);
        events.statusChanged(saved, previous);
    }
}
