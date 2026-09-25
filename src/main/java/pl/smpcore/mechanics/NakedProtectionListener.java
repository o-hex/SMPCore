package pl.smpcore.mechanics;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import pl.Main;
import pl.smpcore.cooldown.CooldownManager;

public class NakedProtectionListener implements Listener {
    private boolean isNaked(Player player) {
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor != null && armor.getType() != Material.AIR) {
                return false;
            }
        }
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand.getType() != Material.AIR) {
            String name = mainHand.getType().name();
            if (name.endsWith("_SWORD") || name.endsWith("_AXE") || name.equals("MACE") || name.equals("BOW") || name.equals("CROSSBOW") || name.equals("TRIDENT")) {
                return false;
            }
        }
        return true;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!Main.getInstance().getConfig().getBoolean("rules.naked_protection", false)) return;
        if (!(event.getEntity() instanceof Player)) return;

        Player victim = (Player) event.getEntity();
        if (event.getDamager() instanceof Player) {
            Player attacker = (Player) event.getDamager();

            // Attacker loses naked protection when attacking
            CooldownManager.setCooldown(attacker.getUniqueId(), "naked_combat", 30.0);

            if (isNaked(victim)) {
                if (!CooldownManager.hasCooldown(victim.getUniqueId(), "naked_combat")) {
                    event.setCancelled(true);
                    attacker.sendMessage(ChatColor.RED + "This player is naked and protected from attacks!");
                }
            }
        }
    }
}
