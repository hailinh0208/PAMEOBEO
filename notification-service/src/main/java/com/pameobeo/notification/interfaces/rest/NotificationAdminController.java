package com.pameobeo.notification.interfaces.rest;

import com.pameobeo.notification.application.port.out.NotificationLog;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Shows the last sent emails, including the virtual thread that sent each one (demo evidence). */
@RestController
@RequestMapping("/api/notifications/admin")
class NotificationAdminController {

    private final NotificationLog log;

    NotificationAdminController(NotificationLog log) {
        this.log = log;
    }

    @GetMapping("/recent")
    List<NotificationLog.Entry> recent(@RequestParam(defaultValue = "20") int limit) {
        return log.recent(Math.min(limit, 200));
    }
}
