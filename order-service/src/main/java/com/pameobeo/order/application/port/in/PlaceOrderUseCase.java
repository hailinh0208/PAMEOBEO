package com.pameobeo.order.application.port.in;

import com.pameobeo.order.domain.model.Order;
import java.util.List;
import java.util.UUID;

public interface PlaceOrderUseCase {

    Order place(PlaceOrderCommand command);

    record PlaceOrderCommand(UUID customerId, String customerEmail, String customerName, List<Line> lines,
                             String deliveryAddress, String idempotencyKey, boolean confirmNextCookingDay) {}

    record Line(UUID menuItemId, int quantity) {}
}
