package craftingreimagined.gui;

import craftingreimagined.crafting.CraftPlanner;
import craftingreimagined.recipe.RecipeEntry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

public final class AmountSelector {

    private final int x;
    private final int y;
    private final int width;

    private final Button decreaseButton;
    private final Button increaseButton;
    private final Button maxButton;

    private final EditBox amountField;

    private RecipeEntry recipe;
    private int desiredOutput = 1;

    private boolean changingText = false;

    private IntConsumer amountChanged = amount -> {};

    public AmountSelector(
            int x,
            int y,
            int width
    ) {
        this.x = x;
        this.y = y;
        this.width = width;

        decreaseButton = Button.builder(
                Component.literal("-"),
                button -> decrease()
        ).bounds(
                x, y, 22, 20
        ).build();

        increaseButton = Button.builder(
                Component.literal("+"),
                button -> increase()
        ).bounds(
                x + width - 74,
                y,
                22,
                20
        ).build();

        maxButton = Button.builder(
                Component.literal("MAX"),
                button -> setMaximum()
        ).bounds(
                x + width - 48,
                y,
                48,
                20
        ).build();

        amountField = new EditBox(
                Minecraft.getInstance().font,
                x + 26,
                y,
                width - 104,
                20,
                Component.literal("Amount")
        );

        amountField.setMaxLength(8);

        amountField.setResponder(
                this::onTextChanged
        );

        updateControls();
    }

    public void setRecipe(RecipeEntry recipe) {
        this.recipe = recipe;

        if (recipe == null) {
            desiredOutput = 1;
        } else {
            desiredOutput = recipe.getResultCount();
        }

        updateControls();
        notifyAmountChanged();
    }

    public RecipeEntry getRecipe() {
        return recipe;
    }

    public int getDesiredOutput() {
        return recipe == null ? 0 : desiredOutput;
    }

    public int getRequiredCrafts() {
        if (recipe == null) {
            return 0;
        }

        return recipe.getCraftsForOutput(desiredOutput);
    }

    public void setDesiredOutput(int requestedAmount) {
        if (recipe == null) {
            return;
        }

        int perCraft = recipe.getResultCount();

        int crafts = Math.max(
                1,
                Math.min(
                        CraftPlanner.MAX_CRAFTS,
                        recipe.getCraftsForOutput(requestedAmount)
                )
        );

        desiredOutput = crafts * perCraft;

        updateControls();
        notifyAmountChanged();
    }

    public void increase() {
        if (recipe == null) {
            return;
        }

        setDesiredOutput(
                desiredOutput + recipe.getResultCount()
        );
    }

    public void decrease() {
        if (recipe == null) {
            return;
        }

        setDesiredOutput(
                desiredOutput - recipe.getResultCount()
        );
    }

    public void reset() {
        if (recipe != null) {
            setDesiredOutput(recipe.getResultCount());
        }
    }

    public void setMaximum() {
        if (recipe == null || !recipe.isCraftingRecipe()) {
            return;
        }

        int maximum = CraftPlanner.getMaximumCrafts(recipe);

        if (maximum > 0) {
            setDesiredOutput(
                    recipe.getOutputCount(maximum)
            );
        }
    }

    public void setAmountChangedListener(
            IntConsumer listener
    ) {
        amountChanged = listener == null
                ? amount -> {}
                : listener;
    }

    public boolean handleScroll(
            double mouseX,
            double mouseY,
            double scrollY
    ) {
        if (recipe == null || scrollY == 0) {
            return false;
        }

        if (mouseX < x || mouseX >= x + width
                || mouseY < y || mouseY >= y + 20) {
            return false;
        }

        if (scrollY > 0) {
            increase();
        } else {
            decrease();
        }

        return true;
    }

    public void registerWidgets(
            Consumer<AbstractWidget> register
    ) {
        register.accept(decreaseButton);
        register.accept(amountField);
        register.accept(increaseButton);
        register.accept(maxButton);
    }

    public void setVisible(boolean visible) {
        decreaseButton.visible = visible;
        increaseButton.visible = visible;
        maxButton.visible = visible;
        amountField.visible = visible;
    }

    private void onTextChanged(String text) {
        if (changingText || recipe == null || text.isBlank()) {
            return;
        }

        try {
            int requested = Integer.parseInt(text);

            if (requested < 1) {
                return;
            }

            int crafts = Math.max(
                    1,
                    Math.min(
                            CraftPlanner.MAX_CRAFTS,
                            recipe.getCraftsForOutput(requested)
                    )
            );

            desiredOutput = recipe.getOutputCount(crafts);

            updateButtonStates();
            notifyAmountChanged();

        } catch (NumberFormatException ignored) {
        }
    }

    private void updateControls() {
        changingText = true;

        amountField.setValue(
                recipe == null
                        ? "0"
                        : String.valueOf(desiredOutput)
        );

        changingText = false;

        updateButtonStates();
    }

    private void updateButtonStates() {
        boolean selected = recipe != null;

        amountField.active = selected;

        decreaseButton.active = selected
                && getRequiredCrafts() > 1;

        increaseButton.active = selected
                && getRequiredCrafts() < CraftPlanner.MAX_CRAFTS;

        maxButton.active = selected
                && recipe.isCraftingRecipe();
    }

    private void notifyAmountChanged() {
        amountChanged.accept(getDesiredOutput());
    }
}