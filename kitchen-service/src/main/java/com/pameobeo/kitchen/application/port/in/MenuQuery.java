package com.pameobeo.kitchen.application.port.in;

import com.pameobeo.kitchen.domain.model.MenuHighlights;
import com.pameobeo.kitchen.domain.model.MenuItem;
import com.pameobeo.kitchen.domain.model.MenuItemImage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MenuQuery {

    /** Sales are counted over this many days: jars of confirmed orders, cancelled ones excluded. */
    int SALES_WINDOW_DAYS = 30;

    int HIGHLIGHTS = 5;

    /** Items on sale, each with the jars sold over the last {@link #SALES_WINDOW_DAYS} days and its photos. */
    List<MenuEntry> activeMenu();

    /** Up to {@link #HIGHLIGHTS} best sellers, completed with the admin's picks. */
    List<HighlightEntry> highlights();

    Optional<MenuItemImage> image(UUID menuItemId, UUID imageId);

    /** @param imageIds photos of the item, cover first (empty if the shop has not uploaded any) */
    record MenuEntry(MenuItem item, int jarsSold, List<UUID> imageIds) {}

    record HighlightEntry(MenuEntry entry, MenuHighlights.Reason reason) {}
}
