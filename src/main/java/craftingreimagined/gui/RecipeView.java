package craftingreimagined.gui;

import craftingreimagined.crafting.MaterialAvailability;
import craftingreimagined.recipe.RecipeEntry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;

import java.util.List;

public final class RecipeView extends AbstractWidget {

    private static final int BACKGROUND = 0xE0202020;
    private static final int BORDER = 0xFF777777;
    private static final int SLOT_BACKGROUND = 0xFF404040;
    private static final int SLOT_BORDER = 0xFF888888;
    private static final int MISSING_BACKGROUND = 0xFF973535;
    private static final int MISSING_BORDER = 0xFFFF5555;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED_TEXT = 0xFFAAAAAA;
    private static final int RED_TEXT = 0xFFFF7777;
    private static final int GREEN_TEXT = 0xFF77DD77;

    private static final int SLOT_SIZE = 22;

    private RecipeEntry recipe;
    private int desiredOutput = 1;

    public RecipeView(int x, int y, int width, int height) {
        super(
                x,
                y,
                width,
                height,
                Component.literal("Recipe View")
        );
    }

    public void setRecipe(RecipeEntry recipe) {
        this.recipe = recipe;

        desiredOutput = recipe == null
                ? 1
                : recipe.getResultCount();
    }

    public RecipeEntry getRecipe() {
        return recipe;
    }

    public void setDesiredOutput(int amount) {
        desiredOutput = Math.max(1, amount);
    }

    public int getDesiredOutput() {
        return desiredOutput;
    }

    @Override
    protected void extractWidgetRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float delta
    ) {
        int x = getX();
        int y = getY();

        Font font = Minecraft.getInstance().font;

        graphics.fill(
                x,
                y,
                x + getWidth(),
                y + getHeight(),
                BACKGROUND
        );

        graphics.outline(
                x,
                y,
                getWidth(),
                getHeight(),
                BORDER
        );

        graphics.enableScissor(
                x + 1,
                y + 1,
                x + getWidth() - 1,
                y + getHeight() - 1
        );

        setTooltip(null);

        if (recipe == null) {
            graphics.text(
                    font,
                    "Select a recipe",
                    x + 12,
                    y + 15,
                    TEXT,
                    true
            );

            graphics.text(
                    font,
                    "Choose an item from the grid.",
                    x + 12,
                    y + 32,
                    MUTED_TEXT,
                    false
            );

            graphics.disableScissor();
            return;
        }

        Minecraft client = Minecraft.getInstance();

        ContextMap context = client.level == null
                ? null
                : SlotDisplayContext.fromLevel(client.level);

        int crafts = recipe.getCraftsForOutput(
                desiredOutput
        );

        int actualOutput = recipe.getOutputCount(
                crafts
        );

        int[] missing = context == null
                ? new int[9]
                : MaterialAvailability.getMissingPerSlot(
                recipe,
                desiredOutput,
                context
        );

        int totalMissing = MaterialAvailability.getTotalMissing(
                missing
        );

        ItemStack result = recipe.getResult();

        graphics.text(
                font,
                result.getHoverName(),
                x + 12,
                y + 12,
                TEXT,
                true
        );

        graphics.text(
                font,
                recipe.getType().name(),
                x + 12,
                y + 27,
                MUTED_TEXT,
                false
        );

        int gridX = x + 12;
        int gridY = y + 47;

        ItemStack hoveredStack = ItemStack.EMPTY;
        int hoveredMissing = 0;

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {

                int slot = row * 3 + column;

                int slotX = gridX + column * SLOT_SIZE;
                int slotY = gridY + row * SLOT_SIZE;

                boolean hasMissing = missing[slot] > 0;

                graphics.fill(
                        slotX,
                        slotY,
                        slotX + SLOT_SIZE - 1,
                        slotY + SLOT_SIZE - 1,
                        hasMissing
                                ? MISSING_BACKGROUND
                                : SLOT_BACKGROUND
                );

                graphics.outline(
                        slotX,
                        slotY,
                        SLOT_SIZE - 1,
                        SLOT_SIZE - 1,
                        hasMissing
                                ? MISSING_BORDER
                                : SLOT_BORDER
                );

                if (context == null
                        || recipe.isGridSlotEmpty(slot)) {
                    continue;
                }

                List<ItemStack> options =
                        recipe.getIngredientOptions(
                                slot,
                                context
                        );

                if (options.isEmpty()) {
                    continue;
                }

                ItemStack ingredient = options.getFirst();

                graphics.item(
                        ingredient,
                        slotX + 3,
                        slotY + 3
                );

                boolean hovered =
                        mouseX >= slotX
                                && mouseX < slotX + SLOT_SIZE
                                && mouseY >= slotY
                                && mouseY < slotY + SLOT_SIZE;

                if (hovered) {
                    hoveredStack = ingredient;
                    hoveredMissing = missing[slot];
                }
            }
        }

        int outputX = gridX + 108;
        int outputY = gridY + 22;

        graphics.text(
                font,
                "->",
                gridX + 79,
                outputY + 5,
                TEXT,
                true
        );

        graphics.fill(
                outputX,
                outputY,
                outputX + SLOT_SIZE,
                outputY + SLOT_SIZE,
                SLOT_BACKGROUND
        );

        graphics.outline(
                outputX,
                outputY,
                SLOT_SIZE,
                SLOT_SIZE,
                BORDER
        );

        graphics.item(
                result,
                outputX + 3,
                outputY + 3
        );

        graphics.itemDecorations(
                font,
                result,
                outputX + 3,
                outputY + 3
        );

        if (mouseX >= outputX
                && mouseX < outputX + SLOT_SIZE
                && mouseY >= outputY
                && mouseY < outputY + SLOT_SIZE) {

            hoveredStack = result;
            hoveredMissing = 0;
        }

        int infoY = gridY + 84;

        graphics.text(
                font,
                "Crafting Plan",
                x + 12,
                infoY,
                TEXT,
                true
        );

        graphics.text(
                font,
                "Output: " + actualOutput,
                x + 12,
                infoY + 17,
                TEXT,
                false
        );

        graphics.text(
                font,
                "Crafts needed: " + crafts,
                x + 12,
                infoY + 31,
                MUTED_TEXT,
                false
        );

        graphics.text(
                font,
                "Ingredients required: "
                        + recipe.getIngredientCountForCrafts(crafts),
                x + 12,
                infoY + 45,
                MUTED_TEXT,
                false
        );

        if (context != null && recipe.isCraftingRecipe()) {

            if (totalMissing > 0) {
                graphics.text(
                        font,
                        "Missing materials: " + totalMissing,
                        x + 12,
                        infoY + 62,
                        RED_TEXT,
                        true
                );
            } else {
                graphics.text(
                        font,
                        "All materials available",
                        x + 12,
                        infoY + 62,
                        GREEN_TEXT,
                        true
                );
            }
        }

        if (recipe.isCookingRecipe()) {
            graphics.text(
                    font,
                    "Cooking time: " + recipe.getCookingTime(),
                    x + 12,
                    infoY + 62,
                    MUTED_TEXT,
                    false
            );
        }

        graphics.disableScissor();

        if (!hoveredStack.isEmpty()) {

            String tooltipText = hoveredStack
                    .getHoverName()
                    .getString();

            if (hoveredMissing > 0) {
                tooltipText += " | Missing: " + hoveredMissing;
            }

            setTooltip(
                    Tooltip.create(
                            Component.literal(tooltipText)
                    )
            );
        }
    }

    @Override
    protected void updateWidgetNarration(
            NarrationElementOutput output
    ) {
        defaultButtonNarrationText(output);
    }
}

