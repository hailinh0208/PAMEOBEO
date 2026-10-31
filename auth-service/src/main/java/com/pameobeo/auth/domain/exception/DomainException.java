package com.pameobeo.auth.domain.exception;

/** Base class for business rule violations. {@code code} is returned to the client as-is. */
public abstract class DomainException extends RuntimeException {

    private final String code;

    protected DomainException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
