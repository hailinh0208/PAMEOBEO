package com.pameobeo.order.infrastructure.config;

import com.pameobeo.order.application.port.in.OrderAdminUseCase;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * UC04: every day at 18:00 (Vietnam time) orders for tomorrow are closed and move to COOKING.
 * Safe to run twice (only CONFIRMED orders are picked). With several instances a distributed lock
 * (e.g. Redis/ShedLock) would be added - out of scope for the assignment.
 */
@Component
class CutoffJob {

    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");

    private final OrderAdminUseCase admin;
    private final Clock clock;

    CutoffJob(OrderAdminUseCase admin, Clock clock) {
        this.admin = admin;
        this.clock = clock;
    }

    @Scheduled(cron = "0 0 18 * * *", zone = "Asia/Ho_Chi_Minh")
    void closeOrdersForTomorrow() {
        admin.startCooking(LocalDate.now(clock.withZone(VIETNAM)).plusDays(1));
    }
}
