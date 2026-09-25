package pl.smpcore.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ReplyCommand implements CommandExecutor, Listener {
    private static final Map<UUID, UUID> lastMessaged = new ConcurrentHashMap<>();

    public static void setReplyTarget(UUID player, UUID target) {
        lastMessaged.put(player, target);
        lastMessaged.put(target, player);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("smpcore.reply")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Usage: /reply <message>");
            return true;
        }

        UUID targetId = lastMessaged.get(player.getUniqueId());
        if (targetId == null) {
            player.sendMessage(ChatColor.RED + "You have nobody to reply to!");
            return true;
        }

        Player target = Bukkit.getPlayer(targetId);
        if (target == null || !target.isOnline()) {
            player.sendMessage(ChatColor.RED + "That player is no longer online!");
            return true;
        }

        String msg = String.join(" ", args);
        player.sendMessage(ChatColor.GRAY + "[me -> " + target.getName() + "] " + ChatColor.WHITE + msg);
        target.sendMessage(ChatColor.GRAY + "[" + player.getName() + " -> me] " + ChatColor.WHITE + msg);
        setReplyTarget(player.getUniqueId(), target.getUniqueId());
        return true;
    }

    @EventHandler
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String msg = event.getMessage();
        String[] parts = msg.split(" ");
        if (parts.length < 3) return;

        String cmd = parts[0].toLowerCase();
        if (cmd.equals("/msg") || cmd.equals("/tell") || cmd.equals("/w")) {
            Player target = Bukkit.getPlayer(parts[1]);
            if (target != null && target.isOnline()) {
                setReplyTarget(event.getPlayer().getUniqueId(), target.getUniqueId());
            }
        }
    }
}
