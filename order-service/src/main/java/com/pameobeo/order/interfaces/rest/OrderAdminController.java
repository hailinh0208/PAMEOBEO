package com.pameobeo.order.interfaces.rest;

import com.pameobeo.order.application.port.in.OrderAdminUseCase;
import com.pameobeo.order.application.port.in.OrderQuery;
import com.pameobeo.order.domain.model.OrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders/admin")
class OrderAdminController {

    private final OrderQuery orderQuery;
    private final OrderAdminUseCase admin;

    OrderAdminController(OrderQuery orderQuery, OrderAdminUseCase admin) {
        this.orderQuery = orderQuery;
        this.admin = admin;
    }

    @GetMapping
    List<OrderResponse> all(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate cookingDate) {
        return orderQuery.allOrders(cookingDate).stream().map(OrderResponse::from).toList();
    }

    /** Same as the 18:00 job, triggered manually (useful for the demo). */
    @PostMapping("/cooking-days/{date}/start-cooking")
    Map<String, Object> startCooking(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Map.of("cookingDate", date, "ordersMovedToCooking", admin.startCooking(date));
    }

    @PatchMapping("/{id}/status")
    OrderResponse advance(@PathVariable UUID id, @Valid @RequestBody StatusRequest request) {
        return OrderResponse.from(admin.advance(id, request.status()));
    }

    record StatusRequest(@NotNull OrderStatus status) {}
}
