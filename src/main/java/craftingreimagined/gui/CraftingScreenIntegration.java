package craftingreimagined.gui;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

import java.util.ArrayList;
import java.util.List;

public final class  CraftingScreenIntegration {

    private static final int VANILLA_MENU_WIDTH = 176;
    private static final int SIDEBAR_WIDTH = 330;
    private static final int MIN_SIDEBAR_WIDTH = 300;
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
                            && !(screen instanceof InventoryScreen)) {
                        return;
                    }

                    int menuLeft = (screenWidth - VANILLA_MENU_WIDTH) / 2;
                    int menuRight = menuLeft + VANILLA_MENU_WIDTH;

                    int availableRight = screenWidth - menuRight - 8;
                    int availableLeft = menuLeft - 8;

                    boolean useRightSide =
                            availableRight >= availableLeft;

                    int availableWidth = useRightSide
                            ? availableRight
                            : availableLeft;

                    int width = Math.min(
                            SIDEBAR_WIDTH,
                            availableWidth - 4
                    );

                    int height = Math.min(
                            SIDEBAR_HEIGHT,
                            screenHeight - 12
                    );

                    if (width < MIN_SIDEBAR_WIDTH || height < 220) {
                        return;
                    }

                    int x = useRightSide
                            ? menuRight + 4
                            : menuLeft - width - 4;

                    int y = (screenHeight - height) / 2;

                    CraftingSidebar sidebar = new CraftingSidebar(
                            x,
                            y,
                            width,
                            height
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

