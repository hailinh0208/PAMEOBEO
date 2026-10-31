package com.pameobeo.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pameobeo.order.application.port.in.PlaceOrderUseCase.Line;
import com.pameobeo.order.application.port.in.PlaceOrderUseCase.PlaceOrderCommand;
import com.pameobeo.order.application.port.out.KitchenGateway;
import com.pameobeo.order.application.port.out.OrderEventPublisher;
import com.pameobeo.order.application.port.out.OrderRepository;
import com.pameobeo.order.application.service.OrderTransactions;
import com.pameobeo.order.application.service.PlaceOrderService;
import com.pameobeo.order.domain.exception.CutoffPassedException;
import com.pameobeo.order.domain.exception.KitchenUnavailableException;
import com.pameobeo.order.domain.exception.MenuItemUnavailableException;
import com.pameobeo.order.domain.exception.NotEnoughSlotsException;
import com.pameobeo.order.domain.model.Order;
import com.pameobeo.order.domain.model.OrderStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Use case tests with hand-written fakes: no Spring, no database, no Kafka. */
class PlaceOrderServiceTest {

    private static final UUID CHICKEN = UUID.randomUUID();
    private static final UUID CUSTOMER = UUID.randomUUID();
    private static final LocalDate TOMORROW = LocalDate.of(2026, 10, 29);

    private final FakeOrders orders = new FakeOrders();
    private final FakeEvents events = new FakeEvents();
    private final FakeKitchen kitchen = new FakeKitchen();
    private final PlaceOrderService service = new PlaceOrderService(orders, kitchen,
            new OrderTransactions(orders, events), Clock.fixed(Instant.parse("2026-10-28T03:00:00Z"), ZoneOffset.UTC));

    private PlaceOrderCommand command(int jars, String key, boolean confirmNextDay) {
        return new PlaceOrderCommand(CUSTOMER, "a@b.c", "A", List.of(new Line(CHICKEN, jars)),
                "1 Nguyen Hue", key, confirmNextDay);
    }

    @Test
    void createsPendingOrderWithPriceSnapshotAndOrderCreatedEvent() {
        Order order = service.place(command(3, "k1", false));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getCookingDate()).isEqualTo(TOMORROW);
        assertThat(order.total().amountVnd()).isEqualTo(135_000);
        assertThat(events.created).containsExactly(order.getId());
    }

    @Test
    void sameIdempotencyKeyReturnsTheFirstOrder() {
        Order first = service.place(command(3, "k1", false));
        Order second = service.place(command(3, "k1", false));

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(events.created).hasSize(1);
        assertThat(kitchen.calls).isEqualTo(1); // the kitchen is not even called the second time
    }

    @Test
    void rejectsWhenFewerJarsLeftThanRequested() {
        kitchen.remaining = 3;

        assertThatThrownBy(() -> service.place(command(5, "k1", false))).isInstanceOf(NotEnoughSlotsException.class);
        assertThat(events.created).isEmpty();
    }

    @Test
    void afterCutoffAsksCustomerToConfirmNextCookingDay() {
        kitchen.todayCutoffPassed = true;

        assertThatThrownBy(() -> service.place(command(1, "k1", false))).isInstanceOf(CutoffPassedException.class);
        assertThat(service.place(command(1, "k2", true)).getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void rejectsInactiveMenuItem() {
        kitchen.available = false;

        assertThatThrownBy(() -> service.place(command(1, "k1", false))).isInstanceOf(MenuItemUnavailableException.class);
    }

    @Test
    void kitchenDownMeansNoOrderIsCreated() {
        kitchen.down = true;

        assertThatThrownBy(() -> service.place(command(1, "k1", false))).isInstanceOf(KitchenUnavailableException.class);
        assertThat(orders.byId).isEmpty();
    }

    // ------------------------------------------------------------------ fakes

    static class FakeKitchen implements KitchenGateway {
        int remaining = 50;
        boolean todayCutoffPassed;
        boolean available = true;
        boolean down;
        int calls;

        @Override
        public KitchenQuote quote(Instant orderedAt, List<UUID> menuItemIds) {
            calls++;
            if (down) {
                throw new KitchenUnavailableException("circuit breaker open");
            }
            return new KitchenQuote(todayCutoffPassed ? TOMORROW.plusDays(1) : TOMORROW,
                    Instant.parse("2026-10-28T11:00:00Z"), todayCutoffPassed, remaining,
                    List.of(new QuotedItem(CHICKEN, "Pate gà bí đỏ", 45_000, available)));
        }
    }

    static class FakeOrders implements OrderRepository {
        final Map<UUID, Order> byId = new HashMap<>();

        @Override public Order save(Order order) { byId.put(order.getId(), order); return order; }
        @Override public Optional<Order> findById(UUID id) { return Optional.ofNullable(byId.get(id)); }
        @Override public Optional<Order> findByIdempotencyKey(UUID customerId, String key) {
            return byId.values().stream()
                    .filter(o -> o.getCustomerId().equals(customerId) && key.equals(o.getIdempotencyKey()))
                    .findFirst();
        }
        @Override public List<Order> findByCustomer(UUID customerId) { return List.copyOf(byId.values()); }
        @Override public List<Order> findByCookingDateAndStatus(LocalDate d, OrderStatus s) { return List.of(); }
        @Override public List<Order> findAll(LocalDate d) { return List.copyOf(byId.values()); }
    }

    static class FakeEvents implements OrderEventPublisher {
        final List<UUID> created = new ArrayList<>();

        @Override public void orderCreated(Order order) { created.add(order.getId()); }
        @Override public void orderCancelled(Order order) { }
        @Override public void statusChanged(Order order, OrderStatus previousStatus) { }
    }
}
