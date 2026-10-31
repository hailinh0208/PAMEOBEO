package com.pameobeo.kitchen.domain.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Picks the items shown at the top of the shop: the best sellers first (most jars sold, real numbers), then the
 * admin's picks ("Gợi ý của tiệm") to fill the remaining places, e.g. when the shop has no sales yet.
 * Only items on sale are shown.
 */
public final class MenuHighlights {

    public enum Reason { BEST_SELLER, FEATURED }

    public record Highlight(MenuItem item, int jarsSold, Reason reason) {}

    private MenuHighlights() {
    }

    public static List<Highlight> pick(List<MenuItem> menu, Map<UUID, Integer> jarsSold, int limit) {
        List<Highlight> picked = new ArrayList<>();
        menu.stream()
                .filter(MenuItem::isActive)
                .filter(m -> sold(jarsSold, m) > 0)
                .sorted(Comparator.comparingInt((MenuItem m) -> sold(jarsSold, m)).reversed()
                        .thenComparing(MenuItem.DISPLAY_ORDER))
                .limit(limit)
                .forEach(m -> picked.add(new Highlight(m, sold(jarsSold, m), Reason.BEST_SELLER)));
        menu.stream()
                .filter(MenuItem::isActive)
                .filter(MenuItem::isFeatured)
                .filter(m -> picked.stream().noneMatch(h -> h.item().getId().equals(m.getId())))
                .sorted(MenuItem.DISPLAY_ORDER)
                .limit(Math.max(0, limit - picked.size()))
                .forEach(m -> picked.add(new Highlight(m, sold(jarsSold, m), Reason.FEATURED)));
        return picked;
    }

    private static int sold(Map<UUID, Integer> jarsSold, MenuItem m) {
        return jarsSold.getOrDefault(m.getId(), 0);
    }
}
