package pl.smpcore.mechanics;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import pl.Main;
import pl.smpcore.util.ItemBuilder;

public class GoldenHeadListener implements Listener {
    public static final String HEAD_NAME = ChatColor.GOLD + "Golden Head";

    public static ItemStack createGoldenHead() {
        return new ItemBuilder(Material.PLAYER_HEAD)
                .name(HEAD_NAME)
                .lore(ChatColor.GRAY + "A golden delicacy crafted from the head of a player.")
                .glowing(true)
                .build();
    }

    public static void registerRecipe() {
        NamespacedKey key = new NamespacedKey(Main.getInstance(), "golden_head");
        if (Bukkit.getRecipe(key) != null) return;

        ShapedRecipe recipe = new ShapedRecipe(key, createGoldenHead());
        recipe.shape("GGG", "GHG", "GGG");
        recipe.setIngredient('G', Material.GOLD_INGOT);
        recipe.setIngredient('H', Material.PLAYER_HEAD);
        Bukkit.addRecipe(recipe);
    }

    @EventHandler
    public void onEatHead(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.PLAYER_HEAD) return;

        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName() && meta.getDisplayName().equals(HEAD_NAME)) {
            event.setCancelled(true);
            Player player = event.getPlayer();

            item.setAmount(item.getAmount() - 1);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 1.0f, 1.0f);

            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1));
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 2400, 0));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 300, 1));
            player.sendMessage(ChatColor.GOLD + "You consumed a Golden Head!");
        }
    }
}
