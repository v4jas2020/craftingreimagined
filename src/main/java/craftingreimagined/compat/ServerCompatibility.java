package craftingreimagined.compat;

import net.minecraft.client.Minecraft;

public final class ServerCompatibility {

    private ServerCompatibility() {
    }

    public static boolean isInGame() {
        Minecraft client = Minecraft.getInstance();

        return client.player != null
                && client.level != null;
    }

    public static boolean isSingleplayer() {
        Minecraft client = Minecraft.getInstance();

        return client.getSingleplayerServer() != null;
    }

    public static boolean isMultiplayer() {
        Minecraft client = Minecraft.getInstance();

        return isInGame()
                && client.getConnection() != null
                && !isSingleplayer();
    }

    public static boolean canUseContainerAutomation() {
        Minecraft client = Minecraft.getInstance();

        return client.player != null
                && client.level != null
                && client.gameMode != null;
    }

    public static boolean requiresServerMod() {
        return false;
    }

    public static String getConnectionType() {

        if (!isInGame()) {
            return "Not connected";
        }

        if (isSingleplayer()) {
            return "Singleplayer";
        }

        if (isMultiplayer()) {
            return "Multiplayer";
        }

        return "Unknown";
    }
}