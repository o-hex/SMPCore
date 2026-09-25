package pl.smpcore.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import pl.Main;

import java.util.ArrayList;
import java.util.List;

public class SpawnCommands implements CommandExecutor, TabCompleter, Listener {
    private static void saveLocation(String path, Location loc) {
        FileConfiguration config = Main.getInstance().getConfig();
        config.set(path + ".world", loc.getWorld().getName());
        config.set(path + ".x", loc.getX());
        config.set(path + ".y", loc.getY());
        config.set(path + ".z", loc.getZ());
        config.set(path + ".yaw", loc.getYaw());
        config.set(path + ".pitch", loc.getPitch());
        Main.getInstance().saveConfig();
    }

    private static Location getLocation(String path) {
        FileConfiguration config = Main.getInstance().getConfig();
        if (!config.contains(path + ".world")) return null;
        String wName = config.getString(path + ".world");
        World world = Bukkit.getWorld(wName);
        if (world == null) return null;
        double x = config.getDouble(path + ".x");
        double y = config.getDouble(path + ".y");
        double z = config.getDouble(path + ".z");
        float yaw = (float) config.getDouble(path + ".yaw");
        float pitch = (float) config.getDouble(path + ".pitch");
        return new Location(world, x, y, z, yaw, pitch);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        String cmd = command.getName().toLowerCase();

        if (cmd.equals("setcustomspawn")) {
            if (!player.hasPermission("smpcore.setcustomspawn")) {
                player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }
            saveLocation("custom_spawn", player.getLocation());
            player.sendMessage(ChatColor.GREEN + "Custom spawn location set!");
            return true;
        }

        if (cmd.equals("setrespawnspawn")) {
            if (!player.hasPermission("smpcore.setrespawnspawn")) {
                player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }
            saveLocation("respawn_spawn", player.getLocation());
            player.sendMessage(ChatColor.GREEN + "Custom respawn location set!");
            return true;
        }

        return false;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onJoin(PlayerJoinEvent event) {
        Location spawn = getLocation("custom_spawn");
        if (spawn != null) {
            Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                if (event.getPlayer().isOnline()) {
                    event.getPlayer().teleport(spawn);
                }
            }, 5L);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        Location respawn = getLocation("respawn_spawn");
        if (respawn != null) {
            event.setRespawnLocation(respawn);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return new ArrayList<>();
    }
}
