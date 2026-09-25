package pl.smpcore.mechanics;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import pl.Main;

public class ShieldTweaksListener implements Listener {
    @EventHandler
    public void onShieldInteract(PlayerInteractEvent event) {
        if (!Main.getInstance().getConfig().getBoolean("shield_tweaks.5_tick_delay_fix", false)) return;

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Player player = event.getPlayer();
            if (player.getInventory().getItemInMainHand().getType() == Material.SHIELD ||
                    player.getInventory().getItemInOffHand().getType() == Material.SHIELD) {
                // Instantly make active item if crouching or holding
                if (!player.isHandRaised()) {
                    player.setShieldBlockingDelay(0);
                }
            }
        }
    }
}
