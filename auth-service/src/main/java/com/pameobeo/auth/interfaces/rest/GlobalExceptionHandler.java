package com.pameobeo.auth.interfaces.rest;

import com.pameobeo.auth.domain.exception.AccountTemporarilyLockedException;
import com.pameobeo.auth.domain.exception.DomainException;
import com.pameobeo.auth.domain.exception.EmailAlreadyExistsException;
import com.pameobeo.auth.domain.exception.InvalidCredentialsException;
import com.pameobeo.auth.domain.exception.InvalidRefreshTokenException;
import java.time.Instant;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class GlobalExceptionHandler {

    record ErrorResponse(String code, String message, Instant timestamp) {}

    @ExceptionHandler(DomainException.class)
    ResponseEntity<ErrorResponse> handleDomain(DomainException ex) {
        HttpStatus status = switch (ex) {
            case EmailAlreadyExistsException e -> HttpStatus.CONFLICT;
            case InvalidCredentialsException e -> HttpStatus.UNAUTHORIZED;
            case InvalidRefreshTokenException e -> HttpStatus.UNAUTHORIZED;
            case AccountTemporarilyLockedException e -> HttpStatus.LOCKED;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status).body(new ErrorResponse(ex.getCode(), ex.getMessage(), Instant.now()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + " " + f.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(new ErrorResponse("VALIDATION_FAILED", message, Instant.now()));
    }
}
