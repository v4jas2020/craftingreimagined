package craftingreimagined.gui;

import craftingreimagined.recipe.RecipeEntry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.FurnaceScreen;
import net.minecraft.client.gui.screens.inventory.BlastFurnaceScreen;
import net.minecraft.client.gui.screens.inventory.SmokerScreen;
import net.minecraft.client.gui.screens.inventory.BrewingStandScreen;

import java.util.ArrayList;
import java.util.List;

public final class  CraftingScreenIntegration {

    private static final int VANILLA_MENU_WIDTH = 176;
    private static final int SIDEBAR_WIDTH = 330;
    private static final int MIN_SIDEBAR_WIDTH = 210;
    private static final int SIDEBAR_HEIGHT = 310;

    private static boolean initialized = false;

    private CraftingScreenIntegration() {
    }

    public static void initialize() {

        if (initialized) {
            return;
        }

        initialized = true;

        ScreenEvents.AFTER_INIT.register(
                (client, screen, screenWidth, screenHeight) -> {

                    if (!(screen instanceof CraftingScreen)
                            && !(screen instanceof InventoryScreen)
                            && !(screen instanceof FurnaceScreen)
                            && !(screen instanceof BlastFurnaceScreen)
                            && !(screen instanceof SmokerScreen)) {
                        return;
                    }

                    int menuLeft = (screenWidth - VANILLA_MENU_WIDTH) / 2;
                    int menuRight = menuLeft + VANILLA_MENU_WIDTH;
                    int margin = 4;
                    int availableRight = screenWidth - menuRight - margin * 2;
                    int availableLeft = menuLeft - margin * 2;
                    boolean useRight = availableRight > availableLeft;
                    int available = Math.max(availableRight, availableLeft);
                    int width = Math.min(160, available);
                    int height = screenHeight - 8;

                    if (width < 65 || height < 120) {
                        return;
                    }

                    int x = useRight
                            ? screenWidth - width - margin
                            : margin;
                    int y = 4;

                    boolean compact = width < 290;
                    int vanillaMenuHeight = 166;
                    int menuTop = (screenHeight - vanillaMenuHeight) / 2;
                    int previewX = menuLeft;
                    int previewY = menuTop + vanillaMenuHeight + 4;
                    int previewWidth = VANILLA_MENU_WIDTH;
                    int previewHeight = Math.max(
                            1,
                            Math.min(
                                    118,
                                    screenHeight - previewY -4
                            )
                    );
                    craftingreimagined.recipe.RecipeEntry.RecipeType stationType = null;
                    if (screen instanceof FurnaceScreen) {
                        stationType = RecipeEntry.RecipeType.SMELTING;
                    } else if (screen instanceof BlastFurnaceScreen) {
                        stationType = RecipeEntry.RecipeType.BLASTING;
                    } else if (screen instanceof SmokerScreen) {
                        stationType = RecipeEntry.RecipeType.SMOKING;
                    }
                    CraftingSidebar sidebar = new CraftingSidebar(
                            x,
                            y,
                            width,
                            height,
                            screen instanceof CraftingScreen,
                            compact,
                            previewX,
                            previewY,
                            previewWidth,
                            previewHeight,
                            stationType
                    );

                    List<AbstractWidget> customWidgets =
                            new ArrayList<>();

                    sidebar.registerWidgets(widget -> {

                        Screens.getWidgets(screen).add(widget);

                        if (!(widget instanceof Button)) {
                            customWidgets.add(widget);
                        }
                    });

                    ScreenEvents.afterExtract(screen).register(
                            (currentScreen, graphics, mouseX, mouseY, delta) -> {

                                for (AbstractWidget widget : customWidgets) {
                                    if (widget.visible) {
                                        widget.extractRenderState(
                                                graphics,
                                                mouseX,
                                                mouseY,
                                                delta
                                        );
                                    }
                                }
                            }
                    );

                    ScreenMouseEvents.allowMouseScroll(screen).register(
                            (currentScreen, mouseX, mouseY, horizontal, vertical) -> {

                                boolean handled = sidebar.handleAmountScroll(
                                        mouseX,
                                        mouseY,
                                        vertical
                                );

                                return !handled;
                            }
                    );
                }
        );
    }
}

