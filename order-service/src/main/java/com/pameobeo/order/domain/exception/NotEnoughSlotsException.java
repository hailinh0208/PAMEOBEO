package com.pameobeo.order.domain.exception;

import java.time.LocalDate;

public class NotEnoughSlotsException extends DomainException {
    public NotEnoughSlotsException(LocalDate cookingDate, int requested, int remaining) {
        super("NOT_ENOUGH_SLOTS", remaining == 0
                ? "Cooking day " + cookingDate + " is sold out"
                : "Only " + remaining + " jars left for " + cookingDate + ", requested " + requested);
    }
}
