package pl.smpcore.mechanics;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.scheduler.BukkitRunnable;
import pl.Main;

public class CobwebDecayListener implements Listener {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlaceCobweb(BlockPlaceEvent event) {
        if (!Main.getInstance().getConfig().getBoolean("rules.cobweb_decay", false)) return;

        Block block = event.getBlockPlaced();
        if (block.getType() == Material.COBWEB) {
            int delaySeconds = Main.getInstance().getConfig().contains("rules.cobweb_decay_seconds")
                    ? Main.getInstance().getConfig().getInt("rules.cobweb_decay_seconds", 10)
                    : Main.getInstance().getConfig().getInt("rules.cobweb_decay_time", 20);
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (block.getType() == Material.COBWEB) {
                        block.setType(Material.AIR);
                    }
                }
            }.runTaskLater(Main.getInstance(), delaySeconds * 20L);
        }
    }
}
