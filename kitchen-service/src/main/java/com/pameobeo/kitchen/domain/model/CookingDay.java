package com.pameobeo.kitchen.domain.model;

import com.pameobeo.kitchen.domain.exception.CookingDayFullException;
import com.pameobeo.kitchen.domain.exception.InvalidCapacityException;
import com.pameobeo.kitchen.domain.exception.OrderAfterCutoffException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Aggregate root guarding the main invariant of the shop: a cooking day never accepts more jars
 * than its capacity (UC02 - BR1). Concurrent reservations are serialized by optimistic locking
 * on {@code version} in the persistence layer.
 */
public class CookingDay {

    private final LocalDate date;
    private int capacity;
    private int reservedCount;
    private final Long version;

    public CookingDay(LocalDate date, int capacity, int reservedCount, Long version) {
        this.date = Objects.requireNonNull(date);
        if (capacity < 0 || reservedCount < 0 || reservedCount > capacity) {
            throw new IllegalArgumentException("Invalid cooking day state");
        }
        this.capacity = capacity;
        this.reservedCount = reservedCount;
        this.version = version;
    }

    public static CookingDay open(LocalDate date, int defaultCapacity) {
        return new CookingDay(date, defaultCapacity, 0, null);
    }

    public void reserve(int quantity, Instant orderedAt, CutoffPolicy cutoffPolicy) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        if (!cutoffPolicy.accepts(orderedAt, date)) {
            throw new OrderAfterCutoffException(date);
        }
        if (quantity > remaining()) {
            throw new CookingDayFullException(date, quantity, remaining());
        }
        reservedCount += quantity;
    }

    /** Gives jars back, e.g. when a customer cancels (UC03). Never goes below zero. */
    public void release(int quantity) {
        reservedCount = Math.max(0, reservedCount - quantity);
    }

    /** Admin can change capacity, but never below what is already reserved (UC05). */
    public void changeCapacity(int newCapacity) {
        if (newCapacity < reservedCount) {
            throw new InvalidCapacityException(newCapacity, reservedCount);
        }
        this.capacity = newCapacity;
    }

    public int remaining() {
        return capacity - reservedCount;
    }

    public LocalDate getDate() { return date; }
    public int getCapacity() { return capacity; }
    public int getReservedCount() { return reservedCount; }
    public Long getVersion() { return version; }
}
