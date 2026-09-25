package pl.smpcore.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import pl.Main;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WhitelistPlusCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("smpcore.whitelistplus") && !sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /whitelistplus <add|remove>");
            return true;
        }

        String sub = args[0].toLowerCase();
        if (sub.equals("add")) {
            List<String> list = Main.getInstance().getConfig().getStringList("whitelist");
            if (list.isEmpty()) {
                sender.sendMessage(ChatColor.RED + "No players found in whitelist.yml!");
                return true;
            }
            for (String name : list) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(name);
                op.setWhitelisted(true);
            }
            Bukkit.setWhitelist(true);
            sender.sendMessage(ChatColor.GREEN + "Whitelisted all players from whitelist.yml!");
            return true;
        }

        if (sub.equals("remove")) {
            for (OfflinePlayer op : Bukkit.getWhitelistedPlayers()) {
                op.setWhitelisted(false);
            }
            sender.sendMessage(ChatColor.RED + "Removed whitelist for all players in whitelist.yml!");
            return true;
        }

        sender.sendMessage(ChatColor.RED + "Usage: /whitelistplus <add|remove>");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("add", "remove");
        }
        return new ArrayList<>();
    }
}
