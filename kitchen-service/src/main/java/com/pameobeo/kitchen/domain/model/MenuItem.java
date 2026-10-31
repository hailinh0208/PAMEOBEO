package com.pameobeo.kitchen.domain.model;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * One sellable combination: 1 flavor or a mix of 2, with or without liver. Each combination has its own
 * price (full price table) and can be switched off on its own. The recipe follows from the combination.
 */
public class MenuItem {

    /** Single before mix, with liver before without, then by flavor. */
    public static final Comparator<MenuItem> DISPLAY_ORDER = Comparator
            .comparing(MenuItem::isMix)
            .thenComparing(m -> !m.isWithLiver())
            .thenComparing(m -> m.getMeats().get(0))
            .thenComparing(m -> m.getMeats().get(m.getMeats().size() - 1));

    private final UUID id;
    private final List<Meat> meats;
    private final boolean withLiver;
    private long priceVnd;
    private boolean active;
    /** Picked by the admin as "Gợi ý của tiệm": shown while there are not enough sales to rank best sellers. */
    private boolean featured;

    public MenuItem(UUID id, List<Meat> meats, boolean withLiver, long priceVnd, boolean active, boolean featured) {
        this.id = Objects.requireNonNull(id);
        if (meats == null || meats.isEmpty() || meats.size() > 2) {
            throw new IllegalArgumentException("a pate has 1 flavor or a mix of 2");
        }
        if (meats.stream().distinct().count() != meats.size()) {
            throw new IllegalArgumentException("a mix needs 2 different flavors");
        }
        this.meats = meats.stream().sorted().toList();
        this.withLiver = withLiver;
        if (priceVnd <= 0) {
            throw new IllegalArgumentException("price must be positive");
        }
        this.priceVnd = priceVnd;
        this.active = active;
        this.featured = featured;
    }

    public void changePrice(long newPriceVnd) {
        if (newPriceVnd <= 0) {
            throw new IllegalArgumentException("price must be positive");
        }
        this.priceVnd = newPriceVnd;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setFeatured(boolean featured) {
        this.featured = featured;
    }

    /** e.g. "Pate gà có gan", "Pate mix gà + bò không gan" */
    public String getName() {
        String flavors = meats.stream().map(Meat::label).collect(Collectors.joining(" + "));
        return "Pate " + (isMix() ? "mix " : "") + flavors + (withLiver ? " có gan" : " không gan");
    }

    public Recipe recipe() {
        return Recipe.of(meats, withLiver);
    }

    public boolean isMix() { return meats.size() == 2; }
    public UUID getId() { return id; }
    public List<Meat> getMeats() { return meats; }
    public boolean isWithLiver() { return withLiver; }
    public long getPriceVnd() { return priceVnd; }
    public boolean isActive() { return active; }
    public boolean isFeatured() { return featured; }
}
