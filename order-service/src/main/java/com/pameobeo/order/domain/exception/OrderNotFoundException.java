package com.pameobeo.order.domain.exception;

import java.util.UUID;

public class OrderNotFoundException extends DomainException {
    public OrderNotFoundException(UUID id) {
        super("ORDER_NOT_FOUND", "Order " + id + " not found");
    }
}
