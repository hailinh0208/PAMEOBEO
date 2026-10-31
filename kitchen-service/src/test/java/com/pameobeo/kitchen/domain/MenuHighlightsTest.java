package com.pameobeo.kitchen.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.pameobeo.kitchen.domain.model.Meat;
import com.pameobeo.kitchen.domain.model.MenuHighlights;
import com.pameobeo.kitchen.domain.model.MenuHighlights.Highlight;
import com.pameobeo.kitchen.domain.model.MenuHighlights.Reason;
import com.pameobeo.kitchen.domain.model.MenuItem;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MenuHighlightsTest {

    private static final MenuItem CHICKEN = item(Meat.CHICKEN, true, false);
    private static final MenuItem TUNA = item(Meat.TUNA, true, false);
    private static final MenuItem BEEF = item(Meat.BEEF, true, true);
    private static final MenuItem SALMON = item(Meat.SALMON, true, true);
    private static final MenuItem OFF_SALE = item(Meat.SALMON, false, true);
    private static final List<MenuItem> MENU = List.of(CHICKEN, TUNA, BEEF, SALMON, OFF_SALE);

    private static MenuItem item(Meat meat, boolean active, boolean featured) {
        return new MenuItem(UUID.randomUUID(), List.of(meat), true, 50_000, active, featured);
    }

    private static List<MenuItem> items(List<Highlight> highlights) {
        return highlights.stream().map(Highlight::item).toList();
    }

    @Test
    void noSalesYetShowsTheAdminsPicks() {
        List<Highlight> picked = MenuHighlights.pick(MENU, Map.of(), 5);

        assertThat(items(picked)).containsExactly(BEEF, SALMON); // OFF_SALE is featured but not on sale
        assertThat(picked).allMatch(h -> h.reason() == Reason.FEATURED);
    }

    @Test
    void bestSellersComeFirstWithTheirRealCount() {
        List<Highlight> picked = MenuHighlights.pick(MENU, Map.of(TUNA.getId(), 12, CHICKEN.getId(), 30), 5);

        assertThat(items(picked)).containsExactly(CHICKEN, TUNA, BEEF, SALMON);
        assertThat(picked.get(0)).isEqualTo(new Highlight(CHICKEN, 30, Reason.BEST_SELLER));
        assertThat(picked.get(2).reason()).isEqualTo(Reason.FEATURED);
    }

    @Test
    void aFeaturedBestSellerIsListedOnceAsBestSeller() {
        List<Highlight> picked = MenuHighlights.pick(MENU, Map.of(BEEF.getId(), 3), 5);

        assertThat(items(picked)).containsExactly(BEEF, SALMON);
        assertThat(picked.get(0).reason()).isEqualTo(Reason.BEST_SELLER);
    }

    @Test
    void neverMoreThanTheLimitAndNothingOffSale() {
        List<Highlight> picked = MenuHighlights.pick(MENU,
                Map.of(CHICKEN.getId(), 5, TUNA.getId(), 4, BEEF.getId(), 3, OFF_SALE.getId(), 99), 2);

        assertThat(items(picked)).containsExactly(CHICKEN, TUNA);
    }
}
