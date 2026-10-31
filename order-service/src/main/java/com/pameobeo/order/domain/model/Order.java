package com.pameobeo.order.domain.model;

import com.pameobeo.order.domain.exception.CancellationNotAllowedException;
import com.pameobeo.order.domain.exception.InvalidOrderException;
import com.pameobeo.order.domain.exception.InvalidStatusTransitionException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Order aggregate root. All state changes go through methods that enforce the state machine
 * ({@link OrderStatus}) and the business rules of UC02/UC03.
 */
public class Order {

    public static final int MAX_JARS_PER_ORDER = 10;

    private final UUID id;
    private final UUID customerId;
    private final String customerEmail;
    private final String customerName;
    private final LocalDate cookingDate;
    private final Instant cutoffAt;
    private final List<OrderItem> items;
    private final String deliveryAddress;
    private final String idempotencyKey;
    private final Instant createdAt;
    private OrderStatus status;
    private String statusReason;
    private Instant updatedAt;
    private final Long version;

    public Order(UUID id, UUID customerId, String customerEmail, String customerName, LocalDate cookingDate,
                 Instant cutoffAt, List<OrderItem> items, String deliveryAddress, String idempotencyKey,
                 Instant createdAt, OrderStatus status, String statusReason, Instant updatedAt, Long version) {
        this.id = Objects.requireNonNull(id);
        this.customerId = Objects.requireNonNull(customerId);
        this.customerEmail = Objects.requireNonNull(customerEmail);
        this.customerName = customerName;
        this.cookingDate = Objects.requireNonNull(cookingDate);
        this.cutoffAt = Objects.requireNonNull(cutoffAt);
        this.items = List.copyOf(items);
        this.deliveryAddress = Objects.requireNonNull(deliveryAddress);
        this.idempotencyKey = idempotencyKey;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.status = Objects.requireNonNull(status);
        this.statusReason = statusReason;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    /** Creates a new order in PENDING. The kitchen confirms it asynchronously (Saga). */
    public static Order place(UUID customerId, String customerEmail, String customerName, LocalDate cookingDate,
                              Instant cutoffAt, List<OrderItem> items, String deliveryAddress,
                              String idempotencyKey, Instant now) {
        if (items == null || items.isEmpty()) {
            throw new InvalidOrderException("ORDER_EMPTY", "An order needs at least one item");
        }
        int jars = items.stream().mapToInt(OrderItem::quantity).sum();
        if (jars > MAX_JARS_PER_ORDER) {
            throw new InvalidOrderException("TOO_MANY_JARS",
                    "An order can contain at most " + MAX_JARS_PER_ORDER + " jars, got " + jars);
        }
        if (deliveryAddress == null || deliveryAddress.isBlank()) {
            throw new InvalidOrderException("ADDRESS_REQUIRED", "Delivery address is required");
        }
        return new Order(UUID.randomUUID(), customerId, customerEmail, customerName, cookingDate, cutoffAt,
                items, deliveryAddress.trim(), idempotencyKey, now, OrderStatus.PENDING, null, now, null);
    }

    // ---- Saga outcomes ----

    /**
     * Kitchen reserved the jars. Ignored if the order is no longer PENDING (e.g. customer cancelled
     * meanwhile; the kitchen will receive OrderCancelled and release the jars).
     *
     * @return true if the status changed
     */
    public boolean confirm(Instant now) {
        if (status != OrderStatus.PENDING) {
            return false;
        }
        moveTo(OrderStatus.CONFIRMED, null, now);
        return true;
    }

    /** Compensation: kitchen could not reserve jars, so the order is cancelled. */
    public boolean rejectBecauseNoSlot(String reason, Instant now) {
        if (status != OrderStatus.PENDING) {
            return false;
        }
        moveTo(OrderStatus.CANCELLED, reason, now);
        return true;
    }

    // ---- Customer / admin actions ----

    /** UC03: customer may cancel only before the cut-off of the cooking day (UC02 - BR8). */
    public void cancelByCustomer(Instant now) {
        if (status != OrderStatus.PENDING && status != OrderStatus.CONFIRMED) {
            throw new CancellationNotAllowedException("Order in status " + status + " cannot be cancelled");
        }
        if (!now.isBefore(cutoffAt)) {
            throw new CancellationNotAllowedException("Cut-off has passed, the kitchen is already preparing this order");
        }
        moveTo(OrderStatus.CANCELLED, "CANCELLED_BY_CUSTOMER", now);
    }

    public void startCooking(Instant now) {
        moveTo(OrderStatus.COOKING, null, now);
    }

    public void advanceTo(OrderStatus target, Instant now) {
        moveTo(target, null, now);
    }

    private void moveTo(OrderStatus target, String reason, Instant now) {
        if (!status.canMoveTo(target)) {
            throw new InvalidStatusTransitionException(status, target);
        }
        this.status = target;
        this.statusReason = reason;
        this.updatedAt = now;
    }

    public int totalJars() {
        return items.stream().mapToInt(OrderItem::quantity).sum();
    }

    public Money total() {
        return items.stream().map(OrderItem::subtotal).reduce(Money.ZERO, Money::plus);
    }

    public boolean belongsTo(UUID userId) {
        return customerId.equals(userId);
    }

    public UUID getId() { return id; }
    public UUID getCustomerId() { return customerId; }
    public String getCustomerEmail() { return customerEmail; }
    public String getCustomerName() { return customerName; }
    public LocalDate getCookingDate() { return cookingDate; }
    public Instant getCutoffAt() { return cutoffAt; }
    public List<OrderItem> getItems() { return items; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Instant getCreatedAt() { return createdAt; }
    public OrderStatus getStatus() { return status; }
    public String getStatusReason() { return statusReason; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
}
