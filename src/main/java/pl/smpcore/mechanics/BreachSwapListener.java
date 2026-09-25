package pl.smpcore.mechanics;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import pl.Main;

public class BreachSwapListener implements Listener {
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreachDamage(EntityDamageByEntityEvent event) {
        if (!Main.getInstance().getConfig().getBoolean("rules.ban_breach_swapping", false)) return;
        if (!(event.getDamager() instanceof Player)) return;

        Player attacker = (Player) event.getDamager();
        ItemStack hand = attacker.getInventory().getItemInMainHand();
        if (hand.getType() != Material.MACE) return;

        Enchantment breach = Registry.ENCHANTMENT.get(NamespacedKey.minecraft("breach"));
        if (breach != null && hand.containsEnchantment(breach)) {
            if (attacker.getFallDistance() < 1.5f) {
                event.setCancelled(true);
                attacker.sendMessage(ChatColor.RED + "Breach swapping is not allowed on this server");
            }
        }
    }
}
