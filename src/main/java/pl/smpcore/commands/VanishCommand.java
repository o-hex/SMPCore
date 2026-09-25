package pl.smpcore.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import pl.Main;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class VanishCommand implements CommandExecutor, Listener {
    private static final Set<UUID> vanished = new HashSet<>();

    public static boolean isVanished(UUID uuid) {
        return vanished.contains(uuid);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("smpcore.vanish")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        UUID uuid = player.getUniqueId();
        if (vanished.contains(uuid)) {
            // Unvanish
            vanished.remove(uuid);
            for (Player other : Bukkit.getOnlinePlayers()) {
                other.showPlayer(Main.getInstance(), player);
                if (!other.hasPermission("smpcore.vanish")) {
                    other.sendMessage(ChatColor.YELLOW + player.getName() + " joined the game");
                }
            }
            if (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE) {
                player.setAllowFlight(false);
                player.setFlying(false);
            }
            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
            player.sendMessage(ChatColor.RED + "You are no longer vanished!");
        } else {
            // Vanish
            vanished.add(uuid);
            for (Player other : Bukkit.getOnlinePlayers()) {
                if (!other.hasPermission("smpcore.vanish")) {
                    other.hidePlayer(Main.getInstance(), player);
                    other.sendMessage(ChatColor.YELLOW + player.getName() + " left the game");
                }
            }
            player.setAllowFlight(true);
            player.setFlying(true);
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false));
            player.sendMessage(ChatColor.GREEN + "You are now vanished!");
        }

        return true;
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player && isVanished(event.getEntity().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player joining = event.getPlayer();
        if (!joining.hasPermission("smpcore.vanish")) {
            for (UUID uuid : vanished) {
                Player vPlayer = Bukkit.getPlayer(uuid);
                if (vPlayer != null && vPlayer.isOnline()) {
                    joining.hidePlayer(Main.getInstance(), vPlayer);
                }
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        vanished.remove(event.getPlayer().getUniqueId());
    }
}
