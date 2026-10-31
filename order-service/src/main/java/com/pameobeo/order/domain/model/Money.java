package com.pameobeo.order.domain.model;

/** Value object: amount in VND (no decimals). Immutable, compared by value. */
public record Money(long amountVnd) {

    public static final Money ZERO = new Money(0);

    public Money {
        if (amountVnd < 0) {
            throw new IllegalArgumentException("Money cannot be negative");
        }
    }

    public static Money vnd(long amount) {
        return new Money(amount);
    }

    public Money plus(Money other) {
        return new Money(amountVnd + other.amountVnd);
    }

    public Money times(int quantity) {
        return new Money(amountVnd * quantity);
    }
}
