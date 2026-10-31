package com.pameobeo.notification.application.port.out;

import java.util.UUID;

public interface ProcessedEventStore {

    boolean markProcessed(UUID eventId);
}
