package craftingreimagined.recipe;

import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.item.crafting.display.SlotDisplay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class RecipeEntry {

    public enum RecipeType {
        CRAFTING_SHAPED,
        CRAFTING_SHAPELESS,
        SMELTING,
        BLASTING,
        SMOKING,
        CAMPFIRE,
        BREWING
    }

    private final RecipeDisplayId id;
    private final RecipeType type;
    private final ItemStack result;

    private final int width;
    private final int height;

    private final List<SlotDisplay> grid;
    private final List<Ingredient> requirements;

    private final boolean hasRequirements;

    private final SlotDisplay fuelDisplay;

    private final int cookingTime;
    private final float experience;

    public RecipeEntry(
            RecipeDisplayId id,
            RecipeType type,
            ItemStack result,
            int width,
            int height,
            List<SlotDisplay> grid,
            List<Ingredient> requirements,
            boolean hasRequirements,
            SlotDisplay fuelDisplay,
            int cookingTime,
            float experience
    ) {

        this.id = Objects.requireNonNull(id);
        this.type = Objects.requireNonNull(type);

        if (result == null || result.isEmpty()) {
            throw new IllegalArgumentException(
                    "Recipe result cannot be empty."
            );
        }

        if (width < 1 || width > 3) {
            throw new IllegalArgumentException(
                    "Recipe width must be between 1 and 3."
            );
        }

        if (height < 1 || height > 3) {
            throw new IllegalArgumentException(
                    "Recipe height must be between 1 and 3."
            );
        }

        if (grid == null || grid.size() != 9) {
            throw new IllegalArgumentException(
                    "Recipe grid must contain exactly 9 slots."
            );
        }

        this.result = result.copy();
        this.width = width;
        this.height = height;

        this.grid = Collections.unmodifiableList(
                new ArrayList<>(grid)
        );

        this.requirements = List.copyOf(
                Objects.requireNonNull(requirements)
        );

        this.hasRequirements = hasRequirements;
        this.fuelDisplay = fuelDisplay;

        this.cookingTime = Math.max(0, cookingTime);
        this.experience = Math.max(0.0f, experience);
    }


    public RecipeDisplayId getId() {
        return id;
    }

    public int getDisplayIndex() {
        return id.index();
    }

    public RecipeType getType() {
        return type;
    }

    public ItemStack getResult() {
        return result.copy();
    }

    public int getResultCount() {
        return result.getCount();
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }


    public boolean isCraftingRecipe() {
        return type == RecipeType.CRAFTING_SHAPED
                || type == RecipeType.CRAFTING_SHAPELESS;
    }

    public boolean isShaped() {
        return type == RecipeType.CRAFTING_SHAPED;
    }

    public boolean isShapeless() {
        return type == RecipeType.CRAFTING_SHAPELESS;
    }

    public boolean isCookingRecipe() {
        return type == RecipeType.SMELTING
                || type == RecipeType.BLASTING
                || type == RecipeType.SMOKING
                || type == RecipeType.CAMPFIRE;
    }

    public boolean requiresCraftingTable() {
        return isCraftingRecipe()
                && (width > 2 || height > 2);
    }

    // Crafting grid

    public List<SlotDisplay> getGrid() {
        return grid;
    }

    public SlotDisplay getGridIngredient(int slot) {

        if (slot < 0 || slot >= 9) {
            throw new IndexOutOfBoundsException(
                    "Grid slot must be between 0 and 8."
            );
        }

        return grid.get(slot);
    }

    public SlotDisplay getGridIngredient(
            int row,
            int column
    ) {

        if (row < 0 || row > 2
                || column < 0 || column > 2) {

            throw new IndexOutOfBoundsException(
                    "Invalid crafting grid coordinates."
            );
        }

        return grid.get(row * 3 + column);
    }

    public boolean isGridSlotEmpty(int slot) {

        SlotDisplay display = getGridIngredient(slot);

        return display == null
                || display instanceof SlotDisplay.Empty;
    }

    public int getIngredientSlotCount() {

        int count = 0;

        for (int i = 0; i < 9; i++) {

            if (!isGridSlotEmpty(i)) {
                count++;
            }
        }

        return count;
    }

    public boolean hasCraftingRequirements() {
        return hasRequirements;
    }

    public List<Ingredient> getCraftingRequirements() {
        return requirements;
    }

    // Cooking information

    public SlotDisplay getFuelDisplay() {
        return fuelDisplay;
    }

    public int getCookingTime() {
        return cookingTime;
    }

    public float getExperience() {
        return experience;
    }

    // Quantity calculations

    public int getOutputCount(int crafts) {

        if (crafts < 0) {
            throw new IllegalArgumentException(
                    "Craft count cannot be negative."
            );
        }

        return Math.toIntExact(
                (long) crafts * getResultCount()
        );
    }

    public int getCraftsForOutput(int desiredAmount) {

        if (desiredAmount <= 0) {
            return 0;
        }

        int perCraft = getResultCount();

        return desiredAmount / perCraft
                + (desiredAmount % perCraft == 0 ? 0 : 1);
    }

    public int getIngredientCountForCrafts(int crafts) {

        if (crafts < 0) {
            throw new IllegalArgumentException(
                    "Craft count cannot be negative."
            );
        }

        return Math.toIntExact(
                (long) getIngredientSlotCount() * crafts
        );
    }


    public List<ItemStack> getIngredientOptions(
            int slot,
            ContextMap context
    ) {

        SlotDisplay display = getGridIngredient(slot);

        if (display == null
                || display instanceof SlotDisplay.Empty) {

            return List.of();
        }

        List<ItemStack> options = new ArrayList<>();

        for (ItemStack stack : display.resolveForStacks(context)) {

            if (!stack.isEmpty()) {
                options.add(stack.copy());
            }
        }

        return options;
    }

    public List<ItemStack> getFuelOptions(ContextMap context) {

        if (fuelDisplay == null) {
            return List.of();
        }

        List<ItemStack> options = new ArrayList<>();

        for (ItemStack stack : fuelDisplay.resolveForStacks(context)) {

            if (!stack.isEmpty()) {
                options.add(stack.copy());
            }
        }

        return options;
    }

    @Override
    public String toString() {

        return "RecipeEntry{" +
                "id=" + id +
                ", type=" + type +
                ", result=" + result +
                ", width=" + width +
                ", height=" + height +
                ", ingredients=" + getIngredientSlotCount() +
                '}';
    }
}