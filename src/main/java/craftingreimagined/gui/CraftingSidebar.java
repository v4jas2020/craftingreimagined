package craftingreimagined.gui;

import craftingreimagined.crafting.CraftPlanner;
import craftingreimagined.recipe.RecipeEntry;
import craftingreimagined.recipe.RecipeIndex;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;

public final class CraftingSidebar {

    private final SearchField searchField;
    private final RecipeGrid recipeGrid;
    private final RecipeView recipeView;
    private final AmountSelector amountSelector;
    private final Button craftButton;

    private RecipeEntry selectedRecipe;

    private String status = "Select a recipe.";

    private Consumer<RecipeEntry> selectionListener = recipe -> {};

    public CraftingSidebar(
            int x,
            int y,
            int width,
            int height
    ) {

        int leftWidth = Math.min(
                205,
                Math.max(145, width * 2 / 5)
        );

        int rightX = x + leftWidth + 8;
        int rightWidth = width - leftWidth - 8;

        searchField = new SearchField(
                Minecraft.getInstance().font,
                x + 4,
                y + 4,
                leftWidth - 8,
                20
        );

        recipeGrid = new RecipeGrid(
                x + 4,
                y + 29,
                leftWidth - 8,
                Math.max(55, height - 99)
        );

        amountSelector = new AmountSelector(
                x + 4,
                y + height - 66,
                leftWidth - 8
        );

        craftButton = Button.builder(
                Component.literal("Craft"),
                button -> craftSelectedRecipe()
        ).bounds(
                x + 4,
                y + height - 40,
                leftWidth - 8,
                20
        ).build();

        recipeView = new RecipeView(
                rightX,
                y + 4,
                rightWidth,
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

    public void registerWidgets(
            Consumer<AbstractWidget> register
    ) {

        register.accept(searchField);
        register.accept(recipeGrid);

        amountSelector.registerWidgets(register);

        register.accept(recipeView);
        register.accept(craftButton);
    }

    public void reloadRecipes() {

        RecipeIndex.reload();

        search(searchField.getQuery());
    }

    public void search(String query) {

        recipeGrid.setRecipes(
                RecipeIndex.search(query)
        );

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

        if (!selectedRecipe.isCraftingRecipe()) {

            status = "Automatic crafting is not supported "
                    + "for this recipe.";

            updateControls();

            return false;
        }

        CraftPlanner.Plan plan = CraftPlanner.planForOutput(
                selectedRecipe,
                getDesiredOutput()
        );

        if (!plan.possible()) {

            status = plan.message();

            updateControls();

            return false;
        }

        boolean queued = CraftPlanner.execute(plan);

        if (queued) {

            status = "Queued "
                    + plan.requestedCrafts()
                    + " crafting operations.";

        } else {

            status = "Unable to queue crafting.";
        }

        updateControls();

        return queued;
    }

    public boolean handleAmountScroll(
            double mouseX,
            double mouseY,
            double scrollY
    ) {

        return amountSelector.handleScroll(
                mouseX,
                mouseY,
                scrollY
        );
    }

    public void updateControls() {

        craftButton.active = selectedRecipe != null
                && selectedRecipe.isCraftingRecipe();

        craftButton.setTooltip(
                Tooltip.create(
                        Component.literal(status)
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