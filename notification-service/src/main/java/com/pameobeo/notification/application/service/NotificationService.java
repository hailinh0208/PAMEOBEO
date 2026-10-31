package com.pameobeo.notification.application.service;

import com.pameobeo.notification.application.port.in.NotifyOrderStatusUseCase;
import com.pameobeo.notification.application.port.out.EmailSender;
import com.pameobeo.notification.application.port.out.NotificationLog;
import com.pameobeo.notification.application.port.out.ProcessedEventStore;
import com.pameobeo.notification.domain.model.EmailMessage;
import com.pameobeo.notification.domain.model.NotificationPolicy;
import com.pameobeo.notification.domain.model.OrderStatusChange;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * Asynchronous processing with Java 21 virtual threads:
 * <ol>
 *   <li>the event arrives asynchronously through Kafka (nobody waits for this service);</li>
 *   <li>the emails of one event (customer + shop) are sent <b>in parallel</b>, each on its own
 *       virtual thread. Sending an email is blocking I/O; a virtual thread is parked during the wait
 *       instead of blocking an OS thread, so thousands can run cheaply.</li>
 * </ol>
 */
public class NotificationService implements NotifyOrderStatusUseCase {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationPolicy policy;
    private final EmailSender emailSender;
    private final NotificationLog notificationLog;
    private final ProcessedEventStore processedEvents;
    private final ExecutorService virtualThreads;
    private final Clock clock;

    public NotificationService(NotificationPolicy policy, EmailSender emailSender, NotificationLog notificationLog,
                               ProcessedEventStore processedEvents, ExecutorService virtualThreads, Clock clock) {
        this.policy = policy;
        this.emailSender = emailSender;
        this.notificationLog = notificationLog;
        this.processedEvents = processedEvents;
        this.virtualThreads = virtualThreads;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void notify(UUID eventId, OrderStatusChange change) {
        if (!processedEvents.markProcessed(eventId)) {
            log.info("Duplicate event {} ignored", eventId);
            return;
        }
        List<EmailMessage> messages = policy.messagesFor(change);
        log.info("Order {} is now {} -> {} email(s), received on {}",
                change.orderId(), change.status(), messages.size(), Thread.currentThread());

        List<CompletableFuture<String>> sending = messages.stream()
                .map(message -> CompletableFuture.supplyAsync(() -> send(message), virtualThreads))
                .toList();
        try {
            CompletableFuture.allOf(sending.toArray(CompletableFuture[]::new)).join();
        } catch (CompletionException e) {
            // rollback -> processed marker removed -> Kafka retries the event
            throw new IllegalStateException("Sending email failed for order " + change.orderId(), e.getCause());
        }
        for (int i = 0; i < messages.size(); i++) {
            notificationLog.record(messages.get(i), "SENT", sending.get(i).join(), clock.instant());
        }
    }

    private String send(EmailMessage message) {
        String thread = Thread.currentThread().toString();
        log.info("Sending '{}' to {} on {} (virtual={})",
                message.subject(), message.to(), thread, Thread.currentThread().isVirtual());
        emailSender.send(message);
        return thread;
    }
}
