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
        if (!Main.getInstance().getConfig().getBoolean("rules.better_pearl_catching_hitbox", true)) return;
        if (!(event.getEntity() instanceof EnderPearl)) return;
        if (!(event.getEntity().getShooter() instanceof Player)) return;

        EnderPearl pearl = (EnderPearl) event.getEntity();
        Player player = (Player) pearl.getShooter();

        double hitboxExpansion = Main.getInstance().getConfig().getDouble("rules.better_pearl_catching_hitbox_size", 0.75);
        if (hitboxExpansion > 0.0) {
            Vector dir = player.getLocation().getDirection().normalize().multiply(0.2);
            pearl.setVelocity(pearl.getVelocity().add(dir));
        }
    }
}
