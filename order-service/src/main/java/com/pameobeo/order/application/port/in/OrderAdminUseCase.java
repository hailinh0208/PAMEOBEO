package com.pameobeo.order.application.port.in;

import com.pameobeo.order.domain.model.Order;
import com.pameobeo.order.domain.model.OrderStatus;
import java.time.LocalDate;
import java.util.UUID;

public interface OrderAdminUseCase {

    /** UC04: at the 18:00 cut-off, every CONFIRMED order of that cooking day moves to COOKING. */
    int startCooking(LocalDate cookingDate);

    /** UC04: admin moves an order to DELIVERING, then COMPLETED. */
    Order advance(UUID orderId, OrderStatus target);
}
