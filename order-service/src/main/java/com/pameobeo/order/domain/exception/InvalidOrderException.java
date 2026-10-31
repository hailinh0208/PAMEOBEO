package com.pameobeo.order.domain.exception;

public class InvalidOrderException extends DomainException {
    public InvalidOrderException(String code, String message) {
        super(code, message);
    }
}
