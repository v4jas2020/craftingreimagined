
package craftingreimagined;

import craftingreimagined.config.ModConfig;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CraftingReimaginedClient implements ClientModInitializer {

    public static final String MOD_ID = "craftingreimagined";

    public static final Logger LOGGER =
            LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {

        LOGGER.info("Crafting Reimagined is starting!");

        ModConfig.initialize();
    }
}

        /* Veliau padaryt:
        ModConfig.initialize() - Padaryta :smiley:
        RecipeIndex.initialize()
        CraftingSidebar.initialize();
        Keybinds.initialize();
        Ateities sau: Dbr reikejo padaryti kad butu simple main class
        pato reikes pasiziureti ar viskas gerai uzloadina
         */

