package com.pameobeo.kitchen.application.port.out;

import com.pameobeo.kitchen.domain.model.CookingDay;
import java.time.LocalDate;
import java.util.Optional;

public interface CookingDayRepository {

    Optional<CookingDay> findByDate(LocalDate date);

    /** Fails with an optimistic locking exception if another transaction changed the row meanwhile. */
    CookingDay save(CookingDay day);
}
