package com.pameobeo.order.application.port.in;

import com.pameobeo.order.domain.model.Order;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface OrderQuery {

    List<Order> ordersOf(UUID customerId);

    Order orderOf(UUID orderId, UUID requesterId, boolean requesterIsAdmin);

    List<Order> allOrders(LocalDate cookingDate);
}
