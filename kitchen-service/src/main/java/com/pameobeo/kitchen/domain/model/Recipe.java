package com.pameobeo.kitchen.domain.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Grams of each ingredient in one 400 g jar. Every jar has 50 g carrot + 50 g pumpkin; the other 300 g are:
 * <ul>
 *   <li>with liver: 100 g liver + 200 g meat</li>
 *   <li>without liver: 300 g meat (the liver is replaced by meat)</li>
 * </ul>
 * A mix splits the meat evenly between its two flavors (100 + 100 g, or 150 + 150 g without liver).
 */
public record Recipe(Map<Ingredient, Integer> grams) {

    public static final int JAR_GRAMS = 400;
    static final int LIVER_GRAMS = 100;
    static final int CARROT_GRAMS = 50;
    static final int PUMPKIN_GRAMS = 50;

    public Recipe {
        grams = Collections.unmodifiableMap(new EnumMap<>(grams));
    }

    public static Recipe of(List<Meat> meats, boolean withLiver) {
        int meatGrams = JAR_GRAMS - CARROT_GRAMS - PUMPKIN_GRAMS - (withLiver ? LIVER_GRAMS : 0);
        Map<Ingredient, Integer> grams = new EnumMap<>(Ingredient.class);
        for (Meat meat : meats) {
            grams.put(meat.ingredient(), meatGrams / meats.size());
        }
        if (withLiver) {
            grams.put(Ingredient.LIVER, LIVER_GRAMS);
        }
        grams.put(Ingredient.CARROT, CARROT_GRAMS);
        grams.put(Ingredient.PUMPKIN, PUMPKIN_GRAMS);
        return new Recipe(grams);
    }

    public int totalGrams() {
        return grams.values().stream().mapToInt(Integer::intValue).sum();
    }
}
