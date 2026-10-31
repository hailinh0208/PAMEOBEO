package com.pameobeo.kitchen.domain.model;

/** Everything that goes into a jar. The meats double as the flavors a customer can pick (see {@link Meat}). */
public enum Ingredient {
    CHICKEN("Thịt gà"),
    TUNA("Cá ngừ"),
    BEEF("Thịt bò"),
    SALMON("Cá hồi"),
    LIVER("Gan"),
    CARROT("Cà rốt"),
    PUMPKIN("Bí đỏ");

    private final String label;

    Ingredient(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
