package com.pameobeo.order.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pameobeo.order.application.port.in.KitchenReplyUseCase;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
class KitchenEventsListener {

    private static final Logger log = LoggerFactory.getLogger(KitchenEventsListener.class);

    private final ObjectMapper objectMapper;
    private final KitchenReplyUseCase kitchenReply;

    KitchenEventsListener(ObjectMapper objectMapper, KitchenReplyUseCase kitchenReply) {
        this.objectMapper = objectMapper;
        this.kitchenReply = kitchenReply;
    }

    @KafkaListener(topics = Topics.KITCHEN_EVENTS, groupId = "order-service", concurrency = "3")
    void onMessage(String message) throws Exception {
        EventEnvelope envelope = objectMapper.readValue(message, EventEnvelope.class);
        JsonNode p = envelope.payload();
        UUID orderId = UUID.fromString(p.get("orderId").asText());
        switch (envelope.eventType()) {
            case "SlotReserved" -> kitchenReply.onSlotReserved(envelope.eventId(), orderId);
            case "SlotFull" -> kitchenReply.onSlotFull(envelope.eventId(), orderId, p.get("reason").asText());
            default -> log.debug("Ignoring event type {}", envelope.eventType());
        }
    }
}
