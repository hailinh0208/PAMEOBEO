package com.pameobeo.kitchen.infrastructure.persistence;

import com.pameobeo.kitchen.application.port.out.CookingDayRepository;
import com.pameobeo.kitchen.application.port.out.MenuItemImageRepository;
import com.pameobeo.kitchen.application.port.out.MenuItemRepository;
import com.pameobeo.kitchen.application.port.out.SlotReservationRepository;
import com.pameobeo.kitchen.domain.model.CookingDay;
import com.pameobeo.kitchen.domain.model.MenuItem;
import com.pameobeo.kitchen.domain.model.MenuItemImage;
import com.pameobeo.kitchen.domain.model.SlotReservation;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
class CookingDayRepositoryAdapter implements CookingDayRepository {

    private final SpringDataCookingDayRepository jpa;

    CookingDayRepositoryAdapter(SpringDataCookingDayRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<CookingDay> findByDate(LocalDate date) {
        return jpa.findById(date).map(CookingDayJpaEntity::toDomain);
    }

    @Override
    public CookingDay save(CookingDay day) {
        // saveAndFlush so a version conflict surfaces here, inside the use case, not at commit time
        return jpa.saveAndFlush(CookingDayJpaEntity.fromDomain(day)).toDomain();
    }
}

@Component
class SlotReservationRepositoryAdapter implements SlotReservationRepository {

    private final SpringDataSlotReservationRepository jpa;

    SlotReservationRepositoryAdapter(SpringDataSlotReservationRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<SlotReservation> findByOrderId(UUID orderId) {
        return jpa.findById(orderId).map(SlotReservationJpaEntity::toDomain);
    }

    @Override
    public SlotReservation save(SlotReservation reservation) {
        return jpa.save(SlotReservationJpaEntity.fromDomain(reservation)).toDomain();
    }

    @Override
    public List<SlotReservation> findReservedOn(LocalDate cookingDate) {
        return jpa.findByCookingDateAndStatus(cookingDate, SlotReservation.Status.RESERVED).stream()
                .map(SlotReservationJpaEntity::toDomain).toList();
    }

    @Override
    public Map<UUID, Integer> jarsSoldSince(Instant since) {
        return jpa.sumJarsPerMenuItem(SlotReservation.Status.RESERVED, since).stream()
                .collect(Collectors.toMap(row -> (UUID) row[0], row -> ((Number) row[1]).intValue()));
    }
}

@Component
class MenuItemImageRepositoryAdapter implements MenuItemImageRepository {

    private final SpringDataMenuItemImageRepository jpa;

    MenuItemImageRepositoryAdapter(SpringDataMenuItemImageRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Map<UUID, List<UUID>> imageIdsByMenuItem() {
        return jpa.findAllIds().stream().collect(Collectors.groupingBy(row -> (UUID) row[0], LinkedHashMap::new,
                Collectors.mapping(row -> (UUID) row[1], Collectors.toList())));
    }

    @Override
    public List<UUID> imageIdsOf(UUID menuItemId) {
        return jpa.findIdsByMenuItemId(menuItemId);
    }

    @Override
    public Optional<MenuItemImage> find(UUID menuItemId, UUID imageId) {
        return jpa.findByIdAndMenuItemId(imageId, menuItemId).map(MenuItemImageJpaEntity::toDomain);
    }

    @Override
    public void save(MenuItemImage image) {
        jpa.saveAndFlush(MenuItemImageJpaEntity.fromDomain(image));
    }

    @Override
    public void delete(UUID imageId) {
        jpa.deleteById(imageId);
        jpa.flush();
    }

    @Override
    public void reorder(List<UUID> imageIds) {
        for (int i = 0; i < imageIds.size(); i++) {
            jpa.updatePosition(imageIds.get(i), i);
        }
    }
}

@Component
class MenuItemRepositoryAdapter implements MenuItemRepository {

    private final SpringDataMenuItemRepository jpa;

    MenuItemRepositoryAdapter(SpringDataMenuItemRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<MenuItem> findAll() {
        return jpa.findAll().stream().map(MenuItemJpaEntity::toDomain).sorted(MenuItem.DISPLAY_ORDER).toList();
    }

    @Override
    public List<MenuItem> findAllById(Collection<UUID> ids) {
        return jpa.findAllById(ids).stream().map(MenuItemJpaEntity::toDomain).toList();
    }

    @Override
    public Optional<MenuItem> findById(UUID id) {
        return jpa.findById(id).map(MenuItemJpaEntity::toDomain);
    }

    @Override
    public MenuItem save(MenuItem item) {
        return jpa.save(MenuItemJpaEntity.fromDomain(item)).toDomain();
    }
}
