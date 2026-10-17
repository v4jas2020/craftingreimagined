package craftingreimagined.crafting;

import craftingreimagined.CraftingReimaginedClient;
import craftingreimagined.config.ModConfig;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;

import java.util.ArrayDeque;
import java.util.Queue;

public final class ClickQueue {

    /*
     * All waiting inventory clicks.
     */
    private static final Queue<QueuedClick> QUEUE =
            new ArrayDeque<>();

    /*
     * Time the previous click was executed.
     */
    private static long lastClickTime = 0L;

    /*
     * Prevents initialize() from registering
     * the tick event more than once.
     */
    private static boolean initialized = false;

    private ClickQueue() {
    }


    public static void initialize() {

        if (initialized) {
            return;
        }

        initialized = true;

        ClientTickEvents.END_CLIENT_TICK.register(
                ClickQueue::tick
        );

        CraftingReimaginedClient.LOGGER.info(
                "ClickQueue initialized."
        );
    }


    public static void leftClick(int slot) {

        enqueue(
                slot,
                0,
                ContainerInput.PICKUP
        );
    }


    public static void rightClick(int slot) {

        enqueue(
                slot,
                1,
                ContainerInput.PICKUP
        );
    }


    public static void quickMove(int slot) {

        enqueue(
                slot,
                0,
                ContainerInput.QUICK_MOVE
        );
    }


    public static void enqueue(
            int slot,
            int button,
            ContainerInput input
    ) {

        Minecraft client =
                Minecraft.getInstance();

        if (client.player == null) {

            CraftingReimaginedClient.LOGGER.warn(
                    "Tried to queue a click while no player exists."
            );

            return;
        }

        AbstractContainerMenu menu =
                client.player.containerMenu;

        /*
         * Do not allow invalid slots.
         */
        if (slot < 0 || slot >= menu.slots.size()) {

            CraftingReimaginedClient.LOGGER.warn(
                    "Tried to queue invalid slot {}. Menu contains {} slots.",
                    slot,
                    menu.slots.size()
            );

            return;
        }

        QUEUE.add(
                new QueuedClick(
                        menu,
                        slot,
                        button,
                        input
                )
        );
    }


    private static void tick(
            Minecraft client
    ) {

        if (QUEUE.isEmpty()) {
            return;
        }

        /*
         * If the player/world disappears,
         * stop automation immediately.
         */
        if (client.player == null
                || client.level == null
                || client.gameMode == null) {

            clear();
            return;
        }

        QueuedClick click =
                QUEUE.peek();

        if (click == null) {
            return;
        }

        /*
         * Very important safety check:
         *
         * If the player closed the crafting table,
         * opened a chest, etc. while actions were queued,
         * NEVER send those old clicks to the new menu.
         */
        if (client.player.containerMenu != click.menu()) {

            CraftingReimaginedClient.LOGGER.warn(
                    "Container changed while ClickQueue was running. Cancelling queued clicks."
            );

            clear();
            return;
        }

        long currentTime =
                System.nanoTime();

        long delayNanos =
                ModConfig.getClickDelayMs()
                        * 1_000_000L;

        if (currentTime - lastClickTime < delayNanos) {
            return;
        }

        executeClick(
                client,
                click
        );

        QUEUE.poll();

        lastClickTime =
                currentTime;
    }


    private static void executeClick(
            Minecraft client,
            QueuedClick click
    ) {

        if (client.player == null
                || client.gameMode == null) {

            return;
        }

        AbstractContainerMenu menu =
                click.menu();

        try {

            client.gameMode.handleContainerInput(
                    menu.containerId,
                    click.slot(),
                    click.button(),
                    click.input(),
                    client.player
            );

        } catch (Exception exception) {

            CraftingReimaginedClient.LOGGER.error(
                    "Failed to execute queued container click.",
                    exception
            );

            clear();
        }
    }


    public static void clear() {

        QUEUE.clear();

        lastClickTime = 0L;
    }


    public static boolean isBusy() {
        return !QUEUE.isEmpty();
    }


    public static int size() {
        return QUEUE.size();
    }

    private record QueuedClick(
            AbstractContainerMenu menu,
            int slot,
            int button,
            ContainerInput input
    ) {
    }
}