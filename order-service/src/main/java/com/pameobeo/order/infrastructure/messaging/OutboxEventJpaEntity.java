package com.pameobeo.order.infrastructure.messaging;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEventJpaEntity {

    @Id
    private UUID id;
    @Column(nullable = false)
    private String topic;
    @Column(name = "message_key", nullable = false)
    private String messageKey;
    @Column(name = "event_type", nullable = false)
    private String eventType;
    @Column(nullable = false, columnDefinition = "text")
    private String payload;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "published_at")
    private Instant publishedAt;

    protected OutboxEventJpaEntity() {
    }

    OutboxEventJpaEntity(UUID id, String topic, String messageKey, String eventType, String payload, Instant createdAt) {
        this.id = id;
        this.topic = topic;
        this.messageKey = messageKey;
        this.eventType = eventType;
        this.payload = payload;
        this.createdAt = createdAt;
    }

    void markPublished(Instant at) {
        this.publishedAt = at;
    }

    UUID getId() { return id; }
    String getTopic() { return topic; }
    String getMessageKey() { return messageKey; }
    String getEventType() { return eventType; }
    String getPayload() { return payload; }
}
