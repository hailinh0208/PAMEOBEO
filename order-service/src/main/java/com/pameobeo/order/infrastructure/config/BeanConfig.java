package com.pameobeo.order.infrastructure.config;

import com.pameobeo.order.application.port.out.KitchenGateway;
import com.pameobeo.order.application.port.out.OrderEventPublisher;
import com.pameobeo.order.application.port.out.OrderRepository;
import com.pameobeo.order.application.port.out.ProcessedEventStore;
import com.pameobeo.order.application.service.KitchenReplyService;
import com.pameobeo.order.application.service.OrderManagementService;
import com.pameobeo.order.application.service.OrderTransactions;
import com.pameobeo.order.application.service.PlaceOrderService;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Composition root of order-service. */
@Configuration
class BeanConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    OrderTransactions orderTransactions(OrderRepository orders, OrderEventPublisher events) {
        return new OrderTransactions(orders, events);
    }

    @Bean
    PlaceOrderService placeOrderService(OrderRepository orders, KitchenGateway kitchen,
                                        OrderTransactions transactions, Clock clock) {
        return new PlaceOrderService(orders, kitchen, transactions, clock);
    }

    @Bean
    KitchenReplyService kitchenReplyService(OrderRepository orders, OrderEventPublisher events,
                                            ProcessedEventStore processedEvents, Clock clock) {
        return new KitchenReplyService(orders, events, processedEvents, clock);
    }

    @Bean
    OrderManagementService orderManagementService(OrderRepository orders, OrderEventPublisher events, Clock clock) {
        return new OrderManagementService(orders, events, clock);
    }
}
