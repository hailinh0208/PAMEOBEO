package com.pameobeo.order.interfaces.rest;

import com.pameobeo.order.domain.exception.AccessDeniedToOrderException;
import com.pameobeo.order.domain.exception.CancellationNotAllowedException;
import com.pameobeo.order.domain.exception.CutoffPassedException;
import com.pameobeo.order.domain.exception.DomainException;
import com.pameobeo.order.domain.exception.InvalidStatusTransitionException;
import com.pameobeo.order.domain.exception.KitchenUnavailableException;
import com.pameobeo.order.domain.exception.MenuItemUnavailableException;
import com.pameobeo.order.domain.exception.NotEnoughSlotsException;
import com.pameobeo.order.domain.exception.OrderNotFoundException;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.time.LocalDate;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class GlobalExceptionHandler {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record ErrorResponse(String code, String message, LocalDate suggestedCookingDate, Instant timestamp) {}

    @ExceptionHandler(DomainException.class)
    ResponseEntity<ErrorResponse> handleDomain(DomainException ex) {
        HttpStatus status = switch (ex) {
            case KitchenUnavailableException e -> HttpStatus.SERVICE_UNAVAILABLE;
            case NotEnoughSlotsException e -> HttpStatus.CONFLICT;
            case CutoffPassedException e -> HttpStatus.CONFLICT;
            case MenuItemUnavailableException e -> HttpStatus.CONFLICT;
            case CancellationNotAllowedException e -> HttpStatus.CONFLICT;
            case InvalidStatusTransitionException e -> HttpStatus.CONFLICT;
            case OrderNotFoundException e -> HttpStatus.NOT_FOUND;
            case AccessDeniedToOrderException e -> HttpStatus.FORBIDDEN;
            default -> HttpStatus.BAD_REQUEST;
        };
        LocalDate suggested = ex instanceof CutoffPassedException c ? c.getSuggestedCookingDate() : null;
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(status);
        if (ex instanceof KitchenUnavailableException) {
            builder.header("Retry-After", "10");
        }
        return builder.body(new ErrorResponse(ex.getCode(), ex.getMessage(), suggested, Instant.now()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + " " + f.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(new ErrorResponse("VALIDATION_FAILED", message, null, Instant.now()));
    }
}
