package craftingreimagined.crafting;

import craftingreimagined.recipe.IngredientMatcher;

import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class InventoryScanner {

    private InventoryScanner() {
    }

    public record FoundStack(
            int inventoryIndex,
            int menuSlot,
            ItemStack stack
    ) {

        public FoundStack {
            stack = stack.copy();
        }

        @Override
        public ItemStack stack() {
            return stack.copy();
        }

        public int count() {
            return stack.getCount();
        }
    }

    public static CraftingMenu getOpenCraftingMenu() {
        Minecraft client = Minecraft.getInstance();

        if (client.player == null) {
            return null;
        }

        if (client.player.containerMenu instanceof CraftingMenu menu) {
            return menu;
        }

        return null;
    }

    public static List<FoundStack> scanCurrent() {
        return scan(getOpenCraftingMenu());
    }

    public static List<FoundStack> scan(CraftingMenu menu) {

        if (menu == null || menu.slots.size() <= SlotMapper.INVENTORY_END) {
            return List.of();
        }

        List<FoundStack> found = new ArrayList<>();

        for (int inventoryIndex = 0; inventoryIndex < 36; inventoryIndex++) {

            int menuSlot = SlotMapper.inventoryToMenuSlot(inventoryIndex);

            if (menuSlot == -1 || menuSlot >= menu.slots.size()) {
                continue;
            }

            ItemStack stack = menu.slots.get(menuSlot).getItem();

            if (stack.isEmpty()) {
                continue;
            }

            found.add(new FoundStack(
                    inventoryIndex,
                    menuSlot,
                    stack
            ));
        }

        return List.copyOf(found);
    }

    public static List<FoundStack> findMatching(
            CraftingMenu menu,
            Ingredient ingredient
    ) {

        if (ingredient == null) {
            return List.of();
        }

        List<FoundStack> matches = new ArrayList<>();

        for (FoundStack found : scan(menu)) {
            if (IngredientMatcher.matches(ingredient, found.stack())) {
                matches.add(found);
            }
        }

        return List.copyOf(matches);
    }

    public static int countMatching(
            CraftingMenu menu,
            Ingredient ingredient
    ) {

        int total = 0;

        for (FoundStack found : findMatching(menu, ingredient)) {
            total += found.count();
        }

        return total;
    }

    public static int countItem(
            CraftingMenu menu,
            Item item
    ) {

        if (item == null) {
            return 0;
        }

        int total = 0;

        for (FoundStack found : scan(menu)) {
            if (found.stack().getItem() == item) {
                total += found.count();
            }
        }

        return total;
    }

    public static Optional<FoundStack> findFirstMatching(
            CraftingMenu menu,
            Ingredient ingredient,
            int requiredCount
    ) {

        if (ingredient == null || requiredCount < 1) {
            return Optional.empty();
        }

        for (FoundStack found : scan(menu)) {
            if (found.count() >= requiredCount
                    && IngredientMatcher.matches(ingredient, found.stack())) {
                return Optional.of(found);
            }
        }

        return Optional.empty();
    }

    public static int findFirstMatchingSlot(
            CraftingMenu menu,
            Ingredient ingredient,
            int requiredCount
    ) {

        return findFirstMatching(menu, ingredient, requiredCount)
                .map(FoundStack::menuSlot)
                .orElse(-1);
    }

    public static boolean hasEnough(
            CraftingMenu menu,
            Ingredient ingredient,
            int requiredCount
    ) {

        if (ingredient == null || requiredCount < 0) {
            return false;
        }

        return countMatching(menu, ingredient) >= requiredCount;
    }

    public static int getLargestMatchingStack(
            CraftingMenu menu,
            Ingredient ingredient
    ) {

        int largest = 0;

        for (FoundStack found : findMatching(menu, ingredient)) {
            largest = Math.max(largest, found.count());
        }

        return largest;
    }
}