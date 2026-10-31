package com.pameobeo.order.application.port.out;

import java.util.UUID;

public interface ProcessedEventStore {

    /** @return false if this event was already handled (duplicate delivery) */
    boolean markProcessed(UUID eventId);
}
