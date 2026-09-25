package pl.smpcore.mechanics;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import pl.Main;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AfkProtectionListener implements Listener {
    private static final Map<UUID, Location> afkPlayers = new ConcurrentHashMap<>();

    public static boolean isAfk(Player player) {
        return player != null && afkPlayers.containsKey(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!Main.getInstance().getConfig().getBoolean("rules.afk_protection", false)) return;

        if (event.getMessage().trim().equalsIgnoreCase("afk")) {
            Player player = event.getPlayer();
            afkPlayers.put(player.getUniqueId(), player.getLocation());
            player.sendMessage(ChatColor.YELLOW + "You are now AFK and protected from damage! Move to cancel.");
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (isAfk(player)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location initial = afkPlayers.get(player.getUniqueId());
        if (initial != null) {
            if (event.getTo() != null && event.getTo().distanceSquared(initial) > 0.05) {
                afkPlayers.remove(player.getUniqueId());
                player.sendMessage(ChatColor.GREEN + "You are no longer AFK.");
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        afkPlayers.remove(event.getPlayer().getUniqueId());
    }
}
