package com.pameobeo.kitchen.domain.exception;

import java.time.LocalDate;

public class CookingDayFullException extends DomainException {

    private final int remaining;

    public CookingDayFullException(LocalDate date, int requested, int remaining) {
        super("SOLD_OUT", "Cooking day " + date + " has only " + remaining + " jars left, requested " + requested);
        this.remaining = remaining;
    }

    public int getRemaining() {
        return remaining;
    }
}
