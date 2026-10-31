package com.pameobeo.kitchen.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Order cut-off rule (UC02 - BR2, BR3, BR4).
 * <pre>
 *   ordered before 18:00 on day D  -> cooking day D+1 (delivered 18:00-22:00 on D+1)
 *   ordered at/after 18:00 on day D -> cooking day D+2
 * </pre>
 * The moment that counts is when the customer placed the order, not when the kitchen processes it.
 */
public record CutoffPolicy(LocalTime cutoffTime, ZoneId zone) {

    public static CutoffPolicy defaultPolicy() {
        return new CutoffPolicy(LocalTime.of(18, 0), ZoneId.of("Asia/Ho_Chi_Minh"));
    }

    public LocalDate cookingDateFor(Instant orderedAt) {
        ZonedDateTime local = orderedAt.atZone(zone);
        LocalDate today = local.toLocalDate();
        return local.toLocalTime().isBefore(cutoffTime) ? today.plusDays(1) : today.plusDays(2);
    }

    public boolean isAfterTodayCutoff(Instant orderedAt) {
        return !orderedAt.atZone(zone).toLocalTime().isBefore(cutoffTime);
    }

    /** Last moment an order can be placed for {@code cookingDate}: 18:00 the day before. */
    public Instant cutoffFor(LocalDate cookingDate) {
        return cookingDate.minusDays(1).atTime(cutoffTime).atZone(zone).toInstant();
    }

    public boolean accepts(Instant orderedAt, LocalDate cookingDate) {
        return orderedAt.isBefore(cutoffFor(cookingDate));
    }
}
