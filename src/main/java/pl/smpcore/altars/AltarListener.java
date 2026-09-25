package pl.smpcore.altars;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class AltarListener implements Listener {
    private final AltarManager altarManager;
    private final Map<UUID, Long> clickCooldowns = new HashMap<>();

    public AltarListener(AltarManager altarManager) {
        this.altarManager = altarManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onAltarInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        Altar altar = altarManager.getAltarAt(block.getLocation());
        if (altar == null) return;

        if (!altar.isEnabled()) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();

        // Check ingredients
        List<AltarIngredient> missing = new ArrayList<>();
        for (AltarIngredient ing : altar.getIngredients()) {
            int count = 0;
            for (ItemStack stack : player.getInventory().getContents()) {
                if (stack != null && ing.matches(stack)) {
                    count += stack.getAmount();
                }
            }
            if (count < ing.getAmount()) {
                missing.add(new AltarIngredient(ing.getMaterial(), ing.getAmount() - count, ing.getCustomModelData()));
            }
        }

        if (!missing.isEmpty()) {
            long now = System.currentTimeMillis();
            Long last = clickCooldowns.get(player.getUniqueId());
            if (last == null || now - last > 1000L) {
                clickCooldowns.put(player.getUniqueId(), now);
                player.sendMessage("§c§lMissing items:");
                for (AltarIngredient m : missing) {
                    player.sendMessage("§7- §7" + m.getAmount() + "x §f" + m.getFormattedName());
                }
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
            return;
        }

        // Player has all ingredients! Consume if enabled
        if (altar.isConsumeMaterials()) {
            for (AltarIngredient ing : altar.getIngredients()) {
                int needed = ing.getAmount();
                for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
                    ItemStack stack = player.getInventory().getItem(slot);
                    if (stack != null && ing.matches(stack)) {
                        if (stack.getAmount() <= needed) {
                            needed -= stack.getAmount();
                            player.getInventory().setItem(slot, null);
                        } else {
                            stack.setAmount(stack.getAmount() - needed);
                            needed = 0;
                        }
                        if (needed <= 0) break;
                    }
                }
            }
        }

        // Give reward
        ItemStack reward = altar.getReward();
        if (reward != null && reward.getType() != Material.AIR) {
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(reward.clone());
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }

        // Strike lightning
        block.getWorld().strikeLightning(block.getLocation());

        // Play challenge complete sound
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

        // Broadcast craft message
        altarManager.broadcastCraft(player, altar);

        // Despawn and remove altar
        altarManager.removeAltar(block.getLocation(), true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Altar altar = altarManager.getAltarAt(event.getBlock().getLocation());
        if (altar != null) {
            if (!event.getPlayer().hasPermission("smpcore.saltar")) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.RED + "You cannot break an altar!");
            } else {
                altarManager.removeAltar(event.getBlock().getLocation(), false);
                event.getPlayer().sendMessage(ChatColor.GRAY + "Altar removed.");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(b -> altarManager.getAltarAt(b.getLocation()) != null);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(b -> altarManager.getAltarAt(b.getLocation()) != null);
    }
}
