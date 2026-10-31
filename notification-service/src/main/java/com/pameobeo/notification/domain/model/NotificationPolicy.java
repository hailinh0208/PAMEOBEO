package com.pameobeo.notification.domain.model;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Decides who gets which email for each status (UC06).
 * CONFIRMED / CANCELLED: customer + a copy to the shop. DELIVERING / COMPLETED: customer only.
 * PENDING and COOKING: no email.
 */
public class NotificationPolicy {

    private final String shopEmail;

    public NotificationPolicy(String shopEmail) {
        this.shopEmail = shopEmail;
    }

    public List<EmailMessage> messagesFor(OrderStatusChange change) {
        List<EmailMessage> messages = new ArrayList<>();
        String subject = switch (change.status()) {
            case "CONFIRMED" -> "[Pameobeo] Đơn hàng đã được xác nhận";
            case "CANCELLED" -> "[Pameobeo] Đơn hàng đã bị hủy";
            case "DELIVERING" -> "[Pameobeo] Đơn hàng đang được giao";
            case "COMPLETED" -> "[Pameobeo] Cảm ơn bạn đã đặt pate cho bé mèo";
            default -> null;
        };
        if (subject == null) {
            return messages;
        }
        messages.add(new EmailMessage(change.orderId(), change.customerEmail(), subject,
                customerBody(change), EmailMessage.Recipient.CUSTOMER));
        if (change.status().equals("CONFIRMED") || change.status().equals("CANCELLED")) {
            messages.add(new EmailMessage(change.orderId(), shopEmail,
                    "[Shop] " + change.status() + " " + shortId(change),
                    shopBody(change), EmailMessage.Recipient.SHOP));
        }
        return messages;
    }

    private static String customerBody(OrderStatusChange c) {
        String greeting = "Chào " + (c.customerName() == null ? "bạn" : c.customerName()) + ",\n\n";
        String detail = items(c) + "\nTổng tiền: " + vnd(c.totalVnd()) + "\nĐịa chỉ giao: " + c.deliveryAddress() + "\n";
        return greeting + switch (c.status()) {
            case "CONFIRMED" -> "Đơn hàng " + shortId(c) + " đã được xác nhận.\n"
                    + "Pate sẽ được giao vào " + c.deliveryWindow() + " ngày " + c.cookingDate() + ".\n\n" + detail;
            case "CANCELLED" -> "Đơn hàng " + shortId(c) + " đã bị hủy. Lý do: " + reasonText(c.reason()) + ".\n\n" + detail;
            case "DELIVERING" -> "Shipper đang trên đường giao đơn " + shortId(c) + " tới bạn.\n\n" + detail;
            default -> "Đơn hàng " + shortId(c) + " đã giao thành công. Hẹn gặp lại!\n";
        } + "\nPameobeo - Pate tươi cho mèo";
    }

    private static String shopBody(OrderStatusChange c) {
        return "Order " + c.orderId() + "\nStatus: " + c.previousStatus() + " -> " + c.status()
                + (c.reason() == null ? "" : " (" + c.reason() + ")")
                + "\nCooking day: " + c.cookingDate() + "\nCustomer: " + c.customerEmail() + "\n" + items(c);
    }

    private static String items(OrderStatusChange c) {
        return c.items().stream()
                .map(i -> "- " + i.name() + " x" + i.quantity() + " (" + vnd(i.unitPriceVnd()) + ")")
                .collect(Collectors.joining("\n"));
    }

    static String reasonText(String reason) {
        if (reason == null) {
            return "không rõ";
        }
        return switch (reason) {
            case "SOLD_OUT" -> "ngày nấu đã đủ suất";
            case "CUTOFF_PASSED" -> "đã qua giờ chốt đơn";
            case "CANCELLED_BY_CUSTOMER" -> "bạn đã hủy đơn";
            default -> reason;
        };
    }

    private static String shortId(OrderStatusChange c) {
        return "#" + c.orderId().toString().substring(0, 8).toUpperCase();
    }

    private static String vnd(long amount) {
        return NumberFormat.getInstance(Locale.of("vi", "VN")).format(amount) + "đ";
    }
}
