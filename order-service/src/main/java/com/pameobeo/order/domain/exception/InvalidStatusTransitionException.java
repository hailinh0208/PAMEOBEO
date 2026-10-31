package com.pameobeo.order.domain.exception;

import com.pameobeo.order.domain.model.OrderStatus;

public class InvalidStatusTransitionException extends DomainException {
    public InvalidStatusTransitionException(OrderStatus from, OrderStatus to) {
        super("INVALID_STATUS_TRANSITION", "Cannot move order from " + from + " to " + to);
    }
}
