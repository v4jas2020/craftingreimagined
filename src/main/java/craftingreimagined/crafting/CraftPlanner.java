package craftingreimagined.crafting;

import craftingreimagined.recipe.RecipeEntry;

import net.minecraft.client.Minecraft;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class CraftPlanner {

    public static final int MAX_CRAFTS = 64;

    private CraftPlanner() {
    }


    public record Plan(
            boolean possible,
            int requestedCrafts,
            int maximumCrafts,
            int outputCount,
            List<CraftExecutor.Placement> placements,
            String message
    ) {

        public Plan {
            placements = List.copyOf(placements);
        }
    }


    private record RequiredSlot(
            int gridSlot,
            Set<Item> alternatives
    ) {
    }


    public static Plan plan(
            RecipeEntry recipe,
            int requestedCrafts
    ) {

        if (recipe == null) {
            return failure(0, 0, "No recipe selected.");
        }

        if (!recipe.isCraftingRecipe()) {
            return failure(
                    requestedCrafts,
                    0,
                    "This is not a crafting table recipe."
            );
        }

        if (requestedCrafts < 1 || requestedCrafts > MAX_CRAFTS) {
            return failure(
                    requestedCrafts,
                    0,
                    "Craft amount must be between 1 and 64."
            );
        }

        Minecraft client = Minecraft.getInstance();

        if (client.player == null || client.level == null) {
            return failure(
                    requestedCrafts,
                    0,
                    "Player is not in a world."
            );
        }

        CraftingMenu menu = InventoryScanner.getOpenCraftingMenu();

        if (menu == null) {
            return failure(
                    requestedCrafts,
                    0,
                    "Open a crafting table first."
            );
        }

        ContextMap context = SlotDisplayContext.fromLevel(
                client.level
        );

        List<RequiredSlot> requirements = readRequirements(
                recipe,
                context
        );

        if (requirements == null || requirements.isEmpty()) {
            return failure(
                    requestedCrafts,
                    0,
                    "Cannot resolve recipe ingredients."
            );
        }

        List<InventoryScanner.FoundStack> inventory =
                InventoryScanner.scan(menu);

        int maximum = calculateMaximum(
                requirements,
                inventory
        );

        int outputCount = recipe.getOutputCount(
                requestedCrafts
        );

        if (requestedCrafts > maximum) {
            return new Plan(
                    false,
                    requestedCrafts,
                    maximum,
                    outputCount,
                    List.of(),
                    "Not enough ingredients. Maximum: "
                            + maximum + " crafts."
            );
        }

        List<CraftExecutor.Placement> placements = allocate(
                requirements,
                inventory,
                requestedCrafts
        );

        if (placements == null) {
            return new Plan(
                    false,
                    requestedCrafts,
                    maximum,
                    outputCount,
                    List.of(),
                    "Unable to allocate ingredients."
            );
        }

        return new Plan(
                true,
                requestedCrafts,
                maximum,
                outputCount,
                placements,
                "Ready to craft."
        );
    }


    public static Plan planForOutput(
            RecipeEntry recipe,
            int desiredOutput
    ) {

        if (recipe == null || desiredOutput < 1) {
            return failure(
                    0,
                    0,
                    "Invalid output amount."
            );
        }

        int crafts = recipe.getCraftsForOutput(
                desiredOutput
        );

        return plan(recipe, crafts);
    }


    public static int getMaximumCrafts(
            RecipeEntry recipe
    ) {

        if (recipe == null || !recipe.isCraftingRecipe()) {
            return 0;
        }

        Minecraft client = Minecraft.getInstance();

        CraftingMenu menu = InventoryScanner.getOpenCraftingMenu();

        if (client.level == null || menu == null) {
            return 0;
        }

        ContextMap context = SlotDisplayContext.fromLevel(
                client.level
        );

        List<RequiredSlot> requirements = readRequirements(
                recipe,
                context
        );

        if (requirements == null || requirements.isEmpty()) {
            return 0;
        }

        return calculateMaximum(
                requirements,
                InventoryScanner.scan(menu)
        );
    }


    public static boolean execute(
            Plan plan
    ) {

        if (plan == null || !plan.possible()) {
            return false;
        }

        return CraftExecutor.craft(
                plan.requestedCrafts(),
                plan.placements()
        );
    }

    private static List<RequiredSlot> readRequirements(
            RecipeEntry recipe,
            ContextMap context
    ) {

        List<RequiredSlot> requirements = new ArrayList<>();

        for (int slot = 0; slot < 9; slot++) {

            if (recipe.isGridSlotEmpty(slot)) {
                continue;
            }

            Set<Item> alternatives = new HashSet<>();

            for (ItemStack stack :
                    recipe.getIngredientOptions(slot, context)) {

                if (!stack.isEmpty()) {
                    alternatives.add(stack.getItem());
                }
            }

            if (alternatives.isEmpty()) {
                return null;
            }

            requirements.add(
                    new RequiredSlot(
                            SlotMapper.GRID_START + slot,
                            Set.copyOf(alternatives)
                    )
            );
        }

        return requirements;
    }


    private static int calculateMaximum(
            List<RequiredSlot> requirements,
            List<InventoryScanner.FoundStack> inventory
    ) {

        int low = 0;
        int high = MAX_CRAFTS;

        while (low < high) {

            int middle = (low + high + 1) / 2;

            List<CraftExecutor.Placement> placements = allocate(
                    requirements,
                    inventory,
                    middle
            );

            if (placements != null) {
                low = middle;
            } else {
                high = middle - 1;
            }
        }

        return low;
    }


    private static List<CraftExecutor.Placement> allocate(
            List<RequiredSlot> requirements,
            List<InventoryScanner.FoundStack> inventory,
            int amount
    ) {

        if (amount < 1 || requirements.isEmpty()) {
            return null;
        }

        List<Integer> units = new ArrayList<>();


        for (int i = 0; i < inventory.size(); i++) {

            InventoryScanner.FoundStack found = inventory.get(i);

            ItemStack stack = found.stack();

            if (stack.getMaxStackSize() < amount) {
                continue;
            }

            int capacity = stack.getCount() / amount;

            capacity = Math.min(
                    capacity,
                    requirements.size()
            );

            for (int j = 0; j < capacity; j++) {
                units.add(i);
            }
        }

        if (units.size() < requirements.size()) {
            return null;
        }


        int[] owner = new int[units.size()];

        Arrays.fill(owner, -1);

        for (int requirement = 0;
             requirement < requirements.size();
             requirement++) {

            boolean[] visited = new boolean[units.size()];

            if (!match(
                    requirement,
                    requirements,
                    inventory,
                    units,
                    owner,
                    visited
            )) {
                return null;
            }
        }


        int[] sources = new int[requirements.size()];

        Arrays.fill(sources, -1);

        for (int unit = 0; unit < units.size(); unit++) {

            int requirement = owner[unit];

            if (requirement >= 0) {

                int inventoryIndex = units.get(unit);

                sources[requirement] = inventory
                        .get(inventoryIndex)
                        .menuSlot();
            }
        }

        List<CraftExecutor.Placement> placements =
                new ArrayList<>();

        for (int i = 0; i < requirements.size(); i++) {

            if (sources[i] == -1) {
                return null;
            }

            placements.add(
                    new CraftExecutor.Placement(
                            sources[i],
                            requirements.get(i).gridSlot()
                    )
            );
        }

        return List.copyOf(placements);
    }


    private static boolean match(
            int requirement,
            List<RequiredSlot> requirements,
            List<InventoryScanner.FoundStack> inventory,
            List<Integer> units,
            int[] owner,
            boolean[] visited
    ) {

        for (int unit = 0; unit < units.size(); unit++) {

            if (visited[unit]) {
                continue;
            }

            int inventoryIndex = units.get(unit);

            Item item = inventory
                    .get(inventoryIndex)
                    .stack()
                    .getItem();

            if (!requirements.get(requirement)
                    .alternatives()
                    .contains(item)) {

                continue;
            }

            visited[unit] = true;

            if (owner[unit] == -1 || match(
                    owner[unit],
                    requirements,
                    inventory,
                    units,
                    owner,
                    visited
            )) {

                owner[unit] = requirement;
                return true;
            }
        }

        return false;
    }


    private static Plan failure(
            int requested,
            int maximum,
            String message
    ) {

        return new Plan(
                false,
                requested,
                maximum,
                0,
                List.of(),
                message
        );
    }
}