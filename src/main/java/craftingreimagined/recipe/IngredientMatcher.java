package craftingreimagined.recipe;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;

public final class IngredientMatcher {

    private IngredientMatcher() {
    }


    public static boolean matches(
            Ingredient ingredient,
            ItemStack stack
    ) {

        if (ingredient == null || stack == null) {
            return false;
        }

        if (stack.isEmpty()) {
            return false;
        }

        return ingredient.test(stack);
    }

    public static boolean matchesItem(
            Item item,
            ItemStack stack
    ) {

        if (item == null || stack == null || stack.isEmpty()) {
            return false;
        }

        return stack.getItem() == item;
    }

    public static boolean hasEnough(
            Ingredient ingredient,
            ItemStack stack,
            int requiredCount
    ) {

        if (requiredCount < 1) {
            return false;
        }

        return matches(ingredient, stack)
                && stack.getCount() >= requiredCount;
    }


    public static int countMatching(
            Ingredient ingredient,
            Iterable<ItemStack> stacks
    ) {

        if (ingredient == null || stacks == null) {
            return 0;
        }

        int total = 0;

        for (ItemStack stack : stacks) {

            if (matches(ingredient, stack)) {
                total += stack.getCount();
            }
        }

        return total;
    }


    public static boolean matchesAny(
            Ingredient ingredient,
            Iterable<ItemStack> stacks
    ) {

        if (ingredient == null || stacks == null) {
            return false;
        }

        for (ItemStack stack : stacks) {

            if (matches(ingredient, stack)) {
                return true;
            }
        }

        return false;
    }


    public static int findFirstMatchingIndex(
            Ingredient ingredient,
            List<ItemStack> stacks,
            int requiredCount
    ) {

        if (ingredient == null
                || stacks == null
                || requiredCount < 1) {

            return -1;
        }

        for (int i = 0; i < stacks.size(); i++) {

            if (hasEnough(
                    ingredient,
                    stacks.get(i),
                    requiredCount
            )) {

                return i;
            }
        }

        return -1;
    }

    public static int getPossibleCraftCount(
            Ingredient ingredient,
            Iterable<ItemStack> stacks,
            int itemsPerCraft
    ) {

        if (itemsPerCraft <= 0) {
            return 0;
        }

        int available = countMatching(
                ingredient,
                stacks
        );

        return available / itemsPerCraft;
    }
}
