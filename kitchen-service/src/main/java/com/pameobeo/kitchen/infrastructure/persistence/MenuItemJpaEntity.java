package com.pameobeo.kitchen.infrastructure.persistence;

import com.pameobeo.kitchen.domain.model.Meat;
import com.pameobeo.kitchen.domain.model.MenuItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Entity
@Table(name = "menu_items")
public class MenuItemJpaEntity {

    @Id
    private UUID id;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Meat meat1;
    /** null for a single-flavor pate */
    @Enumerated(EnumType.STRING)
    private Meat meat2;
    @Column(name = "with_liver", nullable = false)
    private boolean withLiver;
    @Column(name = "price_vnd", nullable = false)
    private long priceVnd;
    @Column(nullable = false)
    private boolean active;
    @Column(nullable = false)
    private boolean featured;

    protected MenuItemJpaEntity() {
    }

    static MenuItemJpaEntity fromDomain(MenuItem m) {
        MenuItemJpaEntity e = new MenuItemJpaEntity();
        e.id = m.getId();
        e.meat1 = m.getMeats().get(0);
        e.meat2 = m.isMix() ? m.getMeats().get(1) : null;
        e.withLiver = m.isWithLiver();
        e.priceVnd = m.getPriceVnd();
        e.active = m.isActive();
        e.featured = m.isFeatured();
        return e;
    }

    MenuItem toDomain() {
        List<Meat> meats = Stream.of(meat1, meat2).filter(m -> m != null).toList();
        return new MenuItem(id, meats, withLiver, priceVnd, active, featured);
    }
}
