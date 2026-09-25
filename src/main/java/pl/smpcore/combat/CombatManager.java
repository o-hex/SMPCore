package pl.smpcore.combat;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import pl.Main;
import pl.smpcore.cooldown.CooldownManager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CombatManager {
    private static final Map<UUID, BossBar> bossBars = new ConcurrentHashMap<>();

    private CombatManager() {}

    public static int getTagTime() {
        org.bukkit.configuration.file.FileConfiguration cfg = Main.getInstance().getConfig();
        if (cfg.contains("config.combat_tag_time")) {
            return cfg.getInt("config.combat_tag_time", 30);
        }
        return cfg.getInt("rules.combat_tag_time", 30);
    }

    public static void tag(Player player) {
        if (player == null) return;
        CooldownManager.setCombatTagged(player.getUniqueId(), getTagTime());
    }

    public static void untag(Player player) {
        if (player == null) return;
        CooldownManager.removeCombatTag(player.getUniqueId());
        removeBossBar(player);
    }

    public static boolean isTagged(Player player) {
        return player != null && CooldownManager.isCombatTagged(player.getUniqueId());
    }

    public static BossBar getOrCreateBossBar(Player player) {
        return bossBars.computeIfAbsent(player.getUniqueId(), uuid -> {
            BossBar bar = Bukkit.createBossBar(ChatColor.RED + "" + ChatColor.BOLD + "Combat", BarColor.RED, BarStyle.SOLID);
            bar.addPlayer(player);
            return bar;
        });
    }

    public static void removeBossBar(Player player) {
        if (player == null) return;
        BossBar bar = bossBars.remove(player.getUniqueId());
        if (bar != null) {
            bar.removeAll();
        }
    }

    public static void cleanup() {
        for (BossBar bar : bossBars.values()) {
            bar.removeAll();
        }
        bossBars.clear();
    }
}
