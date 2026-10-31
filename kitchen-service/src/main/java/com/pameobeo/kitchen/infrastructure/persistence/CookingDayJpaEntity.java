package com.pameobeo.kitchen.infrastructure.persistence;

import com.pameobeo.kitchen.domain.model.CookingDay;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;

@Entity
@Table(name = "cooking_days")
public class CookingDayJpaEntity {

    @Id
    @Column(name = "cooking_date")
    private LocalDate date;
    @Column(nullable = false)
    private int capacity;
    @Column(name = "reserved_count", nullable = false)
    private int reservedCount;
    /** Optimistic locking: UPDATE ... WHERE version = ? fails if someone else updated first. */
    @Version
    private Long version;

    protected CookingDayJpaEntity() {
    }

    static CookingDayJpaEntity fromDomain(CookingDay day) {
        CookingDayJpaEntity e = new CookingDayJpaEntity();
        e.date = day.getDate();
        e.capacity = day.getCapacity();
        e.reservedCount = day.getReservedCount();
        e.version = day.getVersion();
        return e;
    }

    CookingDay toDomain() {
        return new CookingDay(date, capacity, reservedCount, version);
    }
}
