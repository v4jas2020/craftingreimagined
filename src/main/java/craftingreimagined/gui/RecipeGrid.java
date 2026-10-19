package craftingreimagined.gui;

import craftingreimagined.recipe.RecipeEntry;
import craftingreimagined.recipe.RecipeIndex;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Consumer;

public final class RecipeGrid extends AbstractWidget {

    private static final int CELL_SIZE = 24;
    private static final int PADDING = 4;
    private static final int SCROLLBAR_WIDTH = 8;

    private static final int BACKGROUND_COLOR = 0xD0101010;
    private static final int BORDER_COLOR = 0xFF555555;
    private static final int SLOT_COLOR = 0xFF303030;
    private static final int HOVER_COLOR = 0xFF505050;
    private static final int SELECTED_COLOR = 0xFF77AAFF;

    private List<RecipeEntry> recipes = List.of();

    private RecipeEntry selectedRecipe;

    private Consumer<RecipeEntry> selectionListener = recipe -> {};

    private int scrollRow = 0;

    public RecipeGrid(
            int x,
            int y,
            int width,
            int height
    ) {
        super(
                x,
                y,
                width,
                height,
                Component.literal("Recipe Grid")
        );
    }


    public void setRecipes(List<RecipeEntry> newRecipes) {

        this.recipes = newRecipes == null
                ? List.of()
                : List.copyOf(newRecipes);

        this.scrollRow = 0;
        this.selectedRecipe = null;
    }


    public void loadAllRecipes() {

        setRecipes(RecipeIndex.getAllRecipes());
    }


    public void search(String query) {

        setRecipes(RecipeIndex.search(query));
    }


    public List<RecipeEntry> getRecipes() {

        return recipes;
    }


    public int getRecipeCount() {

        return recipes.size();
    }

    public RecipeEntry getSelectedRecipe() {

        return selectedRecipe;
    }


    public void setSelectedRecipe(RecipeEntry recipe) {

        if (recipe != null && !recipes.contains(recipe)) {
            return;
        }

        selectedRecipe = recipe;

        if (recipe != null) {
            selectionListener.accept(recipe);
        }
    }


    public void setSelectionListener(
            Consumer<RecipeEntry> listener
    ) {

        selectionListener = listener != null
                ? listener
                : recipe -> {};
    }


    public int getColumns() {

        int availableWidth = getWidth()
                - PADDING * 2
                - SCROLLBAR_WIDTH;

        return Math.max(
                1,
                availableWidth / CELL_SIZE
        );
    }


    public int getVisibleRows() {

        int availableHeight = getHeight() - PADDING * 2;

        return Math.max(
                1,
                availableHeight / CELL_SIZE
        );
    }


    public int getTotalRows() {

        int columns = getColumns();

        return (recipes.size() + columns - 1) / columns;
    }


    public int getMaxScrollRow() {

        return Math.max(
                0,
                getTotalRows() - getVisibleRows()
        );
    }


    public void setScrollRow(int row) {

        scrollRow = Math.max(
                0,
                Math.min(row, getMaxScrollRow())
        );
    }

    public int getScrollRow() {

        return scrollRow;
    }

