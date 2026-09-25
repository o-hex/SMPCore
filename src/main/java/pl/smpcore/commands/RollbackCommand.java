package pl.smpcore.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import pl.Main;
import pl.smpcore.util.ItemBuilder;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class RollbackCommand implements CommandExecutor, TabCompleter, Listener {
    private static final String GUI_LIST_PREFIX = ChatColor.DARK_GRAY + "Rollback: ";
    private static final String GUI_PREVIEW_PREFIX = ChatColor.DARK_GRAY + "Death Preview: ";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static File getRollbackFile(UUID uuid) {
        File dir = new File(Main.getInstance().getDataFolder(), "rollbacks");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, uuid.toString() + ".yml");
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        File file = getRollbackFile(player.getUniqueId());
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        long timestamp = System.currentTimeMillis();
        String path = "deaths." + timestamp;

        config.set(path + ".date", DATE_FORMAT.format(new Date(timestamp)));
        config.set(path + ".cause", event.getDeathMessage() != null ? event.getDeathMessage() : "Unknown");
        config.set(path + ".level", player.getLevel());
        config.set(path + ".exp", player.getExp());

        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null && contents[i].getType() != Material.AIR) {
                config.set(path + ".inventory." + i, contents[i]);
            }
        }

        try {
            config.save(file);
        } catch (IOException e) {
            Main.getInstance().getLogger().warning("Could not save rollback for " + player.getName());
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("smpcore.rollback")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Usage: /rollback <player>");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (target == null || target.getUniqueId() == null) {
            player.sendMessage(ChatColor.RED + "Player not found: " + args[0]);
            return true;
        }

        openRollbackList(player, target);
        return true;
    }

    private static void openRollbackList(Player player, OfflinePlayer target) {
        File file = getRollbackFile(target.getUniqueId());
        if (!file.exists()) {
            player.sendMessage(ChatColor.RED + "No death history found for " + target.getName());
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection deathsSection = config.getConfigurationSection("deaths");
        if (deathsSection == null || deathsSection.getKeys(false).isEmpty()) {
            player.sendMessage(ChatColor.RED + "No death history found for " + target.getName());
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 54, GUI_LIST_PREFIX + target.getName());
        List<String> keys = new ArrayList<>(deathsSection.getKeys(false));
        keys.sort(Collections.reverseOrder()); // Most recent first

        int slot = 0;
        for (String key : keys) {
            if (slot >= 54) break;
            String date = deathsSection.getString(key + ".date", "Unknown date");
            String cause = deathsSection.getString(key + ".cause", "Unknown cause");
            int level = deathsSection.getInt(key + ".level", 0);

            ItemStack item = new ItemBuilder(Material.SKELETON_SKULL)
                    .name(ChatColor.YELLOW + date)
                    .lore(ChatColor.GRAY + "Cause: " + ChatColor.WHITE + cause,
                            ChatColor.GRAY + "XP Level: " + ChatColor.GREEN + level,
                            ChatColor.DARK_GRAY + "ID: " + key,
                            ChatColor.AQUA + "Click to view and restore.")
                    .build();
            inv.setItem(slot++, item);
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        if (title.startsWith(GUI_LIST_PREFIX)) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() != Material.SKELETON_SKULL) return;

            String targetName = title.substring(GUI_LIST_PREFIX.length());
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            if (target.getUniqueId() == null) return;

            List<String> lore = clicked.getItemMeta().getLore();
            if (lore == null) return;

            String deathId = null;
            for (String line : lore) {
                String plain = ChatColor.stripColor(line);
                if (plain.startsWith("ID: ")) {
                    deathId = plain.substring(4).trim();
                    break;
                }
            }
            if (deathId == null) return;

            openPreview(player, target, deathId);
            return;
        }

        if (title.startsWith(GUI_PREVIEW_PREFIX)) {
            event.setCancelled(true);
            int slot = event.getRawSlot();

            // Restore button in slot 49
            if (slot == 49) {
                String raw = title.substring(GUI_PREVIEW_PREFIX.length());
                String[] parts = raw.split(" - ");
                if (parts.length < 2) return;

                String targetName = parts[0];
                String deathId = parts[1];
                Player target = Bukkit.getPlayer(targetName);

                if (target == null || !target.isOnline()) {
                    player.sendMessage(ChatColor.RED + "Target player must be online to restore inventory!");
                    return;
                }

                File file = getRollbackFile(target.getUniqueId());
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                String path = "deaths." + deathId;

                ConfigurationSection invSection = config.getConfigurationSection(path + ".inventory");
                if (invSection != null) {
                    target.getInventory().clear();
                    for (String key : invSection.getKeys(false)) {
                        int itemSlot = Integer.parseInt(key);
                        ItemStack item = invSection.getItemStack(key);
                        target.getInventory().setItem(itemSlot, item);
                    }
                }
                int level = config.getInt(path + ".level", 0);
                target.setLevel(level);

                player.closeInventory();
                player.sendMessage(ChatColor.GREEN + "Restored inventory of " + target.getName() + " from " + deathId);
                target.sendMessage(ChatColor.GREEN + "Your inventory has been restored by an administrator!");
                target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            }
        }
    }

    private static void openPreview(Player player, OfflinePlayer target, String deathId) {
        File file = getRollbackFile(target.getUniqueId());
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        String path = "deaths." + deathId;

        Inventory inv = Bukkit.createInventory(null, 54, GUI_PREVIEW_PREFIX + target.getName() + " - " + deathId);

        ConfigurationSection invSection = config.getConfigurationSection(path + ".inventory");
        if (invSection != null) {
            for (String key : invSection.getKeys(false)) {
                int slot = Integer.parseInt(key);
                if (slot < 45) {
                    inv.setItem(slot, invSection.getItemStack(key));
                }
            }
        }

        // Fill row 45-53
        ItemStack grayGlass = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 45; i < 54; i++) inv.setItem(i, grayGlass);

        // Restore button at 49
        ItemStack restoreBtn = new ItemBuilder(Material.LIME_DYE)
                .name(ChatColor.GREEN + "" + ChatColor.BOLD + "RESTORE INVENTORY")
                .lore(ChatColor.GRAY + "Click to restore this death snapshot to " + target.getName())
                .build();
        inv.setItem(49, restoreBtn);

        player.openInventory(inv);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[0].toLowerCase())) list.add(p.getName());
            }
            return list;
        }
        return new ArrayList<>();
    }
}
