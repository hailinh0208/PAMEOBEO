package com.pameobeo.kitchen.infrastructure.persistence;

import com.pameobeo.kitchen.domain.model.MenuItemImage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "menu_item_images")
public class MenuItemImageJpaEntity {

    @Id
    private UUID id;
    @Column(name = "menu_item_id", nullable = false)
    private UUID menuItemId;
    @Column(nullable = false)
    private int position;
    @Column(name = "content_type", nullable = false)
    private String contentType;
    @Column(nullable = false)
    private byte[] data;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected MenuItemImageJpaEntity() {
    }

    static MenuItemImageJpaEntity fromDomain(MenuItemImage i) {
        MenuItemImageJpaEntity e = new MenuItemImageJpaEntity();
        e.id = i.id();
        e.menuItemId = i.menuItemId();
        e.position = i.position();
        e.contentType = i.contentType();
        e.data = i.data();
        e.createdAt = i.createdAt();
        return e;
    }

    MenuItemImage toDomain() {
        return new MenuItemImage(id, menuItemId, position, contentType, data, createdAt);
    }
}
