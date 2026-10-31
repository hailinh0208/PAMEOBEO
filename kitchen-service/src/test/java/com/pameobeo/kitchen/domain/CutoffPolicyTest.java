package com.pameobeo.kitchen.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.pameobeo.kitchen.domain.model.CutoffPolicy;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

class CutoffPolicyTest {

    private final CutoffPolicy policy = CutoffPolicy.defaultPolicy();

    private static java.time.Instant vn(String localDateTime) {
        return ZonedDateTime.parse(localDateTime + "+07:00[Asia/Ho_Chi_Minh]").toInstant();
    }

    @Test
    void orderBefore18hGoesToNextDay() {
        assertThat(policy.cookingDateFor(vn("2026-10-28T17:59:59"))).isEqualTo(LocalDate.of(2026, 10, 29));
    }

    @Test
    void orderAtOrAfter18hGoesToDayAfterNext() {
        assertThat(policy.cookingDateFor(vn("2026-10-28T18:00:00"))).isEqualTo(LocalDate.of(2026, 10, 30));
        assertThat(policy.cookingDateFor(vn("2026-10-28T23:30:00"))).isEqualTo(LocalDate.of(2026, 10, 30));
    }

    @Test
    void cutoffOfCookingDayIs18hTheDayBefore() {
        assertThat(policy.cutoffFor(LocalDate.of(2026, 10, 29))).isEqualTo(vn("2026-10-28T18:00:00"));
    }

    @Test
    void orderedAtDecidesNotProcessingTime() {
        // placed 17:59:59, processed by the kitchen at 18:00:02 -> still accepted for the 29th (UC02 - BR4)
        assertThat(policy.accepts(vn("2026-10-28T17:59:59"), LocalDate.of(2026, 10, 29))).isTrue();
        assertThat(policy.accepts(vn("2026-10-28T18:00:00"), LocalDate.of(2026, 10, 29))).isFalse();
    }
}
