package pl.smpcore.cooldown;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CooldownManager {
    // Player UUID -> (Cooldown Key -> Expiration Millis)
    private static final Map<UUID, Map<String, Long>> playerCooldowns = new ConcurrentHashMap<>();
    // Global Cooldown Key -> Expiration Millis
    private static final Map<String, Long> globalCooldowns = new ConcurrentHashMap<>();
    // Combat tagged players: UUID -> Expiration Millis
    private static final Map<UUID, Long> combatCooldowns = new ConcurrentHashMap<>();

    private CooldownManager() {}

    public static boolean hasCooldown(UUID uuid, String key) {
        if (uuid == null || key == null) return false;
        Map<String, Long> map = playerCooldowns.get(uuid);
        if (map == null) return false;
        Long expire = map.get(key);
        if (expire == null) return false;
        if (System.currentTimeMillis() >= expire) {
            map.remove(key);
            return false;
        }
        return true;
    }

    public static boolean hasCooldown(Player player, String key) {
        return player != null && hasCooldown(player.getUniqueId(), key);
    }

    public static double getCooldownRemaining(UUID uuid, String key) {
        if (uuid == null || key == null) return 0.0;
        Map<String, Long> map = playerCooldowns.get(uuid);
        if (map == null) return 0.0;
        Long expire = map.get(key);
        if (expire == null) return 0.0;
        long diff = expire - System.currentTimeMillis();
        if (diff <= 0) {
            map.remove(key);
            return 0.0;
        }
        return Math.round((diff / 1000.0) * 10.0) / 10.0;
    }

    public static double getRemainingSeconds(UUID uuid, String key) {
        return getCooldownRemaining(uuid, key);
    }

    public static void setCooldown(UUID uuid, String key, double seconds) {
        if (uuid == null || key == null) return;
        long expire = System.currentTimeMillis() + (long)(seconds * 1000L);
        playerCooldowns.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).put(key, expire);
    }

    public static void setCooldown(UUID uuid, String key, int seconds) {
        setCooldown(uuid, key, (double) seconds);
    }

    public static void setCooldown(Player player, String key, double seconds) {
        if (player != null) {
            setCooldown(player.getUniqueId(), key, seconds);
        }
    }

    public static void removeCooldown(UUID uuid, String key) {
        if (uuid == null || key == null) return;
        Map<String, Long> map = playerCooldowns.get(uuid);
        if (map != null) {
            map.remove(key);
        }
    }

    // Global cooldowns (e.g. grace)
    public static boolean hasGlobalCooldown(String key) {
        if (key == null) return false;
        Long expire = globalCooldowns.get(key);
        if (expire == null) return false;
        if (System.currentTimeMillis() >= expire) {
            globalCooldowns.remove(key);
            return false;
        }
        return true;
    }

    public static double getGlobalCooldownRemaining(String key) {
        if (key == null) return 0.0;
        Long expire = globalCooldowns.get(key);
        if (expire == null) return 0.0;
        long diff = expire - System.currentTimeMillis();
        if (diff <= 0) {
            globalCooldowns.remove(key);
            return 0.0;
        }
        return Math.round((diff / 1000.0) * 10.0) / 10.0;
    }

    public static double getGlobalCooldown(String key) {
        return getGlobalCooldownRemaining(key);
    }

    public static void setGlobalCooldown(String key, double seconds) {
        if (key == null) return;
        if (seconds <= 0.0) {
            globalCooldowns.remove(key);
            return;
        }
        long expire = System.currentTimeMillis() + (long)(seconds * 1000L);
        globalCooldowns.put(key, expire);
    }

    // Combat cooldowns
    public static boolean isCombatTagged(UUID uuid) {
        if (uuid == null) return false;
        Long expire = combatCooldowns.get(uuid);
        if (expire == null) return false;
        if (System.currentTimeMillis() >= expire) {
            combatCooldowns.remove(uuid);
            return false;
        }
        return true;
    }

    public static double getCombatCooldownRemaining(UUID uuid) {
        if (uuid == null) return 0.0;
        Long expire = combatCooldowns.get(uuid);
        if (expire == null) return 0.0;
        long diff = expire - System.currentTimeMillis();
        if (diff <= 0) {
            combatCooldowns.remove(uuid);
            return 0.0;
        }
        return Math.round((diff / 1000.0) * 10.0) / 10.0;
    }

    public static void setCombatTagged(UUID uuid, double seconds) {
        if (uuid == null) return;
        if (seconds <= 0.0) {
            combatCooldowns.remove(uuid);
            return;
        }
        long expire = System.currentTimeMillis() + (long)(seconds * 1000L);
        combatCooldowns.put(uuid, expire);
    }

    public static void removeCombatTag(UUID uuid) {
        if (uuid != null) {
            combatCooldowns.remove(uuid);
        }
    }

    public static Map<UUID, Long> getCombatCooldowns() {
        return combatCooldowns;
    }

    // Periodic pruner
    public static void prune() {
        long now = System.currentTimeMillis();
        globalCooldowns.entrySet().removeIf(e -> now >= e.getValue());
        combatCooldowns.entrySet().removeIf(e -> now >= e.getValue());
        for (Map.Entry<UUID, Map<String, Long>> entry : playerCooldowns.entrySet()) {
            entry.getValue().entrySet().removeIf(e -> now >= e.getValue());
            if (entry.getValue().isEmpty()) {
                playerCooldowns.remove(entry.getKey());
            }
        }
    }

    public static void clearAll() {
        playerCooldowns.clear();
        globalCooldowns.clear();
        combatCooldowns.clear();
    }
}
