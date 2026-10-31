package com.pameobeo.order.infrastructure.persistence;

import com.pameobeo.order.domain.model.Money;
import com.pameobeo.order.domain.model.Order;
import com.pameobeo.order.domain.model.OrderItem;
import com.pameobeo.order.domain.model.OrderStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class OrderJpaEntity {

    @Id
    private UUID id;
    @Column(name = "customer_id", nullable = false)
    private UUID customerId;
    @Column(name = "customer_email", nullable = false)
    private String customerEmail;
    @Column(name = "customer_name")
    private String customerName;
    @Column(name = "cooking_date", nullable = false)
    private LocalDate cookingDate;
    @Column(name = "cutoff_at", nullable = false)
    private Instant cutoffAt;
    @Column(name = "delivery_address", nullable = false)
    private String deliveryAddress;
    @Column(name = "idempotency_key")
    private String idempotencyKey;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;
    @Column(name = "status_reason")
    private String statusReason;
    @Column(name = "total_vnd", nullable = false)
    private long totalVnd;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version
    private Long version;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "order_items", joinColumns = @JoinColumn(name = "order_id"))
    @OrderColumn(name = "line_no")
    private List<Item> items = new ArrayList<>();

    @Embeddable
    public static class Item {
        @Column(name = "menu_item_id", nullable = false)
        UUID menuItemId;
        @Column(nullable = false)
        String name;
        @Column(name = "unit_price_vnd", nullable = false)
        long unitPriceVnd;
        @Column(nullable = false)
        int quantity;
    }

    protected OrderJpaEntity() {
    }

    static OrderJpaEntity fromDomain(Order o) {
        OrderJpaEntity e = new OrderJpaEntity();
        e.id = o.getId();
        e.customerId = o.getCustomerId();
        e.customerEmail = o.getCustomerEmail();
        e.customerName = o.getCustomerName();
        e.cookingDate = o.getCookingDate();
        e.cutoffAt = o.getCutoffAt();
        e.deliveryAddress = o.getDeliveryAddress();
        e.idempotencyKey = o.getIdempotencyKey();
        e.status = o.getStatus();
        e.statusReason = o.getStatusReason();
        e.totalVnd = o.total().amountVnd();
        e.createdAt = o.getCreatedAt();
        e.updatedAt = o.getUpdatedAt();
        e.version = o.getVersion();
        for (OrderItem i : o.getItems()) {
            Item item = new Item();
            item.menuItemId = i.menuItemId();
            item.name = i.name();
            item.unitPriceVnd = i.unitPrice().amountVnd();
            item.quantity = i.quantity();
            e.items.add(item);
        }
        return e;
    }

    Order toDomain() {
        List<OrderItem> domainItems = items.stream()
                .map(i -> new OrderItem(i.menuItemId, i.name, Money.vnd(i.unitPriceVnd), i.quantity))
                .toList();
        return new Order(id, customerId, customerEmail, customerName, cookingDate, cutoffAt, domainItems,
                deliveryAddress, idempotencyKey, createdAt, status, statusReason, updatedAt, version);
    }
}
