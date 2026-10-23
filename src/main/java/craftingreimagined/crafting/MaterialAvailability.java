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
            Map.Entry<Item, Integer> entry =
                    availableItems.get(i);

            int itemNode = itemStart + i;

            capacity[source][itemNode] = entry.getValue();

            for (int slot = 0; slot < GRID_SIZE; slot++) {
                if (alternatives.get(slot).contains(entry.getKey())) {
                    capacity[itemNode][gridStart + slot] = crafts;
                }
                // nuo cia:
            }
        }

        for (int slot = 0; slot < GRID_SIZE; slot++) {
            if (!recipe.isGridSlotEmpty(slot)) {
                capacity[gridStart + slot][sink] = crafts;
            }
        }

        calculateMaximumFlow(
                capacity,
                source,
                sink
        );

        for (int slot = 0; slot < GRID_SIZE; slot++) {
            if (!recipe.isGridSlotEmpty(slot)) {
                missing[slot] = capacity[gridStart + slot][sink];
            }
        }
        return missing;
    }

    public static int getTotalMissing(int[] missing) {
        if (missing == null) {
            return 0;
        }
        int total = 0;

        for (int amount : missing) {
            total += Math.max(0, amount);
        }
        return total;
    }

    public static boolean hasAllMaterials(
            RecipeEntry recipe,
            int desiredOutput,
            ContextMap context
    ) {
        if (recipe == null
                || !recipe.isCraftingRecipe()
                || desiredOutput <= 0
                || context == null) {
            return false;
        }
        int[] missing = getMissingPerSlot(
                recipe,
                desiredOutput,
                context
        );
        return getTotalMissing(missing) == 0;
    }
    private static Map<Item, Integer> ScanInventory() {
        Minecraft client = Minecraft.getInstance();
        Map<Item, Integer> available = new HashMap<>();
        if (client.player == null) {
            return available;
        }

        for (int slot = 0; slot < 36; slot ++) {
            ItemStack stack = client.player
                    .getInventory()
                    .getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            available.merge(
                    stack.getItem(),
                    stack.getCount(),
                    Integer::sum
            );
        }
        return available;
    }

    private static void calculateMaximumFlow(
            int[][] capactity,
            int source,
            int sink
    ) {
        int nodeCount = capacity.length;
        int[] parent = new int[NodeCount];
        while (true) {
            Arrays.fill(parent, -1);
            parent[source] = source;
            ArrayDeque<Integer> queue = new ArrayDeque<>();
            queue.addLast(source);

            while (!queue.isEmpty() && parent[sink] == -1) {
                int current = queue.removeFirst();

                for (int next = 0; next < nodeCount; next++) {
                    if (parent[next] != -1
                        || capacity[current][next] <= 0) {
                        continue;
                    }
                    parent[next] = current;

                    if (next == sink) {
                        break;
                    }

                    queue.addLast(next);
                }
            }

            if (parent[sink] == -1) {
                break;
            }
            int flow = Integer.MAX_VALUE;

            for (int node = sink;
                node != source;
                node = parent[node])  {

                int previous = parent[node];
                flow = Math.min(
                        flow,
                        capacity[previous][node]
                );
            }
            for (int node = sink;
                node != source;
                node = parent[node]) {
                        int previous = parent[node];
                        capactity[previous][node] -= flow;
                        capactity[node][previous] += flow;
            }
        }
    }
}




