package com.pameobeo.kitchen.infrastructure.persistence;

import com.pameobeo.kitchen.domain.model.SlotReservation;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface SpringDataCookingDayRepository extends JpaRepository<CookingDayJpaEntity, LocalDate> {
}

interface SpringDataSlotReservationRepository extends JpaRepository<SlotReservationJpaEntity, UUID> {

    List<SlotReservationJpaEntity> findByCookingDateAndStatus(LocalDate cookingDate, SlotReservation.Status status);

    /** rows of [menuItemId, jars] */
    @Query("""
            select l.menuItemId, sum(l.quantity) from SlotReservationJpaEntity r join r.lines l
            where r.status = :status and r.createdAt >= :since group by l.menuItemId""")
    List<Object[]> sumJarsPerMenuItem(SlotReservation.Status status, Instant since);
}

interface SpringDataMenuItemRepository extends JpaRepository<MenuItemJpaEntity, UUID> {
}

interface SpringDataMenuItemImageRepository extends JpaRepository<MenuItemImageJpaEntity, UUID> {

    /** rows of [menuItemId, imageId], cover first; the image bytes are not selected */
    @Query("select i.menuItemId, i.id from MenuItemImageJpaEntity i order by i.menuItemId, i.position, i.createdAt")
    List<Object[]> findAllIds();

    @Query("select i.id from MenuItemImageJpaEntity i where i.menuItemId = :menuItemId order by i.position, i.createdAt")
    List<UUID> findIdsByMenuItemId(UUID menuItemId);

    Optional<MenuItemImageJpaEntity> findByIdAndMenuItemId(UUID id, UUID menuItemId);

    @Modifying
    @Query("update MenuItemImageJpaEntity i set i.position = :position where i.id = :id")
    void updatePosition(UUID id, int position);
}
