package com.pameobeo.kitchen.domain;

import static com.pameobeo.kitchen.domain.model.Ingredient.BEEF;
import static com.pameobeo.kitchen.domain.model.Ingredient.CARROT;
import static com.pameobeo.kitchen.domain.model.Ingredient.CHICKEN;
import static com.pameobeo.kitchen.domain.model.Ingredient.LIVER;
import static com.pameobeo.kitchen.domain.model.Ingredient.PUMPKIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

import com.pameobeo.kitchen.domain.model.Meat;
import com.pameobeo.kitchen.domain.model.MenuItem;
import com.pameobeo.kitchen.domain.model.Recipe;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MenuItemTest {

    private static MenuItem item(boolean withLiver, Meat... meats) {
        return new MenuItem(UUID.randomUUID(), List.of(meats), withLiver, 50_000, true, false);
    }

    @Test
    void singleFlavorWithLiver() {
        assertThat(item(true, Meat.CHICKEN).recipe().grams()).containsOnly(
                entry(CHICKEN, 200), entry(LIVER, 100), entry(CARROT, 50), entry(PUMPKIN, 50));
    }

    @Test
    void singleFlavorWithoutLiverReplacesTheLiverWithMeat() {
        assertThat(item(false, Meat.CHICKEN).recipe().grams()).containsOnly(
                entry(CHICKEN, 300), entry(CARROT, 50), entry(PUMPKIN, 50));
    }

    @Test
    void mixWithLiverSplitsTheMeat() {
        assertThat(item(true, Meat.CHICKEN, Meat.BEEF).recipe().grams()).containsOnly(
                entry(CHICKEN, 100), entry(BEEF, 100), entry(LIVER, 100), entry(CARROT, 50), entry(PUMPKIN, 50));
    }

    @Test
    void mixWithoutLiver() {
        assertThat(item(false, Meat.CHICKEN, Meat.BEEF).recipe().grams()).containsOnly(
                entry(CHICKEN, 150), entry(BEEF, 150), entry(CARROT, 50), entry(PUMPKIN, 50));
    }

    @Test
    void everyJarWeighs400Grams() {
        for (boolean withLiver : new boolean[] {true, false}) {
            assertThat(item(withLiver, Meat.TUNA).recipe().totalGrams()).isEqualTo(Recipe.JAR_GRAMS);
            assertThat(item(withLiver, Meat.TUNA, Meat.SALMON).recipe().totalGrams()).isEqualTo(Recipe.JAR_GRAMS);
        }
    }

    @Test
    void mixIsStoredInCanonicalOrderAndNamed() {
        MenuItem mix = item(false, Meat.SALMON, Meat.CHICKEN);

        assertThat(mix.getMeats()).containsExactly(Meat.CHICKEN, Meat.SALMON);
        assertThat(mix.getName()).isEqualTo("Pate mix gà + cá hồi không gan");
        assertThat(item(true, Meat.TUNA).getName()).isEqualTo("Pate cá ngừ có gan");
    }

    @Test
    void rejectsInvalidCombinations() {
        assertThatThrownBy(() -> item(true)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> item(true, Meat.BEEF, Meat.BEEF)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> item(true, Meat.BEEF, Meat.TUNA, Meat.SALMON))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
