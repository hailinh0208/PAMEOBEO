package com.pameobeo.notification.infrastructure.persistence;

import com.pameobeo.notification.application.port.out.NotificationLog;
import com.pameobeo.notification.domain.model.EmailMessage;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
class JdbcNotificationLog implements NotificationLog {

    private final JdbcTemplate jdbc;

    JdbcNotificationLog(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void record(EmailMessage m, String status, String thread, Instant at) {
        jdbc.update("""
                INSERT INTO notification_logs (id, order_id, recipient, to_address, subject, status, thread, sent_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), m.orderId(), m.recipient().name(), m.to(), m.subject(), status, thread,
                Timestamp.from(at));
    }

    @Override
    public List<Entry> recent(int limit) {
        return jdbc.query("""
                SELECT order_id, recipient, to_address, subject, status, thread, sent_at
                FROM notification_logs ORDER BY sent_at DESC LIMIT ?
                """, (rs, i) -> new Entry(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                rs.getString(5), rs.getString(6), rs.getTimestamp(7).toInstant()), limit);
    }
}
