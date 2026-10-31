package com.pameobeo.kitchen.application.port.in;

import java.time.Instant;
import java.time.LocalDate;

public interface AvailabilityQuery {

    /** Availability of the cooking day that an order placed right now would go to. */
    Availability nextCookingDay();

    Availability forDate(LocalDate cookingDate);

    record Availability(LocalDate cookingDate, Instant orderCutoffAt, int capacity, int reserved, int remaining) {}
}