    public void scroll(int rows) {

        setScrollRow(scrollRow + rows);
    }


    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double scrollX,
            double scrollY
    ) {

        if (!isMouseOver(mouseX, mouseY)) {
            return false;
        }

        if (scrollY == 0 || getMaxScrollRow() == 0) {
            return false;
        }

        int amount = Math.max(
                1,
                (int) Math.ceil(Math.abs(scrollY))
        );

        if (scrollY > 0) {
            scroll(-amount);
        } else {
            scroll(amount);
        }

        return true;
    }


    public RecipeEntry getRecipeAt(
            double mouseX,
            double mouseY
    ) {

        if (!isMouseOver(mouseX, mouseY)) {
            return null;
        }

        double relativeX = mouseX - getX() - PADDING;
        double relativeY = mouseY - getY() - PADDING;

        if (relativeX < 0 || relativeY < 0) {
            return null;
        }

        int column = (int) (relativeX / CELL_SIZE);
        int row = (int) (relativeY / CELL_SIZE);

        if (column >= getColumns()
                || row >= getVisibleRows()) {

            return null;
        }

        int index = (scrollRow + row)
                * getColumns()
                + column;

        if (index < 0 || index >= recipes.size()) {
            return null;
        }

        return recipes.get(index);
    }


    @Override
    public void onClick(
            MouseButtonEvent event,
            boolean doubleClick
    ) {

        if (event.button() != 0) {
            return;
        }

        RecipeEntry clicked = getRecipeAt(
                event.x(),
                event.y()
        );

        if (clicked != null) {
            setSelectedRecipe(clicked);
        }
    }

    /* renderina */

    @Override
    protected void extractWidgetRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float delta
    ) {

        int x = getX();
        int y = getY();

        int width = getWidth();
        int height = getHeight();

        // Background
        graphics.fill(
                x,
                y,
                x + width,
                y + height,
                BACKGROUND_COLOR
        );

        // Border
        graphics.outline(
                x,
                y,
                width,
                height,
                BORDER_COLOR
        );

        int columns = getColumns();
        int visibleRows = getVisibleRows();

        int contentX = x + PADDING;
        int contentY = y + PADDING;

        // Keep icons inside the grid.
        graphics.enableScissor(
                contentX,
                contentY,
                x + width - SCROLLBAR_WIDTH,
                y + height - PADDING
        );

        for (int row = 0; row < visibleRows; row++) {

            for (int column = 0; column < columns; column++) {

                int index = (scrollRow + row)
                        * columns
                        + column;

                if (index >= recipes.size()) {
                    break;
                }

                RecipeEntry recipe = recipes.get(index);

                int cellX = contentX + column * CELL_SIZE;
                int cellY = contentY + row * CELL_SIZE;

                renderRecipeCell(
                        graphics,
                        recipe,
                        cellX,
                        cellY,
                        mouseX,
                        mouseY
                );
            }
        }

        graphics.disableScissor();

        renderScrollbar(graphics);

        // Update the tooltip for the hovered recipe.
        RecipeEntry hovered = getRecipeAt(mouseX, mouseY);

        if (hovered != null) {

            ItemStack result = hovered.getResult();

            setTooltip(
                    Tooltip.create(result.getHoverName())
            );

        } else {

            setTooltip(null);
        }
    }


    private void renderRecipeCell(
            GuiGraphicsExtractor graphics,
            RecipeEntry recipe,
            int x,
            int y,
            int mouseX,
            int mouseY
    ) {

        int cellWidth = CELL_SIZE - 2;
        int cellHeight = CELL_SIZE - 2;

        boolean hovered = mouseX >= x
                && mouseX < x + cellWidth
                && mouseY >= y
                && mouseY < y + cellHeight;

        boolean selected = selectedRecipe == recipe;

        int color = hovered
                ? HOVER_COLOR
                : SLOT_COLOR;

        graphics.fill(
                x,
                y,
                x + cellWidth,
                y + cellHeight,
                color
        );

        if (selected) {

            graphics.outline(
                    x,
                    y,
                    cellWidth,
                    cellHeight,
                    SELECTED_COLOR
            );
        }

        ItemStack result = recipe.getResult();

        int itemX = x + 3;
        int itemY = y + 3;

        graphics.item(
                result,
                itemX,
                itemY
        );

        graphics.itemDecorations(
                Minecraft.getInstance().font,
                result,
                itemX,
                itemY
        );
    }


    private void renderScrollbar(
            GuiGraphicsExtractor graphics
    ) {

        int maximum = getMaxScrollRow();

        if (maximum <= 0) {
            return;
        }

        int barX = getX() + getWidth() - 6;
        int barY = getY() + PADDING;

        int barHeight = Math.max(
                1,
                getHeight() - PADDING * 2
        );

        graphics.fill(
                barX,
                barY,
                barX + 3,
                barY + barHeight,
                0xFF202020
        );

        int thumbHeight = Math.max(
                12,
                barHeight * getVisibleRows()
                        / Math.max(1, getTotalRows())
        );

        thumbHeight = Math.min(
                barHeight,
                thumbHeight
        );

        int travel = barHeight - thumbHeight;

        int thumbY = barY
                + travel * scrollRow / maximum;

        graphics.fill(
                barX,
                thumbY,
                barX + 3,
                thumbY + thumbHeight,
                SELECTED_COLOR
        );
    }

    /**
     * Naration supportas ka zinau
     */
    @Override
    protected void updateWidgetNarration(
            NarrationElementOutput output
    ) {

        defaultButtonNarrationText(output);
    }
}