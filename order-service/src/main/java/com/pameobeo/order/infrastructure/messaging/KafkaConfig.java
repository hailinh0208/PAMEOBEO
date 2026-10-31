package com.pameobeo.order.infrastructure.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration
class KafkaConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConfig.class);

    @Bean
    NewTopic kitchenEventsTopic() {
        return TopicBuilder.name(Topics.KITCHEN_EVENTS).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic orderStatusChangedTopic() {
        return TopicBuilder.name(Topics.ORDER_STATUS_CHANGED).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic orderEventsTopic() {
        return TopicBuilder.name(Topics.ORDER_EVENTS).partitions(3).replicas(1).build();
    }

    /**
     * Retry with backoff (e.g. optimistic lock conflict when the customer cancels while the kitchen replies),
     * then park the message in {@code <topic>.DLT} so one poison message does not block the partition.
     */
    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> template) {
        ExponentialBackOff backOff = new ExponentialBackOff(100, 2.0);
        backOff.setMaxElapsedTime(10_000);
        DefaultErrorHandler handler = new DefaultErrorHandler(new DeadLetterPublishingRecoverer(template), backOff);
        handler.setRetryListeners((record, ex, attempt) ->
                log.warn("Retry {} for message key={} : {}", attempt, record.key(), ex.getMessage()));
        return handler;
    }
}
