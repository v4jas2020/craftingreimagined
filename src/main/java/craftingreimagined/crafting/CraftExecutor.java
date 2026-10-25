package craftingreimagined.crafting;

import craftingreimagined.CraftingReimaginedClient;
import craftingreimagined.compat.ServerCompatibility;
import craftingreimagined.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Naujesni importai kol keiciam:
import craftingreimagined.recipe.RecipeEntry;
import net.minecraft.world.inventory.InventoryMenu;

public final class CraftExecutor {

    private static String lastError = "";
    private static long lastPlacementTime = 0;

    private CraftExecutor() {
    }

    public record Placement(
            int sourceSlot,
            int gridSlot
    ) {
    }

    public record InventoryPlacement(
            int inventoryIndex,
            int row,
            int column
    ) {
    }

    public static boolean craftFromInventory(
            int amount,
            List<InventoryPlacement> ingredients
    ) {

        lastError = "";

        if (ingredients == null || ingredients.isEmpty()) {
            return fail("No ingredients were provided.");
        }

        List<Placement> placements = new ArrayList<>();

        for (InventoryPlacement ingredient : ingredients) {

            if (ingredient == null) {
                return fail("Invalid ingredient placement.");
            }

            int sourceSlot = SlotMapper.inventoryToMenuSlot(
                    ingredient.inventoryIndex()
            );

            int gridSlot = SlotMapper.gridPositionToMenuSlot(
                    ingredient.row(),
                    ingredient.column()
            );

            if (sourceSlot == -1) {
                return fail("Invalid player inventory index.");
            }

            if (gridSlot == -1) {
                return fail("Invalid crafting grid position.");
            }

            placements.add(new Placement(sourceSlot, gridSlot));
        }

        return craft(amount, placements);
    }

    public static boolean craft(
            int amount,
            List<Placement> placements
    ) {

        lastError = "";

        Minecraft client = Minecraft.getInstance();

        if (!ModConfig.isEnabled()) {
            return fail("Crafting Reimagined is disabled.");
        }

        if (!ServerCompatibility.canUseContainerAutomation()) {
            return fail("Container automation is unavailable.");
        }

        if (ClickQueue.isBusy()) {
            return fail("Another crafting operation is already running.");
        }

        if (client.player == null ||
                !(client.player.containerMenu instanceof CraftingMenu menu)) {

            return fail("Open a crafting table first.");
        }

        if (menu.slots.size() < 46) {
            return fail("Unsupported crafting menu layout.");
        }

        if (amount < 1 || amount > 64) {
            return fail("Craft amount must be between 1 and 64.");
        }

        if (placements == null ||
                placements.isEmpty() ||
                placements.size() > 9) {

            return fail("Invalid crafting ingredient list.");
        }

        if (!menu.getCarried().isEmpty()) {
            return fail("Empty your cursor before crafting.");
        }

        for (int slot = SlotMapper.GRID_START;
             slot <= SlotMapper.GRID_END;
             slot++) {

            if (!menu.slots.get(slot).getItem().isEmpty()) {
                return fail("Clear the crafting grid first.");
            }
        }

        Set<Integer> usedDestinations = new HashSet<>();

        Map<Integer, Integer> requiredItems = new HashMap<>();

        for (Placement placement : placements) {

            if (placement == null) {
                return fail("Null ingredient placement.");
            }

            int source = placement.sourceSlot();
            int destination = placement.gridSlot();

            if (!SlotMapper.isPlayerInventorySlot(source)) {
                return fail("Invalid source inventory slot: " + source);
            }

            if (!SlotMapper.isGridSlot(destination)) {
                return fail("Invalid crafting grid slot: " + destination);
            }

            if (!usedDestinations.add(destination)) {
                return fail("Duplicate crafting grid destination.");
            }

            requiredItems.merge(source, amount, Integer::sum);
        }

        Map<Integer, Integer> remainingItems = new HashMap<>();

        for (Map.Entry<Integer, Integer> entry : requiredItems.entrySet()) {

            int source = entry.getKey();
            int required = entry.getValue();

            ItemStack stack = menu.slots.get(source).getItem();

            if (stack.isEmpty()) {
                return fail("Ingredient slot " + source + " is empty.");
            }

            if (stack.getCount() < required) {

                return fail(
                        "Not enough ingredients in slot " + source
                                + ". Required: " + required
                                + ", available: " + stack.getCount()
                );
            }

            if (stack.getMaxStackSize() < amount) {

                return fail(
                        "Ingredient stack size is too small "
                                + "for the requested crafting amount."
                );
            }

            remainingItems.put(source, stack.getCount());
        }

        for (Placement placement : placements) {

            int source = placement.sourceSlot();
            int destination = placement.gridSlot();

            int before = remainingItems.get(source);

            ClickQueue.leftClick(source);

            for (int i = 0; i < amount; i++) {
                ClickQueue.rightClick(destination);
            }

            if (before > amount) {
                ClickQueue.leftClick(source);
            }

            remainingItems.put(source, before - amount);
        }

        CraftingReimaginedClient.LOGGER.info(
                "Queued {} crafting operations using {} grid slots.",
                amount,
                placements.size()
        );

        return true;
    }
    public static boolean placeRecipe(
            RecipeEntry recipe,
            boolean maximum
    ) {
        lastError = "";
        Minecraft client = Minecraft.getInstance();

        if (!ModConfig.isEnabled()) {
            return fail("Crafting Reimagined is disabled.");
        }
        if (recipe == null || !recipe.isCraftingRecipe()) {
            return fail("Select a crafting recipe.");
        }
        if (client.player == null
            || client.gameMode == null
            || client.getConnection() == null) {
            return fail("not connected to a world.");
        }
        if (ClickQueue.isBusy()) {
            return fail("Previous crafting operation still running.");
        }
        long now = System.nanoTime();
        if (now - lastPlacementTime < 300_000_000L) {
            return fail("Place recipe timed out.");
        }

        var menu = client.player.containerMenu;

        if (!(menu instanceof CraftingMenu)
            && !(menu instanceof InventoryMenu)) {
            return fail("Open your inventory or a crafting table.");
        }
        if (menu instanceof InventoryMenu
            && recipe.requiresCraftingTable()) {
        return fail("This recipe requires a crafting table.");
            }
        if (!menu.getCarried().isEmpty()) {
            return fail("Empty your cursor first.");
        }
        lastPlacementTime = now;

        client.gameMode.handlePlaceRecipe(
                menu.containerId,
                recipe.getId(),
                maximum
        );
        return true;
    }

