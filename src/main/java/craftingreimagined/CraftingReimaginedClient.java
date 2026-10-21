package craftingreimagined;

import craftingreimagined.config.ModConfig;
import craftingreimagined.crafting.ClickQueue;
import craftingreimagined.gui.CraftingScreenIntegration;

import net.fabricmc.api.ClientModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CraftingReimaginedClient
        implements ClientModInitializer {

    public static final String MOD_ID = "craftingreimagined";

    public static final Logger LOGGER =
            LoggerFactory.getLogger(MOD_ID);

    private static boolean initialized = false;

    @Override
    public void onInitializeClient() {

        if (initialized) {
            return;
        }

        initialized = true;

        LOGGER.info("Initializing Crafting Reimagined...");

        ModConfig.initialize();

        ClickQueue.initialize();

        CraftingScreenIntegration.initialize();

        LOGGER.info("Crafting Reimagined initialized successfully!");
    }
}

        /* Veliau padaryt:
        ModConfig.initialize() - Padaryta :smiley:
        RecipeIndex.initialize() - tuoj bus
        CraftingSidebar.initialize();
         */

