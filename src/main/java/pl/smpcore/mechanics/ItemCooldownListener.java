package pl.smpcore.mechanics;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerRiptideEvent;
import pl.Main;

public class ItemCooldownListener implements Listener {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        FileConfiguration config = Main.getInstance().getConfig();

        if (event.getItem().getType() == Material.GOLDEN_APPLE || event.getItem().getType() == Material.ENCHANTED_GOLDEN_APPLE) {
            double cd = config.getDouble("rules.gap", 0.0);
            if (cd > 0.0) {
                int ticks = (int) Math.round(cd * 20.0);
                player.setCooldown(Material.GOLDEN_APPLE, ticks);
                player.setCooldown(Material.ENCHANTED_GOLDEN_APPLE, ticks);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player)) return;
        Player player = (Player) event.getEntity().getShooter();
        FileConfiguration config = Main.getInstance().getConfig();

        String type = event.getEntity().getType().name();
        if (type.equals("ENDER_PEARL")) {
            double cd = config.getDouble("rules.ender_pearl", 0.0);
            if (cd > 0.0) {
                player.setCooldown(Material.ENDER_PEARL, (int) Math.round(cd * 20.0));
            }
        } else if (type.equals("WIND_CHARGE")) {
            double cd = config.getDouble("rules.wind_charge", 0.0);
            if (cd > 0.0) {
                player.setCooldown(Material.WIND_CHARGE, (int) Math.round(cd * 20.0));
            }
        } else if (type.equals("TRIDENT")) {
            double cd = config.getDouble("rules.trident", 0.0);
            if (cd > 0.0) {
                player.setCooldown(Material.TRIDENT, (int) Math.round(cd * 20.0));
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRiptide(PlayerRiptideEvent event) {
        double cd = Main.getInstance().getConfig().getDouble("rules.trident", 0.0);
        if (cd > 0.0) {
            event.getPlayer().setCooldown(Material.TRIDENT, (int) Math.round(cd * 20.0));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player player = (Player) event.getDamager();
        FileConfiguration config = Main.getInstance().getConfig();

        if (player.getInventory().getItemInMainHand().getType() == Material.MACE) {
            double cd = config.getDouble("rules.mace", 0.0);
            if (cd > 0.0) {
                player.setCooldown(Material.MACE, (int) Math.round(cd * 20.0));
            }
        }
    }
}
