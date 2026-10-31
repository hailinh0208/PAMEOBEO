package com.pameobeo.order.infrastructure.messaging;

import com.pameobeo.order.application.port.out.ProcessedEventStore;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
class JdbcProcessedEventStore implements ProcessedEventStore {

    private final JdbcTemplate jdbc;

    JdbcProcessedEventStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The primary key makes this safe even when two consumers get the same event at the same time. */
    @Override
    public boolean markProcessed(UUID eventId) {
        int inserted = jdbc.update(
                "INSERT INTO processed_events (event_id, processed_at) VALUES (?, now()) ON CONFLICT DO NOTHING",
                eventId);
        return inserted == 1;
    }
}
