package com.pameobeo.kitchen.application.port.in;

import com.pameobeo.kitchen.domain.model.Ingredient;
import com.pameobeo.kitchen.domain.model.MenuItemImage;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface KitchenAdminUseCase {

    AvailabilityQuery.Availability changeCapacity(LocalDate cookingDate, int capacity);

    /** null leaves that field unchanged */
    MenuQuery.MenuEntry updateMenuItem(UUID id, Long priceVnd, Boolean active, Boolean featured);

    List<MenuQuery.MenuEntry> fullMenu();

    /** Adds a photo after the existing ones (at most {@link MenuItemImage#MAX_PER_ITEM}). @return the image ids, cover first */
    List<UUID> addImage(UUID menuItemId, byte[] data);

    /** @return the remaining image ids, cover first */
    List<UUID> deleteImage(UUID menuItemId, UUID imageId);

    /** Moves that photo first, so it becomes the cover. @return the image ids, cover first */
    List<UUID> makeCover(UUID menuItemId, UUID imageId);

    /** What to cook and buy for a cooking day: reserved jars per menu item and the summed recipes. */
    CookingPlan cookingPlan(LocalDate cookingDate);

    /** @param jarsWithoutDetails jars reserved before menu items were recorded: counted, but not in the ingredients */
    record CookingPlan(LocalDate cookingDate, int reservedJars, int jarsWithoutDetails, List<PlannedItem> items,
                       List<IngredientTotal> ingredients) {}

    record PlannedItem(UUID menuItemId, String name, int jars) {}

    record IngredientTotal(Ingredient ingredient, String label, int grams) {}
}
