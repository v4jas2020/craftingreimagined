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

    private static final int PADDING = 4;
    private static final int BUTTON_HEIGHT = 20;

    private final SearchField searchField;
    private final RecipeGrid recipeGrid;

    private final Button decreaseButton;
    private final Button increaseButton;
    private final Button amountButton;
    private final Button maxButton;
    private final Button craftButton;

    private RecipeEntry selectedRecipe;

    private int desiredOutput = 1;

    private String status = "Select a recipe.";

    private Consumer<RecipeEntry> selectionListener = recipe -> {};

    public CraftingSidebar(
            int x,
            int y,
            int width,
            int height
    ) {

        if (width < 160 || height < 120) {
            throw new IllegalArgumentException(
                    "Crafting sidebar must be at least 160x120."
            );
        }

        Minecraft client = Minecraft.getInstance();

        int contentWidth = width - PADDING * 2;

        // paieskos laukas
        searchField = new SearchField(
                client.font,
                x + PADDING,
                y + PADDING,
                contentWidth,
                20
        );

        // Recipe grid
        recipeGrid = new RecipeGrid(
                x + PADDING,
                y + 28,
                contentWidth,
                height - 82
        );


        int controlsY = y + height - 50;

        decreaseButton = Button.builder(
                Component.literal("-"),
                button -> decreaseAmount()
        ).bounds(
                x + PADDING,
                controlsY,
                22,
                BUTTON_HEIGHT
        ).build();

        amountButton = Button.builder(
                Component.literal("1"),
                button -> resetAmount()
        ).bounds(
                x + 28,
                controlsY,
                54,
                BUTTON_HEIGHT
        ).build();

        increaseButton = Button.builder(
                Component.literal("+"),
                button -> increaseAmount()
        ).bounds(
                x + 86,
                controlsY,
                22,
                BUTTON_HEIGHT
        ).build();

        maxButton = Button.builder(
                Component.literal("MAX"),
                button -> setMaximumAmount()
        ).bounds(
                x + 112,
                controlsY,
                width - 116,
                BUTTON_HEIGHT
        ).build();

        // Craft Mygtukas
        craftButton = Button.builder(
                Component.literal("Craft"),
                button -> craftSelectedRecipe()
        ).bounds(
                x + PADDING,
                y + height - 26,
                contentWidth,
                BUTTON_HEIGHT
        ).build();


        searchField.setSearchListener(
                this::search
        );


        recipeGrid.setSelectionListener(
                this::onRecipeSelected
        );

        // Receptus uzkrauna
        reloadRecipes();

        updateControls();
    }


    public void registerWidgets(
            Consumer<AbstractWidget> register
    ) {

        register.accept(searchField);
        register.accept(recipeGrid);

        register.accept(decreaseButton);
        register.accept(amountButton);
        register.accept(increaseButton);
        register.accept(maxButton);
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
        desiredOutput = 1;

        status = "Select a recipe.";

        updateControls();
    }


    private void onRecipeSelected(
            RecipeEntry recipe
    ) {

        selectedRecipe = recipe;

        if (recipe == null) {
            desiredOutput = 1;
            status = "Select a recipe.";
        } else {
            desiredOutput = recipe.getResultCount();
            status = "Selected: "
                    + recipe.getResult().getHoverName().getString();
        }

        updateControls();

        selectionListener.accept(recipe);
    }


    public void setSelectionListener(
            Consumer<RecipeEntry> listener
    ) {

        selectionListener = listener != null
                ? listener
                : recipe -> {};
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

        return desiredOutput;
    }


    public int getRequiredCrafts() {

        if (selectedRecipe == null) {
            return 0;
        }

        return selectedRecipe.getCraftsForOutput(
                desiredOutput
        );
    }


    public void setDesiredOutput(
            int amount
    ) {

        if (selectedRecipe == null) {
            return;
        }

        int maxOutput = Math.multiplyExact(
                CraftPlanner.MAX_CRAFTS,
                selectedRecipe.getResultCount()
        );

        desiredOutput = Math.max(
                1,
                Math.min(amount, maxOutput)
        );

        updateControls();
    }


    public void increaseAmount() {

        if (selectedRecipe == null) {
            return;
        }

        int step = selectedRecipe.getResultCount();

        setDesiredOutput(
                desiredOutput + step
        );
    }


    public void decreaseAmount() {

        if (selectedRecipe == null) {
            return;
        }

        int step = selectedRecipe.getResultCount();

        setDesiredOutput(
                Math.max(step, desiredOutput - step)
        );
    }


    public void resetAmount() {

        if (selectedRecipe == null) {
            return;
        }

        setDesiredOutput(
                selectedRecipe.getResultCount()
        );
    }


    public void setMaximumAmount() {

        if (selectedRecipe == null) {
            return;
        }

        int maximumCrafts = CraftPlanner.getMaximumCrafts(
                selectedRecipe
        );

        if (maximumCrafts <= 0) {

            status = "No available crafts. Open a crafting table "
                    + "and check your ingredients.";

            updateControls();
            return;
        }

        setDesiredOutput(
                selectedRecipe.getOutputCount(maximumCrafts)
        );

        status = "Maximum: " + maximumCrafts + " crafts.";

        updateControls();
    }


    public boolean craftSelectedRecipe() {

        if (selectedRecipe == null) {

            status = "Select a recipe first.";
            updateControls();

            return false;
        }

        CraftPlanner.Plan plan = CraftPlanner.planForOutput(
                selectedRecipe,
                desiredOutput
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

            status = "Could not queue crafting. "
                    + "Check the crafting table.";
        }

        updateControls();

        return queued;
    }


    public void updateControls() {

        boolean selected = selectedRecipe != null;

        decreaseButton.active = selected
                && desiredOutput > selectedRecipe.getResultCount();

        increaseButton.active = selected
                && getRequiredCrafts() < CraftPlanner.MAX_CRAFTS;

        amountButton.active = selected;

        maxButton.active = selected
                && selectedRecipe.isCraftingRecipe();

        craftButton.active = selected
                && selectedRecipe.isCraftingRecipe();

        if (selected) {

            int actualOutput = selectedRecipe.getOutputCount(
                    getRequiredCrafts()
            );

            amountButton.setMessage(
                    Component.literal(
                            String.valueOf(actualOutput)
                    )
            );

        } else {

            amountButton.setMessage(
                    Component.literal("0")
            );
        }

        craftButton.setTooltip(
                Tooltip.create(
                        Component.literal(status)
                )
        );
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


    public List<RecipeEntry> getVisibleRecipes() {

        return recipeGrid.getRecipes();
    }


    public int getRecipeCount() {

        return recipeGrid.getRecipeCount();
    }

    /**
     * isvalo viska.
     */
    public void clearSelection() {

        selectRecipe(null);
    }
}