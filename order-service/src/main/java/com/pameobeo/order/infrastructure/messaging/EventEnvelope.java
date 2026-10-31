package com.pameobeo.order.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

/**
 * Common wrapper for every message on Kafka. {@code eventId} is what consumers use for idempotency,
 * {@code eventType} tells them how to read {@code payload}.
 */
public record EventEnvelope(UUID eventId, String eventType, Instant occurredAt, JsonNode payload) {}
