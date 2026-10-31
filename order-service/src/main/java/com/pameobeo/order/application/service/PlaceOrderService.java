package com.pameobeo.order.application.service;

import com.pameobeo.order.application.port.in.PlaceOrderUseCase;
import com.pameobeo.order.application.port.out.KitchenGateway;
import com.pameobeo.order.application.port.out.OrderRepository;
import com.pameobeo.order.domain.exception.CutoffPassedException;
import com.pameobeo.order.domain.exception.InvalidOrderException;
import com.pameobeo.order.domain.exception.MenuItemUnavailableException;
import com.pameobeo.order.domain.exception.NotEnoughSlotsException;
import com.pameobeo.order.domain.model.Money;
import com.pameobeo.order.domain.model.Order;
import com.pameobeo.order.domain.model.OrderItem;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * UC02 step 1-5. The order is created as PENDING; the kitchen reserves jars asynchronously
 * (Saga, see {@link KitchenReplyService}).
 */
public class PlaceOrderService implements PlaceOrderUseCase {

    private final OrderRepository orders;
    private final KitchenGateway kitchen;
    private final OrderTransactions transactions;
    private final Clock clock;

    public PlaceOrderService(OrderRepository orders, KitchenGateway kitchen, OrderTransactions transactions, Clock clock) {
        this.orders = orders;
        this.kitchen = kitchen;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Override
    public Order place(PlaceOrderCommand cmd) {
        // Ex6: same Idempotency-Key -> return the order created by the first request
        Optional<Order> existing = findDuplicate(cmd);
        if (existing.isPresent()) {
            return existing.get();
        }

        Map<UUID, Integer> quantities = mergeLines(cmd.lines());
        int jars = quantities.values().stream().mapToInt(Integer::intValue).sum();
        if (jars > Order.MAX_JARS_PER_ORDER) {
            // checked before calling the kitchen, no need to load it for an invalid request
            throw new InvalidOrderException("TOO_MANY_JARS",
                    "An order can contain at most " + Order.MAX_JARS_PER_ORDER + " jars, got " + jars);
        }

        Instant now = clock.instant();
        KitchenGateway.KitchenQuote quote = kitchen.quote(now, List.copyOf(quantities.keySet())); // Circuit Breaker

        if (quote.todayCutoffPassed() && !cmd.confirmNextCookingDay()) {
            throw new CutoffPassedException(quote.cookingDate());                    // Ex2
        }
        if (jars > quote.remainingSlots()) {
            throw new NotEnoughSlotsException(quote.cookingDate(), jars, quote.remainingSlots()); // Ex1, Ex4
        }

        Map<UUID, KitchenGateway.QuotedItem> quoted = quote.items().stream()
                .collect(Collectors.toMap(KitchenGateway.QuotedItem::menuItemId, Function.identity()));
        List<OrderItem> items = quantities.entrySet().stream().map(e -> {
            KitchenGateway.QuotedItem q = quoted.get(e.getKey());
            if (q == null || !q.available()) {
                throw new MenuItemUnavailableException(q == null ? e.getKey().toString() : q.name()); // Ex8
            }
            return new OrderItem(q.menuItemId(), q.name(), Money.vnd(q.unitPriceVnd()), e.getValue()); // price snapshot
        }).toList();

        Order order = Order.place(cmd.customerId(), cmd.customerEmail(), cmd.customerName(), quote.cookingDate(),
                quote.orderCutoffAt(), items, cmd.deliveryAddress(), cmd.idempotencyKey(), now);
        try {
            return transactions.createOrder(order);
        } catch (OrderRepository.DuplicateOrderRequestException e) {
            return findDuplicate(cmd).orElseThrow(() -> e);
        }
    }

    private Optional<Order> findDuplicate(PlaceOrderCommand cmd) {
        if (cmd.idempotencyKey() == null || cmd.idempotencyKey().isBlank()) {
            return Optional.empty();
        }
        return orders.findByIdempotencyKey(cmd.customerId(), cmd.idempotencyKey());
    }

    private static Map<UUID, Integer> mergeLines(List<Line> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new InvalidOrderException("ORDER_EMPTY", "An order needs at least one item");
        }
        Map<UUID, Integer> merged = new LinkedHashMap<>();
        for (Line line : lines) {
            if (line.quantity() <= 0) {
                throw new InvalidOrderException("INVALID_QUANTITY", "Quantity must be at least 1");   // Ex7
            }
            merged.merge(line.menuItemId(), line.quantity(), Integer::sum);
        }
        return merged;
    }
}
