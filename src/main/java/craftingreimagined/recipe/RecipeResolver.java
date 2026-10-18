package craftingreimagined.recipe;

import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;

import net.minecraft.util.context.ContextMap;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import net.minecraft.world.item.crafting.display.FurnaceRecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class RecipeResolver {

    private RecipeResolver() {
    }

    public static List<RecipeEntry> getKnownRecipes() {

        Minecraft client = Minecraft.getInstance();

        if (client.player == null || client.level == null) {
            return List.of();
        }

        ContextMap context = SlotDisplayContext.fromLevel(
                client.level
        );

        return resolveRecipeBook(
                client.player.getRecipeBook(),
                context
        );
    }

    /**
     * Converts the client's recipe book entries
     * into Crafting Reimagined recipe entries.
     */
    public static List<RecipeEntry> resolveRecipeBook(
            ClientRecipeBook recipeBook,
            ContextMap context
    ) {

        if (recipeBook == null || context == null) {
            return List.of();
        }

        List<RecipeEntry> resolved = new ArrayList<>();

        Set<RecipeDisplayId> processed = new HashSet<>();

        for (RecipeCollection collection : recipeBook.getCollections()) {

            for (RecipeDisplayEntry entry : collection.getRecipes()) {

                if (!processed.add(entry.id())) {
                    continue;
                }

                resolve(entry, context).ifPresent(
                        resolved::add
                );
            }
        }

        return List.copyOf(resolved);
    }


    public static Optional<RecipeEntry> resolve(
            RecipeDisplayEntry entry,
            ContextMap context
    ) {

        if (entry == null || context == null) {
            return Optional.empty();
        }

        RecipeDisplay display = entry.display();

        ItemStack result = ItemStack.EMPTY;

        for (ItemStack stack : entry.resultItems(context)) {

            if (!stack.isEmpty()) {
                result = stack.copy();
                break;
            }
        }

        if (result.isEmpty()) {
            return Optional.empty();
        }

        List<SlotDisplay> grid = emptyGrid();

        RecipeEntry.RecipeType type;

        int width;
        int height;

        SlotDisplay fuel = null;

        int cookingTime = 0;
        float experience = 0.0f;


        if (display instanceof ShapedCraftingRecipeDisplay shaped) {

            type = RecipeEntry.RecipeType.CRAFTING_SHAPED;

            width = shaped.width();
            height = shaped.height();

            if (width < 1 || width > 3
                    || height < 1 || height > 3) {

                return Optional.empty();
            }

            List<SlotDisplay> ingredients = shaped.ingredients();

            if (ingredients.size() != width * height) {
                return Optional.empty();
            }

            for (int row = 0; row < height; row++) {

                for (int column = 0; column < width; column++) {

                    int sourceIndex = row * width + column;
                    int gridIndex = row * 3 + column;

                    grid.set(
                            gridIndex,
                            ingredients.get(sourceIndex)
                    );
                }
            }
        }


        else if (display instanceof ShapelessCraftingRecipeDisplay shapeless) {

            type = RecipeEntry.RecipeType.CRAFTING_SHAPELESS;

            List<SlotDisplay> ingredients = shapeless.ingredients();

            int count = ingredients.size();

            if (count < 1 || count > 9) {
                return Optional.empty();
            }

            if (count == 1) {
                width = 1;
            } else if (count <= 4) {
                width = 2;
            } else {
                width = 3;
            }

            height = (count + width - 1) / width;

            for (int i = 0; i < count; i++) {

                int row = i / width;
                int column = i % width;

                int gridIndex = row * 3 + column;

                grid.set(
                        gridIndex,
                        ingredients.get(i)
                );
            }
        }


        else if (display instanceof FurnaceRecipeDisplay furnace) {

            type = detectCookingType(
                    furnace,
                    context
            );

            width = 1;
            height = 1;

            grid.set(
                    0,
                    furnace.ingredient()
            );

            fuel = furnace.fuel();

            cookingTime = furnace.duration();
            experience = furnace.experience();
        }


        else {
            return Optional.empty();
        }


        boolean hasRequirements =
                entry.craftingRequirements().isPresent();

        List<Ingredient> requirements =
                entry.craftingRequirements().orElseGet(List::of);

        RecipeEntry resolved = new RecipeEntry(
                entry.id(),
                type,
                result,
                width,
                height,
                grid,
                requirements,
                hasRequirements,
                fuel,
                cookingTime,
                experience
        );

        return Optional.of(resolved);
    }


    private static RecipeEntry.RecipeType detectCookingType(
            FurnaceRecipeDisplay display,
            ContextMap context
    ) {

        List<ItemStack> stations =
                display.craftingStation().resolveForStacks(context);

        for (ItemStack station : stations) {

            if (station.isEmpty()) {
                continue;
            }

            if (station.getItem() == Items.BLAST_FURNACE) {

                return RecipeEntry.RecipeType.BLASTING;
            }

            if (station.getItem() == Items.SMOKER) {

                return RecipeEntry.RecipeType.SMOKING;
            }

            if (station.getItem() == Items.CAMPFIRE
                    || station.getItem() == Items.SOUL_CAMPFIRE) {

                return RecipeEntry.RecipeType.CAMPFIRE;
            }
        }

        return RecipeEntry.RecipeType.SMELTING;
    }


    private static List<SlotDisplay> emptyGrid() {

        return new ArrayList<>(
                Collections.nCopies(9, (SlotDisplay) null)
        );
    }
}