package com.pameobeo.kitchen.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pameobeo.kitchen.application.port.in.ReleaseSlotUseCase;
import com.pameobeo.kitchen.application.port.in.ReserveSlotUseCase;
import com.pameobeo.kitchen.domain.model.SlotReservation;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Saga participant entry point: translates Kafka messages into use case calls. */
@Component
class OrderEventsListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventsListener.class);

    private final ObjectMapper objectMapper;
    private final ReserveSlotUseCase reserveSlot;
    private final ReleaseSlotUseCase releaseSlot;

    OrderEventsListener(ObjectMapper objectMapper, ReserveSlotUseCase reserveSlot, ReleaseSlotUseCase releaseSlot) {
        this.objectMapper = objectMapper;
        this.reserveSlot = reserveSlot;
        this.releaseSlot = releaseSlot;
    }

    @KafkaListener(topics = Topics.ORDER_EVENTS, groupId = "kitchen-service", concurrency = "3")
    void onMessage(String message) throws Exception {
        EventEnvelope envelope = objectMapper.readValue(message, EventEnvelope.class);
        JsonNode p = envelope.payload();
        switch (envelope.eventType()) {
            case "OrderCreated" -> reserveSlot.reserve(new ReserveSlotUseCase.ReserveSlotCommand(
                    envelope.eventId(),
                    UUID.fromString(p.get("orderId").asText()),
                    LocalDate.parse(p.get("cookingDate").asText()),
                    p.get("totalQuantity").asInt(),
                    Instant.parse(p.get("orderedAt").asText()),
                    lines(p.path("items"))));
            case "OrderCancelled" -> releaseSlot.release(envelope.eventId(), UUID.fromString(p.get("orderId").asText()));
            default -> log.debug("Ignoring event type {}", envelope.eventType());
        }
    }

    /** OrderCreated events written before menu items were added to the payload have no "items": empty list. */
    private static List<SlotReservation.Line> lines(JsonNode items) {
        List<SlotReservation.Line> lines = new ArrayList<>();
        items.forEach(i -> lines.add(new SlotReservation.Line(
                UUID.fromString(i.get("menuItemId").asText()), i.get("quantity").asInt())));
        return lines;
    }
}
