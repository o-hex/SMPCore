package pl.smpcore.mechanics;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import pl.Main;

public class BannedItemsListener implements Listener {
    public static boolean isBanned(Material material) {
        if (material == null || material == Material.AIR) return false;
        FileConfiguration config = Main.getInstance().getConfig();

        if (material == Material.MACE && config.getBoolean("rules.ban_mace", false)) {
            return true;
        }
        if (material == Material.ENDER_PEARL && config.getBoolean("rules.ban_pearls", false)) {
            return true;
        }
        if (material.name().startsWith("NETHERITE_") && config.getBoolean("rules.ban_netherite", false)) {
            return true;
        }

        return config.getBoolean("banned-items." + material.name(), false);
    }

    private static boolean canBypass(Player player) {
        return player.isOp() || player.hasPermission("smpcore.bypass.banneditem");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        ItemStack result = event.getInventory().getResult();
        if (result != null && isBanned(result.getType())) {
            event.getInventory().setResult(new ItemStack(Material.AIR));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCraft(CraftItemEvent event) {
        ItemStack result = event.getCurrentItem();
        if (result != null && isBanned(result.getType())) {
            if (event.getWhoClicked() instanceof Player player) {
                if (canBypass(player)) return;
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "This item is banned on this server!");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareSmithing(PrepareSmithingEvent event) {
        ItemStack result = event.getResult();
        if (result != null && isBanned(result.getType())) {
            event.setResult(new ItemStack(Material.AIR));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item != null && isBanned(item.getType())) {
            Player player = event.getPlayer();
            if (canBypass(player)) return;
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "This item is banned on this server!");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (isBanned(event.getItem().getItemStack().getType())) {
                if (canBypass(player)) return;
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrop(PlayerDropItemEvent event) {
        if (isBanned(event.getItemDrop().getItemStack().getType())) {
            Player player = event.getPlayer();
            if (canBypass(player)) return;
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "This item is banned on this server!");
        }
    }
}
