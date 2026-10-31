package com.pameobeo.order.infrastructure.persistence;

import com.pameobeo.order.application.port.out.OrderRepository;
import com.pameobeo.order.domain.model.Order;
import com.pameobeo.order.domain.model.OrderStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
class OrderRepositoryAdapter implements OrderRepository {

    private final SpringDataOrderRepository jpa;

    OrderRepositoryAdapter(SpringDataOrderRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Order save(Order order) {
        try {
            return jpa.saveAndFlush(OrderJpaEntity.fromDomain(order)).toDomain();
        } catch (DataIntegrityViolationException e) {
            // unique (customer_id, idempotency_key): a concurrent request with the same key won the race
            throw new DuplicateOrderRequestException(e);
        }
    }

    @Override
    public Optional<Order> findById(UUID id) {
        return jpa.findById(id).map(OrderJpaEntity::toDomain);
    }

    @Override
    public Optional<Order> findByIdempotencyKey(UUID customerId, String idempotencyKey) {
        return jpa.findByCustomerIdAndIdempotencyKey(customerId, idempotencyKey).map(OrderJpaEntity::toDomain);
    }

    @Override
    public List<Order> findByCustomer(UUID customerId) {
        return jpa.findByCustomerIdOrderByCreatedAtDesc(customerId).stream().map(OrderJpaEntity::toDomain).toList();
    }

    @Override
    public List<Order> findByCookingDateAndStatus(LocalDate cookingDate, OrderStatus status) {
        return jpa.findByCookingDateAndStatus(cookingDate, status).stream().map(OrderJpaEntity::toDomain).toList();
    }

    @Override
    public List<Order> findAll(LocalDate cookingDateOrNull) {
        List<OrderJpaEntity> rows = cookingDateOrNull == null
                ? jpa.findTop200ByOrderByCreatedAtDesc()
                : jpa.findByCookingDateOrderByCreatedAtDesc(cookingDateOrNull);
        return rows.stream().map(OrderJpaEntity::toDomain).toList();
    }
}
