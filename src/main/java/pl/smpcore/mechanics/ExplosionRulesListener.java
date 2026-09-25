package pl.smpcore.mechanics;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import pl.Main;

public class ExplosionRulesListener implements Listener {
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCrystalPlace(EntityPlaceEvent event) {
        if (event.getEntity() instanceof EnderCrystal) {
            FileConfiguration config = Main.getInstance().getConfig();
            if (config.getBoolean("rules.ban_crystal_pvp", false)) {
                event.setCancelled(true);
                if (event.getPlayer() != null) {
                    event.getPlayer().sendMessage(ChatColor.RED + "End Crystal PvP is disabled on this server!");
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCrystalDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof EnderCrystal && event.getEntity() instanceof Player) {
            FileConfiguration config = Main.getInstance().getConfig();
            if (config.getBoolean("rules.ban_crystal_pvp", false)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBedInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        FileConfiguration config = Main.getInstance().getConfig();

        // Bed bombing in Nether / End
        if (block.getType().name().endsWith("_BED")) {
            World.Environment env = block.getWorld().getEnvironment();
            if (env == World.Environment.NETHER || env == World.Environment.THE_END) {
                if (config.getBoolean("rules.ban_bed_bombing", false)) {
                    event.setCancelled(true);
                    event.getPlayer().sendMessage(ChatColor.RED + "Bed bombing is disabled in this dimension!");
                }
            }
        }

        // Anchor exploding in Overworld
        if (block.getType() == Material.RESPAWN_ANCHOR) {
            World.Environment env = block.getWorld().getEnvironment();
            if (env == World.Environment.NORMAL) {
                if (config.getBoolean("rules.ban_anchor_exploding", false)) {
                    event.setCancelled(true);
                    event.getPlayer().sendMessage(ChatColor.RED + "Respawn anchor exploding is disabled in the Overworld!");
                }
            }
        }
    }

    private boolean isCartsBanned() {
        return Main.getInstance().getConfig().contains("rules.ban_carts")
                ? Main.getInstance().getConfig().getBoolean("rules.ban_carts", false)
                : Main.getInstance().getConfig().getBoolean("rules.ban_cart_exploding", false);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCartPlace(PlayerInteractEvent event) {
        if (event.getItem() != null && event.getItem().getType() == Material.TNT_MINECART) {
            if (isCartsBanned()) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.RED + "TNT minecarts are disabled on this server!");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCartDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof ExplosiveMinecart && event.getEntity() instanceof Player) {
            if (isCartsBanned()) {
                event.setCancelled(true);
            }
        }
    }
}
