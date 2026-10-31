package com.pameobeo.kitchen.infrastructure.persistence;

import com.pameobeo.kitchen.domain.model.SlotReservation;
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
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "slot_reservations")
public class SlotReservationJpaEntity {

    @Id
    @Column(name = "order_id")
    private UUID orderId;
    @Column(name = "cooking_date", nullable = false)
    private LocalDate cookingDate;
    @Column(nullable = false)
    private int quantity;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "slot_reservation_items", joinColumns = @JoinColumn(name = "order_id"))
    private List<LineEmbeddable> lines = new ArrayList<>();
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SlotReservation.Status status;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected SlotReservationJpaEntity() {
    }

    static SlotReservationJpaEntity fromDomain(SlotReservation r) {
        SlotReservationJpaEntity e = new SlotReservationJpaEntity();
        e.orderId = r.getOrderId();
        e.cookingDate = r.getCookingDate();
        e.quantity = r.getQuantity();
        e.lines = new ArrayList<>(r.getLines().stream().map(LineEmbeddable::fromDomain).toList());
        e.status = r.getStatus();
        e.createdAt = r.getCreatedAt();
        return e;
    }

    SlotReservation toDomain() {
        return new SlotReservation(orderId, cookingDate, quantity,
                lines.stream().map(LineEmbeddable::toDomain).toList(), status, createdAt);
    }

    @Embeddable
    static class LineEmbeddable {

        @Column(name = "menu_item_id", nullable = false)
        private UUID menuItemId;
        @Column(nullable = false)
        private int quantity;

        protected LineEmbeddable() {
        }

        static LineEmbeddable fromDomain(SlotReservation.Line line) {
            LineEmbeddable e = new LineEmbeddable();
            e.menuItemId = line.menuItemId();
            e.quantity = line.quantity();
            return e;
        }

        SlotReservation.Line toDomain() {
            return new SlotReservation.Line(menuItemId, quantity);
        }
    }
}
