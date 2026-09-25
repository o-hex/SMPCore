package pl.smpcore.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class WorldTpCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("smpcore.worldtp")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /worldtp <world> [player]");
            return true;
        }

        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Console must specify a player: /worldtp <world> <player>");
                return true;
            }
            target = (Player) sender;
        }

        String worldName = args[0];
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
            File levelDat = new File(worldFolder, "level.dat");
            if (worldFolder.exists() && levelDat.exists()) {
                sender.sendMessage(ChatColor.GRAY + "Loading world '" + worldName + "' from disk...");
                world = new WorldCreator(worldName).createWorld();
            }
        }

        if (world == null) {
            sender.sendMessage(ChatColor.RED + "World '" + worldName + "' does not exist or could not be loaded.");
            return true;
        }

        target.teleport(world.getSpawnLocation());
        sender.sendMessage(ChatColor.GREEN + "Teleported " + target.getName() + " to world " + worldName);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> worlds = new ArrayList<>();
            for (World w : Bukkit.getWorlds()) {
                if (w.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    worlds.add(w.getName());
                }
            }
            File container = Bukkit.getWorldContainer();
            File[] files = container.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory() && new File(f, "level.dat").exists()) {
                        if (!worlds.contains(f.getName()) && f.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                            worlds.add(f.getName());
                        }
                    }
                }
            }
            return worlds;
        }
        if (args.length == 2) {
            List<String> players = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    players.add(p.getName());
                }
            }
            return players;
        }
        return new ArrayList<>();
    }
}
