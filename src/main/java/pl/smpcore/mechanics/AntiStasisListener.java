package pl.smpcore.mechanics;

import org.bukkit.entity.EnderPearl;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.scheduler.BukkitRunnable;
import pl.Main;

public class AntiStasisListener implements Listener {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLaunch(ProjectileLaunchEvent event) {
        if (!Main.getInstance().getConfig().getBoolean("rules.anti_stasis_chamber", false)) return;

        if (event.getEntity() instanceof EnderPearl) {
            EnderPearl pearl = (EnderPearl) event.getEntity();
            int seconds = Main.getInstance().getConfig().getInt("rules.anti_stasis_chamber_pearl_remove_after_seconds", 10);
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (pearl.isValid() && !pearl.isDead()) {
                        pearl.remove();
                    }
                }
            }.runTaskLater(Main.getInstance(), seconds * 20L);
        }
    }
}
