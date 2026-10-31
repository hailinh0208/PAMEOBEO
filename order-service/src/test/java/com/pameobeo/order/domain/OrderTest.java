package com.pameobeo.order.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pameobeo.order.domain.exception.CancellationNotAllowedException;
import com.pameobeo.order.domain.exception.InvalidOrderException;
import com.pameobeo.order.domain.exception.InvalidStatusTransitionException;
import com.pameobeo.order.domain.model.Money;
import com.pameobeo.order.domain.model.Order;
import com.pameobeo.order.domain.model.OrderItem;
import com.pameobeo.order.domain.model.OrderStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrderTest {

    private static final Instant NOW = Instant.parse("2026-10-28T03:00:00Z");
    private static final Instant CUTOFF = Instant.parse("2026-10-28T11:00:00Z");

    private static Order newOrder(int jars) {
        return Order.place(UUID.randomUUID(), "a@b.c", "A", LocalDate.of(2026, 10, 29), CUTOFF,
                List.of(new OrderItem(UUID.randomUUID(), "Pate gà", Money.vnd(45_000), jars)),
                "1 Nguyen Hue", "key-1", NOW);
    }

    @Test
    void newOrderIsPendingWithTotal() {
        Order order = newOrder(3);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.total()).isEqualTo(Money.vnd(135_000));
    }

    @Test
    void atMostTenJarsPerOrder() {
        assertThatThrownBy(() -> newOrder(11)).isInstanceOf(InvalidOrderException.class);
    }

    @Test
    void slotReservedConfirmsOnlyOnce() {
        Order order = newOrder(2);

        assertThat(order.confirm(NOW)).isTrue();
        assertThat(order.confirm(NOW)).isFalse(); // duplicate event, no change
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void slotFullCancelsPendingOrder() {
        Order order = newOrder(2);

        order.rejectBecauseNoSlot("SOLD_OUT", NOW);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getStatusReason()).isEqualTo("SOLD_OUT");
    }

    @Test
    void lateSlotReservedDoesNotReviveCancelledOrder() {
        Order order = newOrder(2);
        order.cancelByCustomer(NOW);

        assertThat(order.confirm(NOW)).isFalse();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void customerCannotCancelAfterCutoff() {
        Order order = newOrder(2);
        order.confirm(NOW);

        assertThatThrownBy(() -> order.cancelByCustomer(CUTOFF)).isInstanceOf(CancellationNotAllowedException.class);
    }

    @Test
    void cannotSkipStates() {
        Order order = newOrder(2);

        assertThatThrownBy(() -> order.advanceTo(OrderStatus.DELIVERING, NOW))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void fullHappyLifecycle() {
        Order order = newOrder(2);
        order.confirm(NOW);
        order.startCooking(NOW);
        order.advanceTo(OrderStatus.DELIVERING, NOW);
        order.advanceTo(OrderStatus.COMPLETED, NOW);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }
}
