package com.pameobeo.kitchen.application.port.out;

import com.pameobeo.kitchen.domain.model.SlotReservation;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface SlotReservationRepository {

    Optional<SlotReservation> findByOrderId(UUID orderId);

    SlotReservation save(SlotReservation reservation);

    /** Reservations still holding jars on that cooking day (released ones excluded). */
    List<SlotReservation> findReservedOn(LocalDate cookingDate);

    /** Jars per menu item in reservations made since that instant and still held (released ones excluded). */
    Map<UUID, Integer> jarsSoldSince(Instant since);
}
