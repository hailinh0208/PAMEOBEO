package com.pameobeo.order.interfaces.rest;

import com.pameobeo.order.domain.model.Order;
import com.pameobeo.order.domain.model.OrderStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

record OrderResponse(UUID id, OrderStatus status, String statusReason, String message, LocalDate cookingDate,
                     String deliveryWindow, Instant cancellableUntil, String deliveryAddress, long totalVnd,
                     List<Line> items, Instant createdAt, Instant updatedAt) {

    record Line(UUID menuItemId, String name, long unitPriceVnd, int quantity) {}

    static OrderResponse from(Order o) {
        return new OrderResponse(o.getId(), o.getStatus(), o.getStatusReason(), messageFor(o), o.getCookingDate(),
                "18:00-22:00 " + o.getCookingDate(), o.getCutoffAt(), o.getDeliveryAddress(), o.total().amountVnd(),
                o.getItems().stream().map(i -> new Line(i.menuItemId(), i.name(), i.unitPrice().amountVnd(), i.quantity())).toList(),
                o.getCreatedAt(), o.getUpdatedAt());
    }

    private static String messageFor(Order o) {
        return switch (o.getStatus()) {
            case PENDING -> "Đơn hàng đang được xử lý";
            case CONFIRMED -> "Đơn hàng đã được xác nhận";
            case COOKING -> "Bếp đang chuẩn bị đơn hàng";
            case DELIVERING -> "Đơn hàng đang được giao";
            case COMPLETED -> "Đơn hàng đã giao thành công";
            case CANCELLED -> "Đơn hàng đã bị hủy";
        };
    }
}
