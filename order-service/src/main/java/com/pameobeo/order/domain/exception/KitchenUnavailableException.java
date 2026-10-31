package com.pameobeo.order.domain.exception;

/** Kitchen is slow or down, or the Circuit Breaker is OPEN (UC02 - Ex5). */
public class KitchenUnavailableException extends DomainException {
    public KitchenUnavailableException(String detail) {
        super("SERVICE_BUSY", "The system is busy, please try again in a few minutes (" + detail + ")");
    }
}
