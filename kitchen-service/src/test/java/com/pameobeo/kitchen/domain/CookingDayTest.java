package com.pameobeo.kitchen.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pameobeo.kitchen.domain.exception.CookingDayFullException;
import com.pameobeo.kitchen.domain.exception.InvalidCapacityException;
import com.pameobeo.kitchen.domain.exception.OrderAfterCutoffException;
import com.pameobeo.kitchen.domain.model.CookingDay;
import com.pameobeo.kitchen.domain.model.CutoffPolicy;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CookingDayTest {

    private static final CutoffPolicy POLICY = CutoffPolicy.defaultPolicy();
    private static final LocalDate DAY = LocalDate.of(2026, 10, 29);
    private static final Instant BEFORE_CUTOFF = Instant.parse("2026-10-28T10:00:00Z"); // 17:00 VN
    private static final Instant AFTER_CUTOFF = Instant.parse("2026-10-28T11:00:01Z");  // 18:00:01 VN

    @Test
    void reservesJarsWithinCapacity() {
        CookingDay day = CookingDay.open(DAY, 50);

        day.reserve(3, BEFORE_CUTOFF, POLICY);

        assertThat(day.remaining()).isEqualTo(47);
    }

    @Test
    void neverAcceptsMoreThanCapacity() {
        CookingDay day = new CookingDay(DAY, 50, 47, 0L);

        assertThatThrownBy(() -> day.reserve(5, BEFORE_CUTOFF, POLICY))
                .isInstanceOf(CookingDayFullException.class);
        assertThat(day.remaining()).isEqualTo(3); // nothing partially reserved
    }

    @Test
    void lastJarsCanBeTakenExactly() {
        CookingDay day = new CookingDay(DAY, 50, 47, 0L);

        day.reserve(3, BEFORE_CUTOFF, POLICY);

        assertThat(day.remaining()).isZero();
    }

    @Test
    void rejectsOrderPlacedAfterCutoff() {
        CookingDay day = CookingDay.open(DAY, 50);

        assertThatThrownBy(() -> day.reserve(1, AFTER_CUTOFF, POLICY))
                .isInstanceOf(OrderAfterCutoffException.class);
    }

    @Test
    void releaseGivesJarsBack() {
        CookingDay day = new CookingDay(DAY, 50, 10, 0L);

        day.release(4);

        assertThat(day.remaining()).isEqualTo(44);
    }

    @Test
    void capacityCannotGoBelowReserved() {
        CookingDay day = new CookingDay(DAY, 50, 30, 0L);

        assertThatThrownBy(() -> day.changeCapacity(20)).isInstanceOf(InvalidCapacityException.class);
        day.changeCapacity(40);
        assertThat(day.remaining()).isEqualTo(10);
    }
}
