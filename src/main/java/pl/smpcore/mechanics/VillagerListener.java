package pl.smpcore.mechanics;

import io.papermc.paper.event.entity.EntityMoveEvent;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;
import pl.Main;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VillagerListener implements Listener {
    private static final Set<UUID> anchoredVillagers = ConcurrentHashMap.newKeySet();

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVillagerDamage(EntityDamageByEntityEvent event) {
        if (!Main.getInstance().getConfig().getBoolean("rules.ban_killing_villagers", false)) return;

        if (event.getEntityType() == EntityType.VILLAGER) {
            if (event.getDamager() instanceof Player) {
                event.setCancelled(true);
                ((Player) event.getDamager()).sendMessage(ChatColor.RED + "Villagers cannot be harmed on this server!");
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onTradeOpen(InventoryOpenEvent event) {
        if (!Main.getInstance().getConfig().getBoolean("rules.infinite_restock", false)) return;

        if (event.getInventory().getType() == InventoryType.MERCHANT && event.getInventory().getHolder() instanceof Villager) {
            Villager villager = (Villager) event.getInventory().getHolder();
            List<MerchantRecipe> recipes = new ArrayList<>();
            for (MerchantRecipe r : villager.getRecipes()) {
                MerchantRecipe copy = new MerchantRecipe(r.getResult(), r.getMaxUses());
                copy.setIngredients(r.getIngredients());
                copy.setUses(0);
                copy.setExperienceReward(r.hasExperienceReward());
                copy.setPriceMultiplier(r.getPriceMultiplier());
                copy.setDemand(r.getDemand());
                recipes.add(copy);
            }
            villager.setRecipes(recipes);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onShovelClickVillager(PlayerInteractAtEntityEvent event) {
        if (!Main.getInstance().getConfig().getBoolean("rules.clickVillager", false)) return;
        if (!(event.getRightClicked() instanceof Villager)) return;

        Villager villager = (Villager) event.getRightClicked();
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();

        if (hand.getType().name().endsWith("_SHOVEL")) {
            event.setCancelled(true);
            UUID id = villager.getUniqueId();
            if (anchoredVillagers.contains(id)) {
                anchoredVillagers.remove(id);
                villager.setAI(true);
                player.sendMessage(ChatColor.GREEN + "Unanchored villager!");
            } else {
                anchoredVillagers.add(id);
                villager.setAI(false);
                player.sendMessage(ChatColor.GOLD + "Anchored villager! It will not move.");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVillagerMove(EntityMoveEvent event) {
        if (!Main.getInstance().getConfig().getBoolean("rules.clickVillager_anchor", false)) return;
        if (anchoredVillagers.contains(event.getEntity().getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
