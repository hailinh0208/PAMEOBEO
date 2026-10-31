package com.pameobeo.notification.application.port.out;

import com.pameobeo.notification.domain.model.EmailMessage;

public interface EmailSender {

    void send(EmailMessage message);
}
