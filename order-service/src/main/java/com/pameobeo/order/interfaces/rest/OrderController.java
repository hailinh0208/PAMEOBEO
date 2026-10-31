package com.pameobeo.order.interfaces.rest;

import com.pameobeo.order.application.port.in.CancelOrderUseCase;
import com.pameobeo.order.application.port.in.OrderQuery;
import com.pameobeo.order.application.port.in.PlaceOrderUseCase;
import com.pameobeo.order.domain.model.Order;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
class OrderController {

    private final PlaceOrderUseCase placeOrder;
    private final CancelOrderUseCase cancelOrder;
    private final OrderQuery orderQuery;

    OrderController(PlaceOrderUseCase placeOrder, CancelOrderUseCase cancelOrder, OrderQuery orderQuery) {
        this.placeOrder = placeOrder;
        this.cancelOrder = cancelOrder;
        this.orderQuery = orderQuery;
    }

    /**
     * 202 Accepted, not 201: the order is PENDING until the kitchen confirms it asynchronously.
     * The client polls GET /api/orders/{id} (and gets an email) for the final status.
     */
    @PostMapping
    ResponseEntity<OrderResponse> place(@Valid @RequestBody PlaceOrderRequest request,
                                        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                        JwtAuthenticationToken token) {
        CurrentUser user = CurrentUser.from(token);
        Order order = placeOrder.place(new PlaceOrderUseCase.PlaceOrderCommand(
                user.id(), user.email(), user.name(),
                request.items().stream().map(i -> new PlaceOrderUseCase.Line(i.menuItemId(), i.quantity())).toList(),
                request.deliveryAddress(), idempotencyKey, request.confirmNextCookingDay()));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(OrderResponse.from(order));
    }

    @GetMapping
    List<OrderResponse> myOrders(JwtAuthenticationToken token) {
        return orderQuery.ordersOf(CurrentUser.from(token).id()).stream().map(OrderResponse::from).toList();
    }

    @GetMapping("/{id}")
    OrderResponse get(@PathVariable UUID id, JwtAuthenticationToken token) {
        CurrentUser user = CurrentUser.from(token);
        return OrderResponse.from(orderQuery.orderOf(id, user.id(), user.admin()));
    }

    @PostMapping("/{id}/cancel")
    OrderResponse cancel(@PathVariable UUID id, JwtAuthenticationToken token) {
        return OrderResponse.from(cancelOrder.cancel(id, CurrentUser.from(token).id()));
    }

    record PlaceOrderRequest(@NotEmpty List<@Valid Item> items,
                             @NotBlank String deliveryAddress,
                             boolean confirmNextCookingDay) {}

    record Item(@NotNull UUID menuItemId, @Min(1) int quantity) {}
}
