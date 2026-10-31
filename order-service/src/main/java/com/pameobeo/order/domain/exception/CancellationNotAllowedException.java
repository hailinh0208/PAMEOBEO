package com.pameobeo.order.domain.exception;

public class CancellationNotAllowedException extends DomainException {
    public CancellationNotAllowedException(String message) {
        super("CANCELLATION_NOT_ALLOWED", message);
    }
}
