package com.pameobeo.kitchen.domain.exception;

public class InvalidImageException extends DomainException {
    public InvalidImageException(String message) {
        super("INVALID_IMAGE", message);
    }
}
