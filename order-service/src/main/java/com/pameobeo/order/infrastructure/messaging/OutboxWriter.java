package com.pameobeo.order.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Writes an event into the outbox table. Must join the caller's transaction. */
@Component
class OutboxWriter {

    private final OutboxRepository outbox;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    OutboxWriter(OutboxRepository outbox, ObjectMapper objectMapper, Clock clock) {
        this.outbox = outbox;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    void write(String topic, String key, String eventType, Object payload) {
        UUID eventId = UUID.randomUUID();
        EventEnvelope envelope = new EventEnvelope(eventId, eventType, clock.instant(), objectMapper.valueToTree(payload));
        try {
            outbox.save(new OutboxEventJpaEntity(eventId, topic, key, eventType,
                    objectMapper.writeValueAsString(envelope), clock.instant()));
        } catch (Exception e) {
            throw new IllegalStateException("Cannot serialize event " + eventType, e);
        }
    }
}
