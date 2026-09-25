package pl.smpcore.mechanics;

import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.util.Vector;
import pl.Main;

public class PearlCatchListener implements Listener {
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPearlLaunch(ProjectileLaunchEvent event) {
        org.bukkit.configuration.file.FileConfiguration cfg = Main.getInstance().getConfig();
        boolean enabled;
        double hitboxExpansion = 0.75;
        if (cfg.isDouble("rules.better_pearl_catching_hitbox") || cfg.isInt("rules.better_pearl_catching_hitbox")) {
            hitboxExpansion = cfg.getDouble("rules.better_pearl_catching_hitbox", 0.75);
            enabled = hitboxExpansion > 0.0;
        } else {
            enabled = cfg.getBoolean("rules.better_pearl_catching_hitbox", true);
            hitboxExpansion = cfg.getDouble("rules.better_pearl_catching_hitbox_size", 0.75);
        }
        if (!enabled) return;
        if (!(event.getEntity() instanceof EnderPearl)) return;
        if (!(event.getEntity().getShooter() instanceof Player)) return;

        EnderPearl pearl = (EnderPearl) event.getEntity();
        Player player = (Player) pearl.getShooter();

        if (hitboxExpansion > 0.0) {
            Vector dir = player.getLocation().getDirection().normalize().multiply(0.2);
            pearl.setVelocity(pearl.getVelocity().add(dir));
        }
    }
}
