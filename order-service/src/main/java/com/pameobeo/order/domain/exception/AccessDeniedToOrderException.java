package com.pameobeo.order.domain.exception;

public class AccessDeniedToOrderException extends DomainException {
    public AccessDeniedToOrderException() {
        super("ORDER_ACCESS_DENIED", "This order belongs to another customer");
    }
}
