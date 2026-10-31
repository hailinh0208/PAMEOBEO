package com.pameobeo.kitchen.infrastructure.config;

import com.pameobeo.kitchen.application.port.out.CookingDayRepository;
import com.pameobeo.kitchen.application.port.out.KitchenEventPublisher;
import com.pameobeo.kitchen.application.port.out.MenuItemImageRepository;
import com.pameobeo.kitchen.application.port.out.MenuItemRepository;
import com.pameobeo.kitchen.application.port.out.ProcessedEventStore;
import com.pameobeo.kitchen.application.port.out.SlotReservationRepository;
import com.pameobeo.kitchen.application.service.KitchenApplicationService;
import com.pameobeo.kitchen.domain.model.CutoffPolicy;
import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class BeanConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    CutoffPolicy cutoffPolicy(KitchenProperties props) {
        return new CutoffPolicy(props.cutoffTime(), ZoneId.of(props.zone()));
    }

    @Bean
    KitchenApplicationService kitchenApplicationService(CookingDayRepository cookingDays,
                                                        SlotReservationRepository reservations,
                                                        MenuItemRepository menu,
                                                        MenuItemImageRepository images,
                                                        ProcessedEventStore processedEvents,
                                                        KitchenEventPublisher events,
                                                        CutoffPolicy cutoffPolicy,
                                                        KitchenProperties props,
                                                        Clock clock) {
        return new KitchenApplicationService(cookingDays, reservations, menu, images, processedEvents, events,
                cutoffPolicy, props.defaultCapacity(), clock);
    }
}
