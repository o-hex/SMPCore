package pl.smpcore.commands;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import pl.Main;

public class SBroadcastCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("smpcore.sbroadcast")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "You need to add a message");
            return true;
        }

        String message = String.join(" ", args);
        String format = Main.getInstance().getConfig().getString("rules.broadcast_message", "§4[📢 ʙʀᴏᴀᴅᴄᴀꜱᴛ] <message>");
        String broadcast = format.replace("<message>", message);
        Bukkit.broadcast(Component.text(broadcast));
        return true;
    }
}
