package com.pameobeo.order.infrastructure.messaging;

import java.time.Clock;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Polling Publisher: moves unpublished outbox rows to Kafka. A row is marked published only after
 * Kafka acknowledged it, so an event can be sent twice (at-least-once) but never lost.
 */
@Component
class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxRepository outbox;
    private final KafkaTemplate<String, String> kafka;
    private final Clock clock;

    OutboxRelay(OutboxRepository outbox, KafkaTemplate<String, String> kafka, Clock clock) {
        this.outbox = outbox;
        this.kafka = kafka;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${outbox.relay.interval-ms:300}")
    @Transactional
    public void publishPending() {
        List<OutboxEventJpaEntity> batch = outbox.lockUnpublished(100);
        for (OutboxEventJpaEntity event : batch) {
            try {
                kafka.send(MessageBuilder.withPayload(event.getPayload())
                                .setHeader(KafkaHeaders.TOPIC, event.getTopic())
                                .setHeader(KafkaHeaders.KEY, event.getMessageKey())
                                .setHeader("eventType", event.getEventType())
                                .build())
                        .get(5, TimeUnit.SECONDS);
                event.markPublished(clock.instant());
            } catch (Exception e) {
                log.warn("Kafka unavailable, outbox event {} will be retried: {}", event.getId(), e.getMessage());
                return; // rows already marked in this batch are committed; the rest stays pending
            }
        }
        if (!batch.isEmpty()) {
            log.debug("Published {} outbox events", batch.size());
        }
    }
}
