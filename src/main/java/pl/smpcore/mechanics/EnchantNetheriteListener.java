package pl.smpcore.mechanics;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import pl.Main;

public class EnchantNetheriteListener implements Listener {
    private boolean isNetherite(Material mat) {
        if (mat == null) return false;
        String name = mat.name();
        return name.startsWith("NETHERITE_");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onNetheriteClick(InventoryClickEvent event) {
        if (!Main.getInstance().getConfig().getBoolean("rules.ban_netherite", true)) return;
        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        if ((current != null && isNetherite(current.getType())) || (cursor != null && isNetherite(cursor.getType()))) {
            if (event.getRawSlot() < 9 || (event.getSlot() >= 36 && event.getSlot() <= 39)) { // Armor slots
                event.setCancelled(true);
                if (event.getWhoClicked() instanceof Player) {
                    ((Player) event.getWhoClicked()).sendMessage(ChatColor.RED + "Netherite is banned on this server!");
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamageEnchantCheck(EntityDamageByEntityEvent event) {
        FileConfiguration config = Main.getInstance().getConfig();

        // Check sharpness on weapon
        if (event.getDamager() instanceof Player) {
            Player attacker = (Player) event.getDamager();
            ItemStack weapon = attacker.getInventory().getItemInMainHand();
            int maxSharp = config.getInt("rules.sharpness", 5);
            if (weapon != null && weapon.containsEnchantment(Enchantment.SHARPNESS)) {
                if (weapon.getEnchantmentLevel(Enchantment.SHARPNESS) > maxSharp) {
                    weapon.addEnchantment(Enchantment.SHARPNESS, maxSharp);
                }
            }
        }

        // Check protection on armor
        if (event.getEntity() instanceof Player) {
            Player victim = (Player) event.getEntity();
            int maxProt = config.getInt("rules.protection", 4);
            for (ItemStack piece : victim.getInventory().getArmorContents()) {
                if (piece != null && piece.containsEnchantment(Enchantment.PROTECTION)) {
                    if (piece.getEnchantmentLevel(Enchantment.PROTECTION) > maxProt) {
                        piece.addEnchantment(Enchantment.PROTECTION, maxProt);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onShootBow(EntityShootBowEvent event) {
        FileConfiguration config = Main.getInstance().getConfig();

        if (event.getProjectile() instanceof Arrow) {
            Arrow arrow = (Arrow) event.getProjectile();
            ItemStack arrowItem = event.getConsumable();

            if (arrowItem != null && arrowItem.getType() == Material.TIPPED_ARROW) {
                if (config.getBoolean("rules.ban_all_tipped_arrows", false)) {
                    event.setCancelled(true);
                    if (event.getEntity() instanceof Player) {
                        ((Player) event.getEntity()).sendMessage(ChatColor.RED + "Tipped arrows are banned on this server!");
                    }
                    return;
                }

                if (arrowItem.getItemMeta() instanceof PotionMeta) {
                    PotionMeta pm = (PotionMeta) arrowItem.getItemMeta();
                    PotionType type = pm.getBasePotionType();
                    if (type != null) {
                        String typeName = type.name().toLowerCase();
                        if (config.getBoolean("banned-tipped." + typeName, false)) {
                            event.setCancelled(true);
                            if (event.getEntity() instanceof Player) {
                                ((Player) event.getEntity()).sendMessage(ChatColor.RED + "This tipped arrow type is banned!");
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPotionConsume(PlayerItemConsumeEvent event) {
        if (event.getItem().getType() == Material.POTION || event.getItem().getType() == Material.SPLASH_POTION) {
            FileConfiguration config = Main.getInstance().getConfig();
            if (event.getItem().getItemMeta() instanceof PotionMeta) {
                PotionMeta pm = (PotionMeta) event.getItem().getItemMeta();

                // Ban tier 2 potions
                if (config.getBoolean("rules.ban_tier_2_potions", false)) {
                    PotionType type = pm.getBasePotionType();
                    if (type != null && type.name().startsWith("STRONG_")) {
                        event.setCancelled(true);
                        event.getPlayer().sendMessage(ChatColor.RED + "Tier 2 potions are banned on this server!");
                        return;
                    }
                }

                // Check banned effects
                for (PotionEffect effect : pm.getCustomEffects()) {
                    String effName = effect.getType().getName().toLowerCase();
                    if (config.getBoolean("banned-tier1-effects." + effName, false)) {
                        event.setCancelled(true);
                        event.getPlayer().sendMessage(ChatColor.RED + "This potion effect is banned!");
                        return;
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBrew(BrewEvent event) {
        // Block brewing banned potions if configured
        FileConfiguration config = Main.getInstance().getConfig();
        if (config.getBoolean("rules.ban_tier_2_potions", false)) {
            ItemStack ingredient = event.getContents().getIngredient();
            if (ingredient != null && ingredient.getType() == Material.GLOWSTONE_DUST) {
                event.setCancelled(true);
            }
        }
    }
}
