package pl.smpcore.combat;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import pl.Main;
import pl.smpcore.cooldown.CooldownManager;

import java.util.UUID;

public class CombatDisplayTask extends BukkitRunnable {
    @Override
    public void run() {
        if (!Main.getInstance().getConfig().getBoolean("heavy.combat_actionorbossbar", true)) {
            return;
        }

        String displayMode = Main.getInstance().getConfig().getString("config.combat_display", "NONE").toUpperCase();
        if ("NONE".equals(displayMode)) {
            return;
        }

        double maxTagTime = Main.getInstance().getConfig().getInt("config.combat_tag_time", 30);

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            if (CooldownManager.isCombatTagged(uuid)) {
                double remaining = CooldownManager.getCombatCooldownRemaining(uuid);
                int mins = (int)(remaining / 60.0);
                int secs = (int)(remaining % 60.0);
                String timeStr = String.format("%02d:%02d", mins, secs);

                switch (displayMode) {
                    case "ACTIONBAR":
                        player.sendActionBar(Component.text("§l§cCOMBAT§8: §7§l" + timeStr));
                        break;
                    case "BOSSBAR":
                        BossBar bar = CombatManager.getOrCreateBossBar(player);
                        bar.setTitle("§c§lCombat: §f§l" + timeStr);
                        bar.setProgress(Math.max(0.0, Math.min(1.0, remaining / maxTagTime)));
                        break;
                    case "WEAPON_COOLDOWN":
                        player.setCooldown(Material.DIAMOND_SWORD, (int)(remaining * 20.0));
                        break;
                }
            } else {
                if ("BOSSBAR".equals(displayMode)) {
                    CombatManager.removeBossBar(player);
                }
            }
        }
    }
}
