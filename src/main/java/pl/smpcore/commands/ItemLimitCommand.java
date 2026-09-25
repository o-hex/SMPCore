package pl.smpcore.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import pl.Main;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ItemLimitCommand implements CommandExecutor, TabCompleter, Listener {
    private static final Map<Material, Integer> limits = new ConcurrentHashMap<>();
    private static File file;
    private static YamlConfiguration config;

    private static BukkitTask checkTask;

    public static void init() {
        file = new File(Main.getInstance().getDataFolder(), "item_limits.yml");
        if (!file.exists()) {
            Main.getInstance().saveResource("item_limits.yml", false);
        }
        load();

        if (Main.getInstance().getConfig().getBoolean("heavy.continuous_item_limit", true)) {
            int interval = Main.getInstance().getConfig().getInt("heavy.item_limit_check_interval", 20);
            checkTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (!Main.getInstance().getConfig().getBoolean("heavy.item_limit_enabled", true)) return;
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        checkAndEnforce(player);
                    }
                }
            }.runTaskTimer(Main.getInstance(), interval, interval);
        }
    }

    public static void cleanup() {
        if (checkTask != null) {
            checkTask.cancel();
            checkTask = null;
        }
    }

    public static void load() {
        config = YamlConfiguration.loadConfiguration(file);
        limits.clear();
        for (String key : config.getKeys(false)) {
            Material mat = Material.matchMaterial(key);
            if (mat != null) {
                limits.put(mat, config.getInt(key));
            }
        }
    }

    public static void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            Main.getInstance().getLogger().warning("Could not save item_limits.yml");
        }
    }

    public static void checkAndEnforce(Player player) {
        if (player.hasPermission("smpcore.bypass.itemlimit")) return;

        for (Map.Entry<Material, Integer> entry : limits.entrySet()) {
            Material mat = entry.getKey();
            int max = entry.getValue();

            int total = 0;
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.getType() == mat) {
                    total += item.getAmount();
                }
            }

            if (total > max) {
                int toRemove = total - max;
                for (int slot = player.getInventory().getSize() - 1; slot >= 0; slot--) {
                    ItemStack item = player.getInventory().getItem(slot);
                    if (item != null && item.getType() == mat) {
                        int amount = item.getAmount();
                        if (amount <= toRemove) {
                            player.getInventory().setItem(slot, null);
                            toRemove -= amount;
                        } else {
                            item.setAmount(amount - toRemove);
                            toRemove = 0;
                        }
                        if (toRemove <= 0) break;
                    }
                }
                player.sendMessage(ChatColor.RED + "You exceeded the item limit for " + mat.name() + " (" + max + " max). Excess removed.");
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("smpcore.itemlimit")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Usage: /itemlimit <amount>");
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid number: " + args[0]);
            return true;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "You need to be holding an item to set its limit!");
            return true;
        }

        Material mat = hand.getType();
        if (amount <= 0) {
            limits.remove(mat);
            config.set(mat.name(), null);
            save();
            player.sendMessage(ChatColor.GREEN + "Removed item limit for " + mat.name());
        } else {
            limits.put(mat, amount);
            config.set(mat.name(), amount);
            save();
            player.sendMessage(ChatColor.GREEN + "Set item limit for " + mat.name() + " to " + amount);
        }

        return true;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!Main.getInstance().getConfig().getBoolean("heavy.item_limit_enabled", true)) return;
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        if (player.hasPermission("smpcore.bypass.itemlimit")) return;

        Material mat = event.getItem().getItemStack().getType();
        Integer max = limits.get(mat);
        if (max == null) return;

        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == mat) {
                total += item.getAmount();
            }
        }

        if (total + event.getItem().getItemStack().getAmount() > max) {
            event.setCancelled(true);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return new ArrayList<>();
    }
}
