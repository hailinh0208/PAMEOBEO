package com.pameobeo.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.pameobeo.notification.domain.model.EmailMessage;
import com.pameobeo.notification.domain.model.NotificationPolicy;
import com.pameobeo.notification.domain.model.OrderStatusChange;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationPolicyTest {

    private final NotificationPolicy policy = new NotificationPolicy("shop@pameobeo.vn");

    private static OrderStatusChange change(String status, String reason) {
        return new OrderStatusChange(UUID.randomUUID(), "linh@test.vn", "Linh", "PENDING", status, reason,
                LocalDate.of(2026, 10, 29), "18:00-22:00", "1 Nguyen Hue", 90_000,
                List.of(new OrderStatusChange.Line("Pate gà bí đỏ", 2, 45_000)));
    }

    @Test
    void confirmedOrderEmailsCustomerAndShop() {
        List<EmailMessage> messages = policy.messagesFor(change("CONFIRMED", null));

        assertThat(messages).extracting(EmailMessage::to).containsExactly("linh@test.vn", "shop@pameobeo.vn");
        assertThat(messages.get(0).body()).contains("18:00-22:00").contains("2026-10-29");
    }

    @Test
    void soldOutCancellationExplainsWhy() {
        List<EmailMessage> messages = policy.messagesFor(change("CANCELLED", "SOLD_OUT"));

        assertThat(messages.get(0).body()).contains("ngày nấu đã đủ suất");
    }

    @Test
    void cookingStatusSendsNoEmail() {
        assertThat(policy.messagesFor(change("COOKING", null))).isEmpty();
    }
}
