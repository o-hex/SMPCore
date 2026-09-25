package pl.smpcore.commands;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import pl.Main;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class KitCommand implements CommandExecutor, TabCompleter, Listener {
    private static File file;
    private static YamlConfiguration config;

    public static void init() {
        file = new File(Main.getInstance().getDataFolder(), "kits.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                Main.getInstance().getLogger().warning("Could not create kits.yml");
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public static void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            Main.getInstance().getLogger().warning("Could not save kits.yml");
        }
    }

    public static void giveKit(Player player, String kitName) {
        if (config == null) init();
        String path = "kits." + kitName.toLowerCase();
        if (!config.contains(path)) {
            player.sendMessage(ChatColor.RED + "Kit '" + kitName + "' does not exist!");
            return;
        }

        ConfigurationSection section = config.getConfigurationSection(path + ".items");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                int slot = Integer.parseInt(key);
                ItemStack item = section.getItemStack(key);
                if (item != null) {
                    player.getInventory().setItem(slot, item.clone());
                }
            }
        }
        player.sendMessage(ChatColor.GREEN + "Received kit " + kitName + "!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("smpcore.sckit")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /sckit <save|get|resetplayers> <name>");
            return true;
        }

        String sub = args[0].toLowerCase();
        String kitName = args[1].toLowerCase();

        if (config == null) init();

        switch (sub) {
            case "save": {
                String path = "kits." + kitName + ".items";
                config.set(path, null);
                ItemStack[] contents = player.getInventory().getContents();
                for (int i = 0; i < contents.length; i++) {
                    if (contents[i] != null && contents[i].getType() != Material.AIR) {
                        config.set(path + "." + i, contents[i]);
                    }
                }
                save();
                player.sendMessage(ChatColor.GREEN + "Saved kit '" + kitName + "' with your current inventory!");
                return true;
            }
            case "get": {
                giveKit(player, kitName);
                return true;
            }
            case "resetplayers": {
                config.set("claimed_players." + kitName, null);
                save();
                player.sendMessage(ChatColor.GREEN + "Reset claimed status for kit '" + kitName + "'!");
                return true;
            }
            default:
                player.sendMessage(ChatColor.RED + "Usage: /sckit <save|get|resetplayers> <name>");
                return true;
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!Main.getInstance().getConfig().getBoolean("config.kit_on_join", true)) return;

        Player player = event.getPlayer();
        if (config == null) init();

        String claimPath = "claimed_players.join." + player.getUniqueId();
        if (!config.getBoolean(claimPath, false)) {
            if (config.contains("kits.join")) {
                giveKit(player, "join");
                config.set(claimPath, true);
                save();
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("save", "get", "resetplayers");
        }
        if (args.length == 2) {
            if (config == null) init();
            ConfigurationSection sec = config.getConfigurationSection("kits");
            if (sec != null) {
                return new ArrayList<>(sec.getKeys(false));
            }
        }
        return new ArrayList<>();
    }
}
