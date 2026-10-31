package com.pameobeo.auth.domain.exception;

/** Deliberately does not say whether the email or the password was wrong (UC01 - Ex2). */
public class InvalidCredentialsException extends DomainException {
    public InvalidCredentialsException() {
        super("INVALID_CREDENTIALS", "Email or password is incorrect");
    }
}
