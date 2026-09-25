package pl.smpcore.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import pl.smpcore.util.ItemBuilder;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InvseeCommand implements CommandExecutor, Listener {
    private static final String INVSEE_PREFIX = ChatColor.DARK_GRAY + "Invsee: ";
    private static final Map<UUID, UUID> viewers = new ConcurrentHashMap<>();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player viewer = (Player) sender;
        String cmd = command.getName().toLowerCase();

        if (args.length < 1) {
            viewer.sendMessage(ChatColor.RED + "Usage: /" + cmd + " <player>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            viewer.sendMessage(ChatColor.RED + "Player not found: " + args[0]);
            return true;
        }

        if (cmd.equals("endersee")) {
            if (!viewer.hasPermission("smpcore.endersee")) {
                viewer.sendMessage(ChatColor.RED + "You don't have permission to use /endersee.");
                return true;
            }
            viewer.openInventory(target.getEnderChest());
            return true;
        }

        if (cmd.equals("invsee")) {
            if (!viewer.hasPermission("smpcore.invsee")) {
                viewer.sendMessage(ChatColor.RED + "You don't have permission to use /invsee.");
                return true;
            }
            openInvsee(viewer, target);
            return true;
        }

        return false;
    }

    public static void openInvsee(Player viewer, Player target) {
        Inventory inv = Bukkit.createInventory(null, 54, INVSEE_PREFIX + target.getName());

        // Copy 0-35 items
        for (int i = 0; i < 36; i++) {
            inv.setItem(i, target.getInventory().getItem(i));
        }

        // Divider
        ItemStack grayGlass = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 36; i < 45; i++) {
            inv.setItem(i, grayGlass);
        }

        // Armor in 45-48
        ItemStack[] armor = target.getInventory().getArmorContents();
        inv.setItem(45, armor.length > 3 ? armor[3] : null); // Helmet
        inv.setItem(46, armor.length > 2 ? armor[2] : null); // Chestplate
        inv.setItem(47, armor.length > 1 ? armor[1] : null); // Leggings
        inv.setItem(48, armor.length > 0 ? armor[0] : null); // Boots

        // Offhand in 53
        inv.setItem(53, target.getInventory().getItemInOffHand());

        viewers.put(viewer.getUniqueId(), target.getUniqueId());
        viewer.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.startsWith(INVSEE_PREFIX)) return;

        Player viewer = (Player) event.getWhoClicked();
        UUID targetId = viewers.get(viewer.getUniqueId());
        if (targetId == null) return;
        Player target = Bukkit.getPlayer(targetId);
        if (target == null || !target.isOnline()) return;

        int slot = event.getRawSlot();
        if (slot >= 36 && slot < 45) {
            event.setCancelled(true);
            return;
        }

        // Sync changes after tick
        Bukkit.getScheduler().runTask(pl.Main.getInstance(), () -> {
            if (!target.isOnline()) return;
            Inventory inv = event.getView().getTopInventory();

            // Sync main inventory 0-35
            for (int i = 0; i < 36; i++) {
                target.getInventory().setItem(i, inv.getItem(i));
            }

            // Sync armor 45-48
            ItemStack helm = inv.getItem(45);
            ItemStack chest = inv.getItem(46);
            ItemStack legs = inv.getItem(47);
            ItemStack boots = inv.getItem(48);
            target.getInventory().setArmorContents(new ItemStack[]{boots, legs, chest, helm});

            // Sync offhand 53
            target.getInventory().setItemInOffHand(inv.getItem(53));
        });
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        viewers.remove(event.getPlayer().getUniqueId());
    }
}
