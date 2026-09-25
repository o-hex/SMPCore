package pl.smpcore.commands;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.Main;
import pl.smpcore.settings.SettingsGUI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BanItemCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0) {
            String arg = args[0].toLowerCase();
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (!player.hasPermission("smpcore.banitem")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                    return true;
                }
                if (arg.equals("potions") || arg.equals("potion")) {
                    SettingsGUI.openPotionBans(player);
                    return true;
                }
                if (arg.equals("enchants") || arg.equals("enchant")) {
                    SettingsGUI.openEnchantBans(player);
                    return true;
                }
                if (arg.equals("tipped") || arg.equals("arrows")) {
                    SettingsGUI.openTippedArrowBans(player);
                    return true;
                }
            }

            Material mat = Material.matchMaterial(args[0].toUpperCase());
            if (mat != null) {
                toggleBan(sender, mat);
                return true;
            }
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Usage: /banitem <material>");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("smpcore.banitem")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "You need to be holding an item to ban/unban it, or specify a material!");
            return true;
        }

        toggleBan(player, hand.getType());
        return true;
    }

    private void toggleBan(CommandSender sender, Material material) {
        FileConfiguration config = Main.getInstance().getConfig();
        String path = "banned-items." + material.name();
        boolean banned = config.getBoolean(path, false);
        config.set(path, !banned);
        Main.getInstance().saveConfig();

        if (!banned) {
            sender.sendMessage(ChatColor.RED + "Banned " + ChatColor.WHITE + material.name());
        } else {
            sender.sendMessage(ChatColor.GREEN + "Unbanned " + ChatColor.WHITE + material.name());
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>(Arrays.asList("potions", "enchants", "tipped"));
            for (Material m : Material.values()) {
                if (m.isItem() && m.name().toLowerCase().startsWith(args[0].toLowerCase())) {
                    suggestions.add(m.name().toLowerCase());
                    if (suggestions.size() > 50) break;
                }
            }
            return suggestions;
        }
        return new ArrayList<>();
    }
}
