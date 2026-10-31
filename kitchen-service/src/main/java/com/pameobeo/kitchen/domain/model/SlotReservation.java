package com.pameobeo.kitchen.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Remembers how many jars an order holds, so a cancellation can give back exactly that amount, and which
 * menu items they are, so the kitchen knows what to buy for the cooking day.
 */
public class SlotReservation {

    public enum Status { RESERVED, RELEASED }

    public record Line(UUID menuItemId, int quantity) {}

    private final UUID orderId;
    private final LocalDate cookingDate;
    private final int quantity;
    private final List<Line> lines;
    private Status status;
    private final Instant createdAt;

    public SlotReservation(UUID orderId, LocalDate cookingDate, int quantity, List<Line> lines, Status status,
                           Instant createdAt) {
        this.orderId = Objects.requireNonNull(orderId);
        this.cookingDate = Objects.requireNonNull(cookingDate);
        this.quantity = quantity;
        this.lines = List.copyOf(lines);
        this.status = Objects.requireNonNull(status);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public static SlotReservation reserve(UUID orderId, LocalDate cookingDate, int quantity, List<Line> lines,
                                          Instant now) {
        return new SlotReservation(orderId, cookingDate, quantity, lines, Status.RESERVED, now);
    }

    /** @return true only the first time, so releasing twice never gives back jars twice */
    public boolean release() {
        if (status == Status.RELEASED) {
            return false;
        }
        status = Status.RELEASED;
        return true;
    }

    public UUID getOrderId() { return orderId; }
    public LocalDate getCookingDate() { return cookingDate; }
    public int getQuantity() { return quantity; }
    /** Empty for reservations made before menu items were sent along with OrderCreated. */
    public List<Line> getLines() { return lines; }
    public Status getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
