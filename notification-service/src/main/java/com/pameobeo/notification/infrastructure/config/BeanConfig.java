package com.pameobeo.notification.infrastructure.config;

import com.pameobeo.notification.application.port.out.NotificationLog;
import com.pameobeo.notification.application.port.out.ProcessedEventStore;
import com.pameobeo.notification.application.service.NotificationService;
import com.pameobeo.notification.domain.model.NotificationPolicy;
import com.pameobeo.notification.infrastructure.mail.SmtpEmailSender;
import java.time.Clock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
class BeanConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    /** Java 21: one new virtual thread per task, no pool sizing needed. */
    @Bean(destroyMethod = "close")
    ExecutorService emailExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean
    NotificationService notificationService(JavaMailSender mailSender, NotificationLog log,
                                            ProcessedEventStore processedEvents, ExecutorService emailExecutor,
                                            @Value("${notification.from}") String from,
                                            @Value("${notification.shop-email}") String shopEmail, Clock clock) {
        return new NotificationService(new NotificationPolicy(shopEmail), new SmtpEmailSender(mailSender, from),
                log, processedEvents, emailExecutor, clock);
    }
}
