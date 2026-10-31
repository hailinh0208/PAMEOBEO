package com.pameobeo.notification.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pameobeo.notification.application.port.in.NotifyOrderStatusUseCase;
import com.pameobeo.notification.domain.model.OrderStatusChange;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
class OrderStatusChangedListener {

    static final String TOPIC = "order.status-changed";

    private final ObjectMapper objectMapper;
    private final NotifyOrderStatusUseCase notifyOrderStatus;

    OrderStatusChangedListener(ObjectMapper objectMapper, NotifyOrderStatusUseCase notifyOrderStatus) {
        this.objectMapper = objectMapper;
        this.notifyOrderStatus = notifyOrderStatus;
    }

    @KafkaListener(topics = TOPIC, groupId = "notification-service", concurrency = "3")
    void onMessage(String message) throws Exception {
        EventEnvelope envelope = objectMapper.readValue(message, EventEnvelope.class);
        if ("OrderStatusChanged".equals(envelope.eventType())) {
            notifyOrderStatus.notify(envelope.eventId(), objectMapper.treeToValue(envelope.payload(), OrderStatusChange.class));
        }
    }
}
