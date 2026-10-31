package com.pameobeo.kitchen.interfaces.rest;

import com.pameobeo.kitchen.application.port.in.AvailabilityQuery;
import com.pameobeo.kitchen.application.port.in.KitchenAdminUseCase;
import com.pameobeo.kitchen.application.port.in.MenuQuery;
import com.pameobeo.kitchen.domain.model.Ingredient;
import com.pameobeo.kitchen.domain.model.Meat;
import com.pameobeo.kitchen.domain.model.MenuItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/kitchen/admin")
class KitchenAdminController {

    private final KitchenAdminUseCase admin;

    KitchenAdminController(KitchenAdminUseCase admin) {
        this.admin = admin;
    }

    @PutMapping("/cooking-days/{date}/capacity")
    AvailabilityQuery.Availability changeCapacity(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody CapacityRequest request) {
        return admin.changeCapacity(date, request.capacity());
    }

    @GetMapping("/cooking-days/{date}/plan")
    KitchenAdminUseCase.CookingPlan cookingPlan(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return admin.cookingPlan(date);
    }

    @GetMapping("/menu")
    List<AdminMenuItemResponse> fullMenu() {
        return admin.fullMenu().stream().map(AdminMenuItemResponse::from).toList();
    }

    @PatchMapping("/menu/{id}")
    AdminMenuItemResponse updateMenuItem(@PathVariable UUID id, @Valid @RequestBody MenuItemPatch patch) {
        return AdminMenuItemResponse.from(admin.updateMenuItem(id, patch.priceVnd(), patch.active(), patch.featured()));
    }

    /** multipart/form-data with a "file" part. The type and size are checked by the domain (MenuItemImage). */
    @PostMapping("/menu/{id}/images")
    @ResponseStatus(HttpStatus.CREATED)
    ImagesResponse addImage(@PathVariable UUID id, @RequestParam("file") MultipartFile file) throws IOException {
        return new ImagesResponse(admin.addImage(id, file.getBytes()));
    }

    @DeleteMapping("/menu/{id}/images/{imageId}")
    ImagesResponse deleteImage(@PathVariable UUID id, @PathVariable UUID imageId) {
        return new ImagesResponse(admin.deleteImage(id, imageId));
    }

    @PutMapping("/menu/{id}/images/{imageId}/cover")
    ImagesResponse makeCover(@PathVariable UUID id, @PathVariable UUID imageId) {
        return new ImagesResponse(admin.makeCover(id, imageId));
    }

    /**
     * Like the public view, plus the recipe quantities and the admin's pick.
     * @param recipe grams of each ingredient in one jar; @param featured shown as "Gợi ý của tiệm"
     */
    record AdminMenuItemResponse(UUID id, String name, List<Meat> meats, boolean mix, boolean withLiver,
                                 long priceVnd, boolean active, boolean featured, List<RecipeLine> recipe,
                                 int jarsSold, List<UUID> imageIds) {
        static AdminMenuItemResponse from(MenuQuery.MenuEntry e) {
            MenuItem m = e.item();
            List<RecipeLine> recipe = m.recipe().grams().entrySet().stream()
                    .map(r -> new RecipeLine(r.getKey(), r.getKey().label(), r.getValue()))
                    .toList();
            return new AdminMenuItemResponse(m.getId(), m.getName(), m.getMeats(), m.isMix(), m.isWithLiver(),
                    m.getPriceVnd(), m.isActive(), m.isFeatured(), recipe, e.jarsSold(), e.imageIds());
        }
    }

    record RecipeLine(Ingredient ingredient, String label, int grams) {}

    /** @param imageIds the item's photos after the change, cover first */
    record ImagesResponse(List<UUID> imageIds) {}

    record CapacityRequest(@Min(0) int capacity) {}

    record MenuItemPatch(@Positive Long priceVnd, Boolean active, Boolean featured) {}
}
