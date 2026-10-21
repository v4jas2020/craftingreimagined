package craftingreimagined;

import craftingreimagined.recipe.RecipeEntry;

import net.minecraft.client.Minecraft;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.awt.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MaterialAvailability {
    private static final int GRID_SIZE = 9;
    private MaterialAvailability() {
    }

    public static int[] getMissingPerSlot(
            RecipeEntry recipe,
            int desiredOutput,
            ContextMap context
    ) {

        int[] missing = new int[GRID_SIZE];
        if (recipe == null
                || context == null
                || !recipe.isCraftingRecipe()
                || desiredOutput < 0) {
            return missing;
        }

        int crafts = recipe.getCraftsForOutput(
                desiredOutput
        );
        if (crafts <= 0) {
            return missing;
        }

        Map<Item, Integer> inventory = ScanInventory();

        List<Set<Item>> alternatives = new ArrayList<>();

        for (int slot = 0; slot < GRID_SIZE; slot++) {
            Set<Item> acceptedItems = new HashSet<>();

                if (!recipe.isGridSlotEmpty(slot)) {
                    for (ItemStack option :
                        recipe.getIngredientOptions(slot, context)) {

                        if (!option.isEmpty()) {
                            acceptedItems.add(option.getItem());
                        }
                    }
                }
                alternatives.add(acceptedItems);
        }

        List<Map.Entry<Item, Integer>> availableItems =
                new ArrayList<>(inventory.entrySet());

        int itemCount = availableItems.size();
        int source = 0;
        int itemStart = 1;
        int gridStart = 1;
        int sink = gridStart + GRID_SIZE;

        int[][] capacity = new int[sink + 1][sink + 1];
            for (int i = 0; i < itemCount; i++) {
                Map.Entry<Item, Integer > entry =
                        availableItems.get(i);

                int itemNode = itemStart + i;

                capacity[source][itemNode] = entry.getValue();

                for (int slot = 0; slot < GRID_SIZE; slot++) {
                    if // OK I WILL STOP CODING HERE I HAVE BEEN TRYING TO STOP FOR PAST 10 mins since its late and I am tired, I need to rest maybe
                }
            }
    }
}


