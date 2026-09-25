package pl.smpcore.mechanics;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import pl.Main;
import pl.smpcore.cooldown.CooldownManager;

public class AntiDrainingListener implements Listener {
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (!Main.getInstance().getConfig().getBoolean("rules.anti_draining", true)) return;

        Player player = event.getPlayer();
        if (CooldownManager.hasCooldown(player.getUniqueId(), "anti_draining")) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You cannot drain fluids that quickly!");
            return;
        }

        // Set 0.5s cooldown
        CooldownManager.setCooldown(player.getUniqueId(), "anti_draining", 0.5);
    }
}
