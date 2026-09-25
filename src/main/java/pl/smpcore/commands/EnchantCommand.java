package pl.smpcore.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EnchantCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("smpcore.enchant")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /enchant <player|@s|@a> <enchantment> <level|remove> [hand|armor]");
            return true;
        }

        List<Player> targets = new ArrayList<>();
        String targetArg = args[0];
        if (targetArg.equalsIgnoreCase("@s")) {
            if (sender instanceof Player) targets.add((Player) sender);
            else {
                sender.sendMessage(ChatColor.RED + "Only players can use @s.");
                return true;
            }
        } else if (targetArg.equalsIgnoreCase("@a")) {
            targets.addAll(Bukkit.getOnlinePlayers());
        } else {
            Player p = Bukkit.getPlayer(targetArg);
            if (p != null) targets.add(p);
            else {
                sender.sendMessage(ChatColor.RED + "Player not found: " + targetArg);
                return true;
            }
        }

        String enchantName = args[1].toLowerCase().replace("minecraft:", "");
        Enchantment ench = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(enchantName));
        if (ench == null) {
            sender.sendMessage(ChatColor.RED + "Unknown enchantment: " + args[1]);
            return true;
        }

        boolean remove = args[2].equalsIgnoreCase("remove");
        int level = 1;
        if (!remove) {
            try {
                level = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid level: " + args[2]);
                return true;
            }
        }

        String slot = args.length > 3 ? args[3].toLowerCase() : "hand";

        for (Player target : targets) {
            if (slot.equals("armor")) {
                ItemStack[] armor = target.getInventory().getArmorContents();
                for (ItemStack piece : armor) {
                    if (piece != null && !piece.getType().isAir()) {
                        if (remove) piece.removeEnchantment(ench);
                        else piece.addUnsafeEnchantment(ench, level);
                    }
                }
                target.getInventory().setArmorContents(armor);
            } else {
                ItemStack hand = target.getInventory().getItemInMainHand();
                if (hand != null && !hand.getType().isAir()) {
                    if (remove) hand.removeEnchantment(ench);
                    else hand.addUnsafeEnchantment(ench, level);
                }
            }
        }

        if (remove) {
            sender.sendMessage(ChatColor.GREEN + "Removed " + enchantName + " on " + targetArg);
        } else {
            sender.sendMessage(ChatColor.GREEN + "Applied " + enchantName + " " + level + " on " + targetArg);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>(Arrays.asList("@s", "@a"));
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[0].toLowerCase())) list.add(p.getName());
            }
            return list;
        }
        if (args.length == 2) {
            List<String> list = new ArrayList<>();
            for (Enchantment e : Registry.ENCHANTMENT) {
                String key = e.getKey().getKey();
                if (key.toLowerCase().startsWith(args[1].toLowerCase())) list.add(key);
            }
            return list;
        }
        if (args.length == 3) {
            return Arrays.asList("1", "2", "3", "4", "5", "remove");
        }
        if (args.length == 4) {
            return Arrays.asList("hand", "armor");
        }
        return new ArrayList<>();
    }
}
