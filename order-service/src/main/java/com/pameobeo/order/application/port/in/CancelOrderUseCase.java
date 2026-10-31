package com.pameobeo.order.application.port.in;

import com.pameobeo.order.domain.model.Order;
import java.util.UUID;

public interface CancelOrderUseCase {

    Order cancel(UUID orderId, UUID customerId);
}
