package com.pameobeo.auth.domain.exception;

public class InvalidRefreshTokenException extends DomainException {
    public InvalidRefreshTokenException() {
        super("INVALID_REFRESH_TOKEN", "Session expired, please log in again");
    }
}
