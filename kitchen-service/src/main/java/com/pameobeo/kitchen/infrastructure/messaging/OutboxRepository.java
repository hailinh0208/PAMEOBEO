package com.pameobeo.kitchen.infrastructure.messaging;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface OutboxRepository extends JpaRepository<OutboxEventJpaEntity, UUID> {

    /** SKIP LOCKED lets several instances run the relay without sending the same row twice. */
    @Query(value = """
            SELECT * FROM outbox_events
            WHERE published_at IS NULL
            ORDER BY created_at
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEventJpaEntity> lockUnpublished(int limit);
}
