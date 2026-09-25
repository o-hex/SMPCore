package pl.smpcore.altars;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import pl.Main;
import pl.smpcore.util.ItemBuilder;

public class AltarEditGUI implements Listener {
    private static final String GUI_TITLE_PREFIX = ChatColor.DARK_GRAY + "Edit Altar: ";

    public static void open(Player player, Altar altar) {
        if (player == null || altar == null) return;
        Inventory inv = Bukkit.createInventory(null, 27, GUI_TITLE_PREFIX + altar.getType());

        // Fill background
        ItemStack grayGlass = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, grayGlass);
        }

        // Slot 11: Enabled Toggle
        ItemStack enabledItem = new ItemBuilder(altar.isEnabled() ? Material.LIME_DYE : Material.GRAY_DYE)
                .name(ChatColor.GOLD + "Enabled: " + (altar.isEnabled() ? ChatColor.GREEN + "YES" : ChatColor.RED + "NO"))
                .lore(ChatColor.GRAY + "Click to toggle enabled state.")
                .build();
        inv.setItem(11, enabledItem);

        // Slot 12: Consume Materials Toggle
        ItemStack consumeItem = new ItemBuilder(altar.isConsumeMaterials() ? Material.LIME_DYE : Material.GRAY_DYE)
                .name(ChatColor.GOLD + "Consume Item: " + (altar.isConsumeMaterials() ? ChatColor.GREEN + "YES" : ChatColor.RED + "NO"))
                .lore(ChatColor.GRAY + "Click to toggle consuming input materials.")
                .build();
        inv.setItem(12, consumeItem);

        // Slot 13: Ritual Toggle
        ItemStack ritualItem = new ItemBuilder(altar.isRitual() ? Material.LIME_DYE : Material.GRAY_DYE)
                .name(ChatColor.GOLD + "Ritual: " + (altar.isRitual() ? ChatColor.GREEN + "YES" : ChatColor.RED + "NO"))
                .lore(ChatColor.GRAY + "Click to toggle ritual activation mode.")
                .build();
        inv.setItem(13, ritualItem);

        // Slot 14: Hologram Height (+/- 0.1)
        ItemStack holoItem = new ItemBuilder(Material.ARMOR_STAND)
                .name(ChatColor.GOLD + "Hologram Height: " + ChatColor.YELLOW + altar.getHologramHeight())
                .lore(ChatColor.GRAY + "Left Click: +0.1", ChatColor.GRAY + "Right Click: -0.1")
                .build();
        inv.setItem(14, holoItem);

        // Slot 15: Item Height (+/- 0.1)
        ItemStack itemHItem = new ItemBuilder(Material.ITEM_FRAME)
                .name(ChatColor.GOLD + "Item Height: " + ChatColor.YELLOW + altar.getItemHeight())
                .lore(ChatColor.GRAY + "Left Click: +0.1", ChatColor.GRAY + "Right Click: -0.1")
                .build();
        inv.setItem(15, itemHItem);

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.startsWith(GUI_TITLE_PREFIX)) return;

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        String type = title.substring(GUI_TITLE_PREFIX.length()).toLowerCase();

        AltarManager manager = Main.getAltarManager();
        if (manager == null) return;
        Altar altar = manager.getAltar(type);
        if (altar == null) return;

        int slot = event.getRawSlot();
        if (slot == 11) {
            altar.setEnabled(!altar.isEnabled());
        } else if (slot == 12) {
            altar.setConsumeMaterials(!altar.isConsumeMaterials());
        } else if (slot == 13) {
            altar.setRitual(!altar.isRitual());
        } else if (slot == 14) {
            double delta = event.isRightClick() ? -0.1 : 0.1;
            altar.setHologramHeight(Math.max(0.5, Math.min(5.0, Math.round((altar.getHologramHeight() + delta) * 10.0) / 10.0)));
            manager.spawnDisplays(altar);
        } else if (slot == 15) {
            double delta = event.isRightClick() ? -0.1 : 0.1;
            altar.setItemHeight(Math.max(0.0, Math.min(5.0, Math.round((altar.getItemHeight() + delta) * 10.0) / 10.0)));
            manager.spawnDisplays(altar);
        } else {
            return;
        }

        if (Main.getInstance().getConfigManager() != null) {
            Main.getInstance().getConfigManager().saveAltar(altar);
        }
        open(player, altar);
    }
}
