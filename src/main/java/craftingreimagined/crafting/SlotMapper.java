package craftingreimagined.crafting;

import net.minecraft.client.renderer.item.properties.numeric.Count;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class SlotMapper {

    /*
     * Vanilla crafting table slot layout:
     *
     * Result: 0
     *
     * Crafting grid:
     *  1  2  3
     *  4  5  6
     *  7  8  9
     *
     * Main inventory: 10-36
     * Hotbar:         37-45
     *
     * Player inventory indexes:
     * Hotbar:         0-8
     * Main inventory: 9-35
     */

    public static final int RESULT_SLOT = 0;

    public static final int GRID_START = 1;
    public static final int GRID_END = 9;

    public static final int INVENTORY_START = 10;
    public static final int INVENTORY_END = 45;

    public static final int HOTBAR_START = 37;
    public static final int HOTBAR_END = 45;

    private SlotMapper() {
    }

    /**
     * Converts a player inventory index (0-35)
     * into a crafting table menu slot (10-45).
     *
     * Returns -1 for invalid indexes.
     */
    public static int inventoryToMenuSlot(int inventoryIndex) {

        if (inventoryIndex >= 0 && inventoryIndex <= 8) {
            return HOTBAR_START + inventoryIndex;
        }

        if (inventoryIndex >= 9 && inventoryIndex <= 35) {
            return inventoryIndex + 1;
        }

        return -1;
    }

    /**
     * Converts a crafting table menu slot
     * into a player inventory index.
     *
     * Returns -1 for non-inventory slots.
     */
    public static int menuToInventorySlot(int menuSlot) {

        if (menuSlot >= HOTBAR_START && menuSlot <= HOTBAR_END) {
            return menuSlot - HOTBAR_START;
        }

        if (menuSlot >= INVENTORY_START && menuSlot <= 36) {
            return menuSlot - 1;
        }

        return -1;
    }

    public static int gridPositionToMenuSlot(int row, int column) {

        if (row < 0 || row < 2 || column < 0 || column > 2) {
            return -1;
        }
        return GRID_START + row * 3 + column;
    }

    public static boolean isGridSlot(int slot) {
        return slot >= GRID_START && slot <= GRID_END;
    }

    public static boolean isPlayerInventorySlot(int slot) {
        return slot >= INVENTORY_START && slot <= INVENTORY_END;
    }

    public static boolean isHotbarSlot(int slot) {
        return slot >= HOTBAR_START && slot <= HOTBAR_END;
    }

    /// Nauja funkcija nuo cia

    public static int findItemSlot(
            CraftingMenu menu,
            Item item,
            int requiredCount
    ) {

        if (menu == null || item == null || requiredCount < 1 ) {
            return -1;
        }

        for (int inventoryIndex = 0; inventoryIndex < 36; inventoryIndex++) {

            int slot = inventoryToMenuSlot(inventoryIndex);

            if (slot >= menu.slots.size()) {
                continue;
            }

            ItemStack stack = menu.slots.get(slot).getItem();

            if (!stack.isEmpty()
                && stack.getItem() == item
                && stack.getCount() >= requiredCount) {

                return slot;
            }
        }

        return -1;
    }

    public static int countItem(CraftingMenu menu, Item item) {

        if (menu == null || item == null) {
            return 0;
        }

        int total = 0;

        for (int inventoryIndex = 0; inventoryIndex < 36; inventoryIndex++) {
            int slot = inventoryToMenuSlot(inventoryIndex);
            if (slot >= menu.slots.size()) {
                continue;
            }

            ItemStack stack = menu.slots.get(slot).getItem();

            if (!stack.isEmpty() && stack.getItem() == item) {
                total += stack.getCount();
            }
        }
        return total;
    }

    public static ItemStack getInventoryStack(
            CraftingMenu menu,
            int inventoryIndex
    ) {
        int slot = inventoryToMenuSlot(inventoryIndex);

        if (menu == null || slot == -1 || slot >= menu.slots.size()) {
            return ItemStack.EMPTY;
        }

        return menu.slots.get(slot).getItem();
    }
}