    public static boolean placeCookingIngredient(RecipeEntry recipe) {
        lastError = "";
        Minecraft client = Minecraft.getInstance();

        if (recipe == null || !recipe.isCookingRecipe()) {
            return fail("Select a cooking recipe.");
        }
        if (client.player == null
                || client.level == null
                || client.gameMode == null) {
            return fail("Player is not ready.");
        }
        if (!(client.player.containerMenu instanceof AbstractFurnaceMenu menu)) {
            return fail("Open a furnace, smoker, or blast furnace.");
        }
        if (ClickQueue.isBusy()) {
            return fail("Another inventory operation is running.");
        }
        if (!menu.getCarried().isEmpty()) {
            return fail("Empty your cursor first.");
        }
        if (!menu.slots.get(0).getItem().isEmpty()) {
            return fail("Furnace input slot is occupied.");
        }

        ContextMap context = SlotDisplayContext.fromLevel(client.level);
        List<ItemStack> options = recipe.getIngredientOptions(
                0,
                context
        );
        if (options.isEmpty()) {
            return fail ("No valid cooking ingriedients found");
        }
        for (int slot = 3; slot < menu.slots.size(); slot++) {
            ItemStack stack = menu.slots.get(slot).getItem();

            if (stack.isEmpty()) {
                continue;
            }
            for (ItemStack option : options) {
                if (stack.getItem() == option.getItem()
                && menu.slots.get(0).mayPlace(stack)) {
                    ClickQueue.quickMove(slot);

                    return true;
                }
            }
        }
        return fail("Required ingredient not found in inventory.");
    }
    public static boolean isBusy() {
        return ClickQueue.isBusy();
    }

    public static int getRemainingClicks() {
        return ClickQueue.size();
    }

    public static void cancel() {

        ClickQueue.clear();

        CraftingReimaginedClient.LOGGER.info(
                "Crafting operation cancelled."
        );
    }

    public static String getLastError() {
        return lastError;
    }

    private static boolean fail(String message) {

        lastError = message;

        CraftingReimaginedClient.LOGGER.warn(
                "CraftExecutor: {}",
                message
        );

        return false;
    }
}