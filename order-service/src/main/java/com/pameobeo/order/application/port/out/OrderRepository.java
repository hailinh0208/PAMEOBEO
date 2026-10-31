package com.pameobeo.order.application.port.out;

import com.pameobeo.order.domain.model.Order;
import com.pameobeo.order.domain.model.OrderStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(UUID id);

    Optional<Order> findByIdempotencyKey(UUID customerId, String idempotencyKey);

    List<Order> findByCustomer(UUID customerId);

    List<Order> findByCookingDateAndStatus(LocalDate cookingDate, OrderStatus status);

    List<Order> findAll(LocalDate cookingDateOrNull);

    /** Thrown when two requests with the same Idempotency-Key race each other. */
    class DuplicateOrderRequestException extends RuntimeException {
        public DuplicateOrderRequestException(Throwable cause) {
            super(cause);
        }
    }
}
