package com.pameobeo.kitchen.application.port.out;

import com.pameobeo.kitchen.domain.model.MenuItemImage;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface MenuItemImageRepository {

    /** Image ids of every menu item, cover first. The image bytes are not loaded. */
    Map<UUID, List<UUID>> imageIdsByMenuItem();

    /** Image ids of one menu item, cover first. */
    List<UUID> imageIdsOf(UUID menuItemId);

    Optional<MenuItemImage> find(UUID menuItemId, UUID imageId);

    void save(MenuItemImage image);

    void delete(UUID imageId);

    /** Sets each image's position to its index in the list (0 = cover). */
    void reorder(List<UUID> imageIds);
}
