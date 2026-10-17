package craftingreimagined.config;

import craftingreimagined.CraftingReimaginedClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class ModConfig {

    private static final Path CONFIG_PATH =
            FabricLoader.getInstance()
                    .getConfigDir()
                    .resolve("craftingreimagined.properties");

    /*
     * Default nustatymai
     */

    private static boolean enabled = true;

    private static boolean showCraftingSidebar = true;

    private static boolean showOnlyCraftableRecipes = false;

    private static boolean rememberCraftAmount = true;

    private static int defaultCraftAmount = 1;


    private static int clickDelayMs = 50;

    private ModConfig() {
    }


    public static void initialize() {

        if (Files.exists(CONFIG_PATH)) {
            load();
        } else {
            save();
        }

        CraftingReimaginedClient.LOGGER.info(
                "Crafting Reimagined config loaded from {}",
                CONFIG_PATH
        );
    }


    public static void load() {

        Properties properties = new Properties();

        try (InputStream input = Files.newInputStream(CONFIG_PATH)) {

            properties.load(input);

            enabled = getBoolean(
                    properties,
                    "enabled",
                    true
            );

            showCraftingSidebar = getBoolean(
                    properties,
                    "showCraftingSidebar",
                    true
            );

            showOnlyCraftableRecipes = getBoolean(
                    properties,
                    "showOnlyCraftableRecipes",
                    false
            );

            rememberCraftAmount = getBoolean(
                    properties,
                    "rememberCraftAmount",
                    true
            );

            defaultCraftAmount = getInt(
                    properties,
                    "defaultCraftAmount",
                    1,
                    1,
                    64
            );

            clickDelayMs = getInt(
                    properties,
                    "clickDelayMs",
                    50,
                    0,
                    1000
            );

        } catch (IOException exception) {

            CraftingReimaginedClient.LOGGER.error(
                    "Failed to load Crafting Reimagined config",
                    exception
            );
        }
    }

    /**
     * Issaugo settingus
     */
    public static void save() {

        Properties properties = new Properties();

        properties.setProperty(
                "enabled",
                Boolean.toString(enabled)
        );

        properties.setProperty(
                "showCraftingSidebar",
                Boolean.toString(showCraftingSidebar)
        );

        properties.setProperty(
                "showOnlyCraftableRecipes",
                Boolean.toString(showOnlyCraftableRecipes)
        );

        properties.setProperty(
                "rememberCraftAmount",
                Boolean.toString(rememberCraftAmount)
        );

        properties.setProperty(
                "defaultCraftAmount",
                Integer.toString(defaultCraftAmount)
        );

        properties.setProperty(
                "clickDelayMs",
                Integer.toString(clickDelayMs)
        );

        try {

            Files.createDirectories(
                    CONFIG_PATH.getParent()
            );

            try (OutputStream output =
                         Files.newOutputStream(CONFIG_PATH)) {

                properties.store(
                        output,
                        "Crafting Reimagined Configuration"
                );
            }

        } catch (IOException exception) {

            CraftingReimaginedClient.LOGGER.error(
                    "Failed to save Crafting Reimagined config",
                    exception
            );
        }
    }


    public static boolean isEnabled() {
        return enabled;
    }

    public static boolean isShowCraftingSidebar() {
        return showCraftingSidebar;
    }

    public static boolean isShowOnlyCraftableRecipes() {
        return showOnlyCraftableRecipes;
    }

    public static boolean isRememberCraftAmount() {
        return rememberCraftAmount;
    }

    public static int getDefaultCraftAmount() {
        return defaultCraftAmount;
    }

    public static int getClickDelayMs() {
        return clickDelayMs;
    }


    public static void setEnabled(boolean value) {
        enabled = value;
        save();
    }

    public static void setShowCraftingSidebar(boolean value) {
        showCraftingSidebar = value;
        save();
    }

    public static void setShowOnlyCraftableRecipes(boolean value) {
        showOnlyCraftableRecipes = value;
        save();
    }

    public static void setRememberCraftAmount(boolean value) {
        rememberCraftAmount = value;
        save();
    }

    public static void setDefaultCraftAmount(int value) {

        defaultCraftAmount = clamp(
                value,
                1,
                64
        );

        save();
    }

    public static void setClickDelayMs(int value) {

        clickDelayMs = clamp(
                value,
                0,
                1000
        );

        save();
    }

    /*
     * Helpers
     */

    private static boolean getBoolean(
            Properties properties,
            String key,
            boolean defaultValue
    ) {

        String value = properties.getProperty(key);

        if (value == null) {
            return defaultValue;
        }

        return Boolean.parseBoolean(value);
    }

    private static int getInt(
            Properties properties,
            String key,
            int defaultValue,
            int minimum,
            int maximum
    ) {

        String value = properties.getProperty(key);

        if (value == null) {
            return defaultValue;
        }

        try {

            int number = Integer.parseInt(value);

            return clamp(
                    number,
                    minimum,
                    maximum
            );

        } catch (NumberFormatException exception) {

            return defaultValue;
        }
    }

    private static int clamp(
            int value,
            int minimum,
            int maximum
    ) {

        return Math.max(
                minimum,
                Math.min(maximum, value)
        );
    }
}