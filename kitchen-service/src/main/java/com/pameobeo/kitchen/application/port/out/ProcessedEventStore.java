package com.pameobeo.kitchen.application.port.out;

import java.util.UUID;

/** Idempotent Consumer: remembers which events were already handled. */
public interface ProcessedEventStore {

    /**
     * Records the event inside the current transaction.
     *
     * @return false if the event was already processed (a duplicate delivery)
     */
    boolean markProcessed(UUID eventId);
}
