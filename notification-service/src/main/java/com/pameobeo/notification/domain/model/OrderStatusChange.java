package com.pameobeo.notification.domain.model;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** What notification-service needs to know about an order status change (from the OrderStatusChanged event). */
public record OrderStatusChange(UUID orderId, String customerEmail, String customerName, String previousStatus,
                                String status, String reason, LocalDate cookingDate, String deliveryWindow,
                                String deliveryAddress, long totalVnd, List<Line> items) {

    public record Line(String name, int quantity, long unitPriceVnd) {}
}
