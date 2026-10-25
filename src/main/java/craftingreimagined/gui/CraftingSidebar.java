package craftingreimagined.gui;

import craftingreimagined.crafting.CraftPlanner;
import craftingreimagined.crafting.CraftExecutor;
import craftingreimagined.crafting.MaterialAvailability;
import craftingreimagined.recipe.RecipeEntry;
import craftingreimagined.recipe.RecipeIndex;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;

import java.util.List;
import java.util.function.Consumer;

public final class CraftingSidebar {

    private final SearchField searchField;
    private final RecipeGrid recipeGrid;
    private final RecipeView recipeView;
    private final AmountSelector amountSelector;
    private final Button filterButton;
    private final boolean craftingTableScreen;
    private int filterMode = 0;
    private final boolean compact;

    private RecipeEntry selectedRecipe;

    private String status = "Select a recipe.";

    private Consumer<RecipeEntry> selectionListener = recipe -> {};

    public CraftingSidebar(
            int x,
            int y,
            int width,
            int height,
            boolean craftingTableScreen,
            boolean compact
    ) {
        this.craftingTableScreen = craftingTableScreen;
        this.compact = compact;
        int leftWidth = width;

        searchField = new SearchField(
                Minecraft.getInstance().font,
                x + 30,
                y + 4,
                leftWidth - 34,
                20
        );
        filterButton = Button.builder(
                Component.literal("F"),
                button -> cycleFilter()
        ).bounds(
                x + 4,
                y + 4,
                22,
                20
        ).build();

        recipeGrid = new RecipeGrid(
                x + 4,
                y + 29,
                leftWidth - 8,
                Math.max(
                        55,
                        height - (craftingTableScreen ? 61 : 37)
                )
        );

        amountSelector = new AmountSelector(
                x + 4,
                y + height - 28,
                leftWidth - 8
        );

        recipeView = new RecipeView(
              x + 4,
                y + 4,
                Math.max(100, width - 8),
                height - 8
        );

        searchField.setSearchListener(
                this::search
        );

        recipeGrid.setSelectionListener(
                this::onRecipeSelected
        );

        amountSelector.setAmountChangedListener(
                this::onAmountChanged
        );

        reloadRecipes();
        updateControls();
    }
    private void refreshRecipes() {
        List<RecipeEntry> recipes = RecipeIndex.search(
                searchField.getQuery()
        );
        Minecraft client = Minecraft.getInstance();

        if (filterMode != 0 && client.level != null) {
            var context = SlotDisplayContext.fromLevel(
                    client.level
            );
            recipes = recipes.stream()
                    .filter(recipe -> {
                        if (!recipe.isCraftingRecipe()) {
                            return false;
                        }
                        boolean craftable =
                                MaterialAvailability.hasAllMaterials(
                                        recipe,
                                        recipe.getResultCount(),
                                        context
                                );
                        return filterMode == 1
                                ? craftable
                                : !craftable;
                    })
                    .toList();
        }
        recipeGrid.setRecipes(recipes);
    }
    private void cycleFilter() {
        filterMode = (filterMode + 1) % 3;

        refreshRecipes();
        updateControls();
    }

    public void registerWidgets(
            Consumer<AbstractWidget> register
    ) {
        register.accept(filterButton);
        register.accept(searchField);
        register.accept(recipeGrid);

        if (craftingTableScreen) {
            amountSelector.registerWidgets(register);
        }
    }

    public void reloadRecipes() {

        RecipeIndex.reload();

        search(searchField.getQuery());
    }

    public void search(String query) {
        refreshRecipes();
        selectedRecipe = null;
        recipeView.setRecipe(null);
        amountSelector.setRecipe(null);
        status = "Select a recipe.";
        updateControls();
    }

    private void onRecipeSelected(
            RecipeEntry recipe
    ) {

        selectedRecipe = recipe;

        recipeView.setRecipe(recipe);
        amountSelector.setRecipe(recipe);

        if (recipe == null) {

            status = "Select a recipe.";

        } else {

            status = "Selected: "
                    + recipe.getResult()
                    .getHoverName()
                    .getString();
        }

        updateControls();
        if (recipe != null && recipe.isCraftingRecipe()) {
            craftSelectedRecipe();
        }
        selectionListener.accept(recipe);
    }

    private void onAmountChanged(
            int amount
    ) {

        recipeView.setDesiredOutput(
                Math.max(1, amount)
        );

        updateControls();
    }

    public void setSelectionListener(
            Consumer<RecipeEntry> listener
    ) {

        selectionListener = listener == null
                ? recipe -> {}
                : listener;
    }

    public RecipeEntry getSelectedRecipe() {

        return selectedRecipe;
    }

    public void selectRecipe(
            RecipeEntry recipe
    ) {

        if (recipe == null) {

            recipeGrid.setSelectedRecipe(null);

            onRecipeSelected(null);

            return;
        }

        recipeGrid.setSelectedRecipe(recipe);
    }

    public int getDesiredOutput() {

        return amountSelector.getDesiredOutput();
    }

    public int getRequiredCrafts() {

        return amountSelector.getRequiredCrafts();
    }

    public void setDesiredOutput(
            int amount
    ) {

        amountSelector.setDesiredOutput(amount);
    }

    public void increaseAmount() {

        amountSelector.increase();
    }

    public void decreaseAmount() {

        amountSelector.decrease();
    }

    public void resetAmount() {

        amountSelector.reset();
    }

    public void setMaximumAmount() {

        if (selectedRecipe == null) {
            return;
        }

        int maximum = CraftPlanner.getMaximumCrafts(
                selectedRecipe
        );

        if (maximum <= 0) {

            status = "No available crafts.";

            updateControls();

            return;
        }

        amountSelector.setMaximum();

        status = "Maximum: " + maximum + " crafts.";

        updateControls();
    }

    public boolean craftSelectedRecipe() {

        if (selectedRecipe == null) {
            status = "Select a recipe first.";
            updateControls();
            return false;
        }
        boolean success = CraftExecutor.placeRecipe(
                selectedRecipe,
                false
        );
        status = success
                ? "Recipe placement requested."
                : CraftExecutor.getLastError();
        updateControls();
        return success;
    }
    public boolean fillMaximum() {
        if (selectedRecipe == null) {
            return false;
        }

        boolean success = CraftExecutor.placeRecipe(
                selectedRecipe,
                true
        );
        status = success
               ? "Maximum placement requested."
                : CraftExecutor.getLastError();
        updateControls();

        return success;
    }
    public boolean handleAmountScroll(
            double mouseX,
            double mouseY,
            double scrollY
    ) {
        return craftingTableScreen
                && amountSelector.handleScroll(
                        mouseX,
                        mouseY,
                        scrollY
        );
    }

    public void updateControls() {
        String mode = switch (filterMode) {
            case 1 -> "Craftable";
            case 2 -> "Missing";
            default -> "All";
        };
        filterButton.setTooltip(
                Tooltip.create(
                        Component.literal("Filter:" + mode)
                )
        );
    }

    public void clearSelection() {

        selectRecipe(null);
    }

    public String getStatus() {

        return status;
    }

    public SearchField getSearchField() {

        return searchField;
    }

    public RecipeGrid getRecipeGrid() {

        return recipeGrid;
    }

    public RecipeView getRecipeView() {

        return recipeView;
    }

    public AmountSelector getAmountSelector() {

        return amountSelector;
    }

    public List<RecipeEntry> getVisibleRecipes() {

        return recipeGrid.getRecipes();
    }

    public int getRecipeCount() {

        return recipeGrid.getRecipeCount();
    }
}