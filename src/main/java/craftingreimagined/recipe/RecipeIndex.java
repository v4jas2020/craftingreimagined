package craftingreimagined.recipe;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

import net.minecraft.core.registries.BuiltInRegistries;

import net.minecraft.util.context.ContextMap;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class RecipeIndex {

    private static List<RecipeEntry> recipes = List.of();

    private static Map<RecipeDisplayId, RecipeEntry> byId = Map.of();

    private static Map<Item, List<RecipeEntry>> byResult = Map.of();

    private static ClientLevel cachedLevel = null;

    private static boolean loaded = false;

    private RecipeIndex() {
    }


    public static void reload() {

        Minecraft client = Minecraft.getInstance();

        if (client.level == null || client.player == null) {
            clear();
            return;
        }

        cachedLevel = client.level;

        List<RecipeEntry> loadedRecipes =
                RecipeResolver.getKnownRecipes();

        Map<RecipeDisplayId, RecipeEntry> ids =
                new HashMap<>();

        Map<Item, List<RecipeEntry>> results =
                new HashMap<>();

        for (RecipeEntry recipe : loadedRecipes) {

            ids.put(
                    recipe.getId(),
                    recipe
            );

            Item resultItem = recipe.getResult().getItem();

            results.computeIfAbsent(
                    resultItem,
                    ignored -> new ArrayList<>()
            ).add(recipe);
        }

        recipes = List.copyOf(loadedRecipes);

        byId = Map.copyOf(ids);

        Map<Item, List<RecipeEntry>> finished =
                new HashMap<>();

        for (Map.Entry<Item, List<RecipeEntry>> entry
                : results.entrySet()) {

            finished.put(
                    entry.getKey(),
                    List.copyOf(entry.getValue())
            );
        }

        byResult = Map.copyOf(finished);

        loaded = true;
    }


    public static void clear() {

        recipes = List.of();

        byId = Map.of();

        byResult = Map.of();

        cachedLevel = null;

        loaded = false;
    }


    private static void ensureLoaded() {

        Minecraft client = Minecraft.getInstance();

        if (client.level == null) {

            if (cachedLevel != null) {
                clear();
            }

            return;
        }

        if (!loaded || cachedLevel != client.level) {
            reload();
        }
    }

    public static List<RecipeEntry> getAllRecipes() {

        ensureLoaded();

        return recipes;
    }


    public static Optional<RecipeEntry> getRecipe(
            RecipeDisplayId id
    ) {

        ensureLoaded();

        if (id == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(byId.get(id));
    }


    public static List<RecipeEntry> getRecipesFor(
            Item item
    ) {

        ensureLoaded();

        if (item == null) {
            return List.of();
        }

        return byResult.getOrDefault(
                item,
                List.of()
        );
    }


    public static List<RecipeEntry> getRecipesFor(
            ItemStack stack
    ) {

        if (stack == null || stack.isEmpty()) {
            return List.of();
        }

        return getRecipesFor(stack.getItem());
    }


    public static List<RecipeEntry> getRecipesByType(
            RecipeEntry.RecipeType type
    ) {

        ensureLoaded();

        if (type == null) {
            return List.of();
        }

        List<RecipeEntry> found = new ArrayList<>();

        for (RecipeEntry recipe : recipes) {

            if (recipe.getType() == type) {
                found.add(recipe);
            }
        }

        return List.copyOf(found);
    }


    public static List<RecipeEntry> search(
            String query
    ) {

        ensureLoaded();

        if (query == null || query.isBlank()) {
            return recipes;
        }

        String searchText = query
                .trim()
                .toLowerCase(Locale.ROOT);

        List<RecipeEntry> found = new ArrayList<>();

        for (RecipeEntry recipe : recipes) {

            ItemStack result = recipe.getResult();

            String displayName = result
                    .getHoverName()
                    .getString()
                    .toLowerCase(Locale.ROOT);

            String registryName = BuiltInRegistries.ITEM
                    .getKey(result.getItem())
                    .toString()
                    .toLowerCase(Locale.ROOT);

            if (displayName.contains(searchText)
                    || registryName.contains(searchText)) {

                found.add(recipe);
            }
        }

        return List.copyOf(found);
    }


    public static List<RecipeEntry> getRecipesUsing(
            Item item
    ) {

        ensureLoaded();

        Minecraft client = Minecraft.getInstance();

        if (item == null || client.level == null) {
            return List.of();
        }

        ContextMap context = SlotDisplayContext.fromLevel(
                client.level
        );

        List<RecipeEntry> found = new ArrayList<>();

        for (RecipeEntry recipe : recipes) {

            boolean usesItem = false;

            for (int slot = 0; slot < 9; slot++) {

                for (ItemStack option :
                        recipe.getIngredientOptions(
                                slot,
                                context
                        )) {

                    if (option.getItem() == item) {
                        usesItem = true;
                        break;
                    }
                }

                if (usesItem) {
                    break;
                }
            }

            if (usesItem) {
                found.add(recipe);
            }
        }

        return List.copyOf(found);
    }


    public static int size() {

        ensureLoaded();

        return recipes.size();
    }

    public static boolean isEmpty() {

        return size() == 0;
    }
}