package com.pameobeo.order.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Line of an order. Name and price are a snapshot taken when the order was placed (UC02 - BR6):
 * later price changes in the kitchen menu do not affect existing orders.
 */
public record OrderItem(UUID menuItemId, String name, Money unitPrice, int quantity) {

    public OrderItem {
        Objects.requireNonNull(menuItemId);
        Objects.requireNonNull(name);
        Objects.requireNonNull(unitPrice);
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
    }

    public Money subtotal() {
        return unitPrice.times(quantity);
    }
}
