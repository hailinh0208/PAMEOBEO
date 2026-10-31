package com.pameobeo.notification.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.pameobeo.notification.application.port.out.NotificationLog;
import com.pameobeo.notification.application.service.NotificationService;
import com.pameobeo.notification.domain.model.EmailMessage;
import com.pameobeo.notification.domain.model.NotificationPolicy;
import com.pameobeo.notification.domain.model.OrderStatusChange;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

class NotificationServiceTest {

    @Test
    void sendsEmailsInParallelOnVirtualThreadsAndOnlyOncePerEvent() {
        List<Boolean> sentOnVirtualThread = new CopyOnWriteArrayList<>();
        List<String> logged = new CopyOnWriteArrayList<>();
        Set<UUID> processed = ConcurrentHashMap.newKeySet();

        NotificationService service = new NotificationService(
                new NotificationPolicy("shop@pameobeo.vn"),
                message -> sentOnVirtualThread.add(Thread.currentThread().isVirtual()),
                new NotificationLog() {
                    @Override public void record(EmailMessage m, String status, String thread, Instant at) { logged.add(m.to()); }
                    @Override public List<Entry> recent(int limit) { return List.of(); }
                },
                processed::add,
                Executors.newVirtualThreadPerTaskExecutor(),
                Clock.systemUTC());

        UUID eventId = UUID.randomUUID();
        OrderStatusChange change = new OrderStatusChange(UUID.randomUUID(), "linh@test.vn", "Linh", "PENDING",
                "CONFIRMED", null, LocalDate.of(2026, 10, 29), "18:00-22:00", "1 Nguyen Hue", 45_000,
                List.of(new OrderStatusChange.Line("Pate gà", 1, 45_000)));

        service.notify(eventId, change);
        service.notify(eventId, change); // duplicate delivery

        assertThat(sentOnVirtualThread).hasSize(2).containsOnly(true);
        assertThat(new HashSet<>(logged)).containsExactlyInAnyOrder("linh@test.vn", "shop@pameobeo.vn");
    }
}
