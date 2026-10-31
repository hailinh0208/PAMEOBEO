package com.pameobeo.order.infrastructure.messaging;

import com.pameobeo.order.application.port.out.OrderEventPublisher;
import com.pameobeo.order.domain.model.Order;
import com.pameobeo.order.domain.model.OrderStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class OutboxOrderEventPublisher implements OrderEventPublisher {

    /** items: lets the kitchen plan ingredients per cooking day (it owns the recipes) */
    record OrderCreated(UUID orderId, UUID customerId, LocalDate cookingDate, int totalQuantity, Instant orderedAt,
                        List<CreatedLine> items) {}

    record CreatedLine(UUID menuItemId, int quantity) {}

    record OrderCancelled(UUID orderId, LocalDate cookingDate, String reason) {}

    record Line(String name, int quantity, long unitPriceVnd) {}

    record OrderStatusChanged(UUID orderId, UUID customerId, String customerEmail, String customerName,
                              String previousStatus, String status, String reason, LocalDate cookingDate,
                              String deliveryWindow, String deliveryAddress, long totalVnd, List<Line> items) {}

    private final OutboxWriter outbox;

    OutboxOrderEventPublisher(OutboxWriter outbox) {
        this.outbox = outbox;
    }

    @Override
    public void orderCreated(Order order) {
        outbox.write(Topics.ORDER_EVENTS, order.getId().toString(), "OrderCreated",
                new OrderCreated(order.getId(), order.getCustomerId(), order.getCookingDate(),
                        order.totalJars(), order.getCreatedAt(),
                        order.getItems().stream().map(i -> new CreatedLine(i.menuItemId(), i.quantity())).toList()));
    }

    @Override
    public void orderCancelled(Order order) {
        outbox.write(Topics.ORDER_EVENTS, order.getId().toString(), "OrderCancelled",
                new OrderCancelled(order.getId(), order.getCookingDate(), order.getStatusReason()));
    }

    @Override
    public void statusChanged(Order order, OrderStatus previousStatus) {
        List<Line> lines = order.getItems().stream()
                .map(i -> new Line(i.name(), i.quantity(), i.unitPrice().amountVnd()))
                .toList();
        outbox.write(Topics.ORDER_STATUS_CHANGED, order.getId().toString(), "OrderStatusChanged",
                new OrderStatusChanged(order.getId(), order.getCustomerId(), order.getCustomerEmail(),
                        order.getCustomerName(), previousStatus.name(), order.getStatus().name(),
                        order.getStatusReason(), order.getCookingDate(), "18:00-22:00",
                        order.getDeliveryAddress(), order.total().amountVnd(), lines));
    }
}
