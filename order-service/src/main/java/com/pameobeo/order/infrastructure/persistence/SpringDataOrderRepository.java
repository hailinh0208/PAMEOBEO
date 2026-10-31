package com.pameobeo.order.infrastructure.persistence;

import com.pameobeo.order.domain.model.OrderStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataOrderRepository extends JpaRepository<OrderJpaEntity, UUID> {

    Optional<OrderJpaEntity> findByCustomerIdAndIdempotencyKey(UUID customerId, String idempotencyKey);

    List<OrderJpaEntity> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    List<OrderJpaEntity> findByCookingDateAndStatus(LocalDate cookingDate, OrderStatus status);

    List<OrderJpaEntity> findByCookingDateOrderByCreatedAtDesc(LocalDate cookingDate);

    List<OrderJpaEntity> findTop200ByOrderByCreatedAtDesc();
}
