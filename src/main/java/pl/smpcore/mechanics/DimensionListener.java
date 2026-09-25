package pl.smpcore.mechanics;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.PortalCreateEvent;
import pl.Main;
import pl.smpcore.cooldown.CooldownManager;

public class DimensionListener implements Listener {

    public static boolean isNetherDisabled() {
        FileConfiguration cfg = Main.getInstance().getConfig();
        if (cfg.contains("rules.disable_nether")) {
            return cfg.getBoolean("rules.disable_nether", false);
        }
        return !cfg.getBoolean("dimensions.allow_nether", true);
    }

    public static boolean isEndDisabled() {
        FileConfiguration cfg = Main.getInstance().getConfig();
        if (cfg.contains("rules.disable_end")) {
            return cfg.getBoolean("rules.disable_end", false);
        }
        return !cfg.getBoolean("dimensions.allow_end", true);
    }

    private void sendNetherMsg(Player player) {
        if (!CooldownManager.hasCooldown(player.getUniqueId(), "nether_block_msg")) {
            CooldownManager.setCooldown(player.getUniqueId(), "nether_block_msg", 3.0);
            player.sendMessage(ChatColor.RED + "The Nether is disabled on this server!");
        }
    }

    private void sendEndMsg(Player player) {
        if (!CooldownManager.hasCooldown(player.getUniqueId(), "end_block_msg")) {
            CooldownManager.setCooldown(player.getUniqueId(), "end_block_msg", 3.0);
            player.sendMessage(ChatColor.RED + "The End is disabled on this server!");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPortal(PlayerPortalEvent event) {
        Player player = event.getPlayer();

        if (event.getCause() == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL && isNetherDisabled()) {
            event.setCancelled(true);
            sendNetherMsg(player);
            return;
        }
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.END_PORTAL && isEndDisabled()) {
            event.setCancelled(true);
            sendEndMsg(player);
            return;
        }

        if (event.getTo() != null && event.getTo().getWorld() != null) {
            World.Environment toEnv = event.getTo().getWorld().getEnvironment();
            if (toEnv == World.Environment.NETHER && isNetherDisabled()) {
                event.setCancelled(true);
                sendNetherMsg(player);
            } else if (toEnv == World.Environment.THE_END && isEndDisabled()) {
                event.setCancelled(true);
                sendEndMsg(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();

        if (event.getCause() == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL && isNetherDisabled()) {
            event.setCancelled(true);
            sendNetherMsg(player);
            return;
        }
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.END_PORTAL && isEndDisabled()) {
            event.setCancelled(true);
            sendEndMsg(player);
            return;
        }

        if (event.getTo() != null && event.getTo().getWorld() != null) {
            World.Environment toEnv = event.getTo().getWorld().getEnvironment();
            if (toEnv == World.Environment.NETHER && isNetherDisabled()) {
                event.setCancelled(true);
                sendNetherMsg(player);
            } else if (toEnv == World.Environment.THE_END && isEndDisabled()) {
                event.setCancelled(true);
                sendEndMsg(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPortalCreate(PortalCreateEvent event) {
        if (event.getReason() == PortalCreateEvent.CreateReason.NETHER_PAIR && isNetherDisabled()) {
            event.setCancelled(true);
        } else if (event.getReason() == PortalCreateEvent.CreateReason.END_PLATFORM && isEndDisabled()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            if (event.getClickedBlock().getType() == Material.END_PORTAL_FRAME && isEndDisabled()) {
                if (event.getItem() != null && event.getItem().getType() == Material.ENDER_EYE) {
                    event.setCancelled(true);
                    sendEndMsg(event.getPlayer());
                }
            }
        }
    }
}
