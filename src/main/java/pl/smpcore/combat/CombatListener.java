package pl.smpcore.combat;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import pl.Main;
import pl.smpcore.cooldown.CooldownManager;

import java.util.Arrays;
import java.util.List;

public class CombatListener implements Listener {
    private static final List<String> DEFAULT_ALLOWED_COMMANDS = Arrays.asList(
            "/reply", "/r", "/msg", "/tell", "/w"
    );

    private boolean isWorldCombatEnabled(String worldName) {
        List<String> worlds = Main.getInstance().getConfig().getStringList("combat_system_world");
        if (worlds.isEmpty() || worlds.contains("ALL")) return true;
        return worlds.contains(worldName);
    }

    private Player resolveAttacker(Entity damager) {
        if (damager instanceof Player) {
            return (Player) damager;
        }
        if (damager instanceof Projectile) {
            Projectile proj = (Projectile) damager;
            if (proj.getShooter() instanceof Player) {
                return (Player) proj.getShooter();
            }
        }
        if (damager instanceof TNTPrimed) {
            TNTPrimed tnt = (TNTPrimed) damager;
            if (tnt.getSource() instanceof Player) {
                return (Player) tnt.getSource();
            }
        }
        return null;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCombatDamage(EntityDamageByEntityEvent event) {
        if (!Main.getInstance().getConfig().getBoolean("rules.combat_system", true)) {
            return;
        }
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player victim = (Player) event.getEntity();
        if (!isWorldCombatEnabled(victim.getWorld().getName())) {
            return;
        }

        Player attacker = resolveAttacker(event.getDamager());
        if (attacker == null || attacker.equals(victim)) {
            return;
        }

        int tagTime = Main.getInstance().getConfig().getInt("config.combat_tag_time", 30);
        String rawMsg = Main.getInstance().getConfig().getString("rules.combat_message", "§cYou are now in combat for §f§l<combat_time>");
        String combatMsg = rawMsg.replace("<combat_time>", tagTime + "s");

        if (!CooldownManager.isCombatTagged(attacker.getUniqueId()) && !combatMsg.isEmpty()) {
            attacker.sendMessage(combatMsg);
        }
        if (!CooldownManager.isCombatTagged(victim.getUniqueId()) && !combatMsg.isEmpty()) {
            victim.sendMessage(combatMsg);
        }

        CooldownManager.setCombatTagged(attacker.getUniqueId(), tagTime);
        CooldownManager.setCombatTagged(victim.getUniqueId(), tagTime);

        if (attacker.isGliding()) {
            attacker.setGliding(false);
        }
        if (victim.isGliding()) {
            victim.setGliding(false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGlide(EntityToggleGlideEvent event) {
        if (event.getEntity() instanceof Player && event.isGliding()) {
            Player player = (Player) event.getEntity();
            if (CooldownManager.isCombatTagged(player.getUniqueId())) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "You cannot use Elytra while in combat!");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!CooldownManager.isCombatTagged(player.getUniqueId())) {
            return;
        }

        String message = event.getMessage().toLowerCase();
        String command = message.split(" ")[0];

        List<String> allowed = Main.getInstance().getConfig().getStringList("rules.allowed_combat_commands");
        if (allowed.isEmpty()) {
            allowed = DEFAULT_ALLOWED_COMMANDS;
        }

        boolean isAllowed = false;
        for (String allow : allowed) {
            if (command.equalsIgnoreCase(allow) || command.startsWith(allow.toLowerCase() + " ")) {
                isAllowed = true;
                break;
            }
        }

        if (!isAllowed) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You cannot use commands while in combat!");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVillagerOpen(InventoryOpenEvent event) {
        if (Main.getInstance().getConfig().getBoolean("config.combat_log_no_restock", false)) {
            if (event.getInventory().getType() == InventoryType.MERCHANT && event.getPlayer() instanceof Player) {
                Player player = (Player) event.getPlayer();
                if (CooldownManager.isCombatTagged(player.getUniqueId())) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "You cannot trade with villagers while in combat!");
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (CooldownManager.isCombatTagged(player.getUniqueId())) {
            String mode = Main.getInstance().getConfig().getString("config.combat_log_mode", "ANNOUNCE").toUpperCase();
            if ("KILL".equals(mode)) {
                player.setHealth(0.0);
            } else {
                String logMsg = Main.getInstance().getConfig().getString("rules.combat_log_message", "§c<player> combat logged!")
                        .replace("<player>", player.getName());
                Bukkit.broadcastMessage(logMsg);
            }
            CombatManager.untag(player);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        CombatManager.untag(event.getEntity());
    }
}
