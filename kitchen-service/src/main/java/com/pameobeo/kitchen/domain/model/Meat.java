package com.pameobeo.kitchen.domain.model;

/** Flavors the customer picks. Declaration order is the display order and the canonical order of a mix. */
public enum Meat {
    CHICKEN("gà", Ingredient.CHICKEN),
    TUNA("cá ngừ", Ingredient.TUNA),
    BEEF("bò", Ingredient.BEEF),
    SALMON("cá hồi", Ingredient.SALMON);

    private final String label;
    private final Ingredient ingredient;

    Meat(String label, Ingredient ingredient) {
        this.label = label;
        this.ingredient = ingredient;
    }

    public String label() {
        return label;
    }

    public Ingredient ingredient() {
        return ingredient;
    }
}
