package com.pameobeo.notification.application.port.out;

import com.pameobeo.notification.domain.model.EmailMessage;
import java.time.Instant;
import java.util.List;

public interface NotificationLog {

    void record(EmailMessage message, String status, String thread, Instant at);

    List<Entry> recent(int limit);

    record Entry(String orderId, String recipient, String toAddress, String subject, String status,
                 String thread, Instant sentAt) {}
}
