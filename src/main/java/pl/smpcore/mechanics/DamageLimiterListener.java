package pl.smpcore.mechanics;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import pl.Main;

public class DamageLimiterListener implements Listener {
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamageLimit(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        FileConfiguration config = Main.getInstance().getConfig();

        // Fall damage limiter
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            int max = config.getInt("rules.fall_damage_limiter", 0);
            if (max > 0 && event.getDamage() > max) {
                event.setDamage(max);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageLimit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        FileConfiguration config = Main.getInstance().getConfig();
        Entity damager = event.getDamager();

        // Arrow damage limiter
        if (damager instanceof Arrow) {
            int max = config.getInt("rules.arrow_damage_limiter", 0);
            if (max > 0 && event.getDamage() > max) {
                event.setDamage(max);
            }
        }

        // Crystal damage limiter
        if (damager instanceof EnderCrystal) {
            int max = config.getInt("rules.crystal_damage_limiter", 0);
            if (max > 0 && event.getDamage() > max) {
                event.setDamage(max);
            }
        }

        // TNT damage limiter
        if (damager instanceof TNTPrimed) {
            int max = config.getInt("rules.tnt_damage_limiter", 0);
            if (max > 0 && event.getDamage() > max) {
                event.setDamage(max);
            }
        }

        // Cart damage limiter
        if (damager instanceof ExplosiveMinecart) {
            int max = config.getInt("rules.cart_damage_limiter", 0);
            if (max > 0 && event.getDamage() > max) {
                event.setDamage(max);
            }
        }

        // Mace damage limiter
        if (damager instanceof Player) {
            Player attacker = (Player) damager;
            if (attacker.getInventory().getItemInMainHand() != null &&
                    attacker.getInventory().getItemInMainHand().getType().name().equals("MACE")) {
                int max = config.getInt("rules.mace_damage_limiter", 0);
                if (max > 0 && event.getDamage() > max) {
                    event.setDamage(max);
                }
            }
        }
    }
}
