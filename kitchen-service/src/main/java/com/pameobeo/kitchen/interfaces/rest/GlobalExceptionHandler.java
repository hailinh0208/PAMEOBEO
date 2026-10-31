package com.pameobeo.kitchen.interfaces.rest;

import com.pameobeo.kitchen.domain.exception.CookingDayFullException;
import com.pameobeo.kitchen.domain.exception.DomainException;
import com.pameobeo.kitchen.domain.exception.ImageNotFoundException;
import com.pameobeo.kitchen.domain.exception.MenuItemNotFoundException;
import com.pameobeo.kitchen.domain.exception.TooManyImagesException;
import com.pameobeo.kitchen.domain.model.MenuItemImage;
import java.time.Instant;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
class GlobalExceptionHandler {

    record ErrorResponse(String code, String message, Instant timestamp) {}

    @ExceptionHandler(DomainException.class)
    ResponseEntity<ErrorResponse> handleDomain(DomainException ex) {
        HttpStatus status = switch (ex) {
            case MenuItemNotFoundException e -> HttpStatus.NOT_FOUND;
            case ImageNotFoundException e -> HttpStatus.NOT_FOUND;
            case CookingDayFullException e -> HttpStatus.CONFLICT;
            case TooManyImagesException e -> HttpStatus.CONFLICT;
            default -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
        return ResponseEntity.status(status).body(new ErrorResponse(ex.getCode(), ex.getMessage(), Instant.now()));
    }

    /** Above spring.servlet.multipart.max-file-size: rejected before reaching the domain check. */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ErrorResponse> handleTooLarge(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(new ErrorResponse("INVALID_IMAGE",
                "The image is larger than " + MenuItemImage.MAX_BYTES / (1024 * 1024) + " MB", Instant.now()));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    ResponseEntity<ErrorResponse> handleMissingFile(MissingServletRequestPartException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse("INVALID_IMAGE", "No file was sent", Instant.now()));
    }

    /** Two uploads at the same time for the 3rd photo: the database lets only one of them in. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorResponse> handleConflict(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("CONCURRENT_UPDATE",
                "Someone changed this at the same time, please reload and try again", Instant.now()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + " " + f.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(new ErrorResponse("VALIDATION_FAILED", message, Instant.now()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse("INVALID_REQUEST", ex.getMessage(), Instant.now()));
    }
}
