package pl.smpcore.altars;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.Main;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AltarCommand implements CommandExecutor, TabCompleter {
    private final AltarManager altarManager;
    private final AltarConfigManager configManager;

    public AltarCommand(AltarManager altarManager, AltarConfigManager configManager) {
        this.altarManager = altarManager;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("smpcore.saltar")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /saltar <spawn|setitem|create|delete|toggle> <type>");
            return true;
        }

        String sub = args[0].toLowerCase();
        String type = args[1].toLowerCase();

        if (!configManager.hasAltarType(type) && !sub.equals("create") && !sub.equals("reload")) {
            player.sendMessage(ChatColor.RED + "Unknown altar type: " + type);
            return true;
        }

        Main plugin = Main.getInstance();

        switch (sub) {
            case "create": {
                ItemStack hand = player.getInventory().getItemInMainHand();
                if (hand.getType() == Material.AIR) {
                    player.sendMessage("§cYou need to be holding an item");
                    return true;
                }
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 1.0f, 1.0f);
                player.sendMessage("§aCreated " + type + ", Use §f/saltar spawn §ato spawn it");

                plugin.getConfig().set("altars." + type + ".reward", hand.clone());
                plugin.getConfig().set("altars." + type + ".name", type);
                plugin.getConfig().set("altars." + type + ".hologram-height", 2.5);
                plugin.getConfig().set("altars." + type + ".item_height", 1.75);
                plugin.getConfig().set("altars." + type + ".message", "§f§l<player> §6§lhas crafted the §e§l<item>§6!");
                plugin.getConfig().set("altars." + type + ".consume_materials", true);
                plugin.getConfig().set("altars." + type + ".enabled", true);
                plugin.saveConfig();
                return true;
            }
            case "spawn": {
                Location loc = player.getLocation().getBlock().getLocation();
                if (altarManager.spawnAltar(loc, type)) {
                    player.sendMessage(ChatColor.GREEN + "Altar spawned!");
                } else {
                    player.sendMessage(ChatColor.RED + "Failed to spawn altar!");
                }
                return true;
            }
            case "setitem": {
                ItemStack hand = player.getInventory().getItemInMainHand();
                if (hand.getType() == Material.AIR) {
                    player.sendMessage("§cYou need to be holding an item");
                    return true;
                }
                plugin.getConfig().set("altars." + type + ".reward", hand.clone());
                plugin.saveConfig();

                Altar active = altarManager.getAltar(type);
                if (active != null) {
                    active.setReward(hand.clone());
                    active.setItem(hand.clone());
                    altarManager.spawnDisplays(active);
                }
                player.sendMessage(ChatColor.GREEN + "Set reward item for " + type);
                return true;
            }
            case "delete": {
                Altar active = altarManager.getAltar(type);
                if (active != null) {
                    altarManager.removeAltar(active.getLocation(), true);
                }
                plugin.getConfig().set("altars." + type, null);
                plugin.saveConfig();
                player.sendMessage("§7Deleted §f" + type);
                return true;
            }
            case "toggle": {
                boolean enabled = plugin.getConfig().getBoolean("altars." + type + ".enabled", true);
                plugin.getConfig().set("altars." + type + ".enabled", !enabled);
                plugin.saveConfig();

                player.playSound(player.getLocation(), Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 1.0f, 1.0f);
                player.sendMessage("§7Toggled §f" + type + "§7 to " + (!enabled));
                return true;
            }
            case "edit": {
                Altar altar = configManager.loadAltarType(type, player.getLocation());
                if (altar != null) {
                    AltarEditGUI.open(player, altar);
                }
                return true;
            }
            default:
                player.sendMessage(ChatColor.RED + "Usage: /saltar <spawn|setitem|create|delete|toggle> <type>");
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length == 1) {
            List<String> list = Arrays.asList("spawn", "setitem", "create", "delete", "toggle", "edit");
            for (String s : list) {
                if (s.toLowerCase().startsWith(args[0].toLowerCase())) result.add(s);
            }
        } else if (args.length == 2) {
            for (String s : configManager.getAltarTypes()) {
                if (s.toLowerCase().startsWith(args[1].toLowerCase())) result.add(s);
            }
        }
        return result;
    }
}
