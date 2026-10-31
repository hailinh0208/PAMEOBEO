package com.pameobeo.kitchen.interfaces.rest;

import com.pameobeo.kitchen.application.port.in.AvailabilityQuery;
import com.pameobeo.kitchen.application.port.in.MenuQuery;
import com.pameobeo.kitchen.application.port.in.QuoteOrderUseCase;
import com.pameobeo.kitchen.domain.exception.ImageNotFoundException;
import com.pameobeo.kitchen.domain.model.Ingredient;
import com.pameobeo.kitchen.domain.model.MenuHighlights;
import com.pameobeo.kitchen.domain.model.Meat;
import com.pameobeo.kitchen.domain.model.MenuItem;
import com.pameobeo.kitchen.domain.model.MenuItemImage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/kitchen")
class KitchenController {

    private final MenuQuery menuQuery;
    private final AvailabilityQuery availabilityQuery;
    private final QuoteOrderUseCase quoteOrder;

    KitchenController(MenuQuery menuQuery, AvailabilityQuery availabilityQuery, QuoteOrderUseCase quoteOrder) {
        this.menuQuery = menuQuery;
        this.availabilityQuery = availabilityQuery;
        this.quoteOrder = quoteOrder;
    }

    @GetMapping("/menu")
    List<MenuItemResponse> menu() {
        return menuQuery.activeMenu().stream().map(MenuItemResponse::from).toList();
    }

    /** Carousel at the top of the shop: best sellers, completed with the admin's picks. */
    @GetMapping("/menu/highlights")
    List<HighlightResponse> highlights() {
        return menuQuery.highlights().stream()
                .map(h -> new HighlightResponse(MenuItemResponse.from(h.entry()), h.reason()))
                .toList();
    }

    /** A product photo. Image ids are never reused, so browsers may cache it forever. */
    @GetMapping("/menu/{id}/images/{imageId}")
    ResponseEntity<byte[]> image(@PathVariable UUID id, @PathVariable UUID imageId) {
        MenuItemImage image = menuQuery.image(id, imageId).orElseThrow(() -> new ImageNotFoundException(imageId));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .header("X-Content-Type-Options", "nosniff")
                .body(image.data());
    }

    @GetMapping("/availability")
    AvailabilityQuery.Availability availability(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return date == null ? availabilityQuery.nextCookingDay() : availabilityQuery.forDate(date);
    }

    /** Called by order-service (service-to-service). This is the endpoint protected by the Circuit Breaker. */
    @PostMapping("/quotes")
    QuoteOrderUseCase.Quote quote(@Valid @RequestBody QuoteRequest request) {
        return quoteOrder.quote(request.orderedAt(), request.menuItemIds());
    }

    record QuoteRequest(@NotNull Instant orderedAt, @NotEmpty List<@NotNull UUID> menuItemIds) {}

    /**
     * Public view of a menu item: what is in the jar, but not how much (the recipe quantities are the shop's
     * know-how, only {@link KitchenAdminController} returns them).
     * @param meats 1 flavor, or 2 for a mix; @param ingredients names only, in recipe order
     * @param jarsSold real number of jars sold over the last {@link MenuQuery#SALES_WINDOW_DAYS} days
     * @param imageIds photos, cover first: GET /api/kitchen/menu/{id}/images/{imageId}
     */
    record MenuItemResponse(UUID id, String name, List<Meat> meats, boolean mix, boolean withLiver, long priceVnd,
                            boolean active, List<String> ingredients, int jarsSold, List<UUID> imageIds) {
        static MenuItemResponse from(MenuQuery.MenuEntry e) {
            MenuItem m = e.item();
            return new MenuItemResponse(m.getId(), m.getName(), m.getMeats(), m.isMix(), m.isWithLiver(),
                    m.getPriceVnd(), m.isActive(), m.recipe().grams().keySet().stream().map(Ingredient::label).toList(),
                    e.jarsSold(), e.imageIds());
        }
    }

    record HighlightResponse(MenuItemResponse item, MenuHighlights.Reason reason) {}
}
