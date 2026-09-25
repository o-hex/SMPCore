package pl.smpcore.packet;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerJoinGame;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerRespawn;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAttributes;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import pl.Main;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PacketEventsHook implements Listener {
    private static Main plugin;
    private static AntiSeedCrackerListener seedCrackerListener;
    private static AntiHealthIndicatorListener healthIndicatorListener;
    private static boolean packetEventsPresent = false;

    public static void onLoad(Main main) {
        plugin = main;
        if (Bukkit.getPluginManager().getPlugin("packetevents") != null) {
            try {
                PacketEvents.setAPI(SpigotPacketEventsBuilder.build(main));
                PacketEvents.getAPI().getSettings().checkForUpdates(true).bStats(false);
                PacketEvents.getAPI().load();
                packetEventsPresent = true;
            } catch (Throwable t) {
                main.getLogger().warning("Failed to initialize PacketEvents: " + t.getMessage());
            }
        }
    }

    public static void onEnable(Main main) {
        plugin = main;
        if (Bukkit.getPluginManager().getPlugin("packetevents") != null && packetEventsPresent) {
            updateListeners();
        }
        updateHealthIndicatorScoreboard(main.getConfig().getBoolean("rules.always_health_indicators", false));
    }

    public static boolean isPacketEventsAvailable() {
        return packetEventsPresent && Bukkit.getPluginManager().getPlugin("packetevents") != null;
    }

    public static void updateListeners() {
        if (!isPacketEventsAvailable() || PacketEvents.getAPI() == null) return;

        // Anti Seed Cracker
        boolean seedCrackEnabled = plugin.getConfig().getBoolean("rules.ban_seed_cracking", true);
        if (seedCrackEnabled && seedCrackerListener == null) {
            seedCrackerListener = new AntiSeedCrackerListener();
            PacketEvents.getAPI().getEventManager().registerListener(seedCrackerListener);
        } else if (!seedCrackEnabled && seedCrackerListener != null) {
            PacketEvents.getAPI().getEventManager().unregisterListener(seedCrackerListener);
            seedCrackerListener = null;
        }

        // Anti Health Indicator
        boolean healthIndicatorEnabled = plugin.getConfig().getBoolean("rules.anti_health_indicators", true);
        if (healthIndicatorEnabled && healthIndicatorListener == null) {
            healthIndicatorListener = new AntiHealthIndicatorListener();
            PacketEvents.getAPI().getEventManager().registerListener(healthIndicatorListener);
            Bukkit.getPluginManager().registerEvents(healthIndicatorListener, plugin);
        } else if (!healthIndicatorEnabled && healthIndicatorListener != null) {
            PacketEvents.getAPI().getEventManager().unregisterListener(healthIndicatorListener);
            HandlerList.unregisterAll(healthIndicatorListener);
            healthIndicatorListener = null;
        }
    }

    public static void onDisable() {
        if (packetEventsPresent && PacketEvents.getAPI() != null) {
            if (seedCrackerListener != null) {
                PacketEvents.getAPI().getEventManager().unregisterListener(seedCrackerListener);
                seedCrackerListener = null;
            }
            if (healthIndicatorListener != null) {
                PacketEvents.getAPI().getEventManager().unregisterListener(healthIndicatorListener);
                HandlerList.unregisterAll(healthIndicatorListener);
                healthIndicatorListener = null;
            }
            PacketEvents.getAPI().terminate();
        }
    }

    public static void updateHealthIndicatorScoreboard(boolean enable) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Objective objective = scoreboard.getObjective("health");
        if (enable) {
            if (objective == null) {
                objective = scoreboard.registerNewObjective("health", Criteria.HEALTH, Component.text("❤", NamedTextColor.RED));
                objective.setDisplaySlot(DisplaySlot.BELOW_NAME);
            }
        } else {
            if (objective != null) {
                objective.unregister();
            }
        }
    }

    public static class AntiSeedCrackerListener extends PacketListenerAbstract {
        @Override
        public void onPacketSend(PacketSendEvent event) {
            if (event.getPacketType() == PacketType.Play.Server.JOIN_GAME) {
                WrapperPlayServerJoinGame wrapper = new WrapperPlayServerJoinGame(event);
                wrapper.setHashedSeed(plugin.randomizeHashedSeed(wrapper.getHashedSeed()));
            } else if (event.getPacketType() == PacketType.Play.Server.RESPAWN) {
                WrapperPlayServerRespawn wrapper = new WrapperPlayServerRespawn(event);
                wrapper.setHashedSeed(plugin.randomizeHashedSeed(wrapper.getHashedSeed()));
            }
        }
    }

    public static class AntiHealthIndicatorListener extends PacketListenerAbstract implements Listener {
        private final Set<Integer> trackedPlayers = Collections.synchronizedSet(new HashSet<>());

        public AntiHealthIndicatorListener() {
            for (Player player : Bukkit.getOnlinePlayers()) {
                trackedPlayers.add(player.getEntityId());
            }
        }

        @EventHandler
        public void onJoin(PlayerJoinEvent event) {
            trackedPlayers.add(event.getPlayer().getEntityId());
        }

        @EventHandler
        public void onQuit(PlayerQuitEvent event) {
            trackedPlayers.remove(event.getPlayer().getEntityId());
        }

        @Override
        public void onPacketSend(PacketSendEvent event) {
            Player receiver = (Player) event.getPlayer();
            if (receiver == null) return;

            if (event.getPacketType() == PacketType.Play.Server.ENTITY_METADATA) {
                WrapperPlayServerEntityMetadata wrapper = new WrapperPlayServerEntityMetadata(event);
                int entityId = wrapper.getEntityId();
                if (entityId == receiver.getEntityId() || !trackedPlayers.contains(entityId)) {
                    return;
                }
                boolean modified = false;
                for (EntityData entry : wrapper.getEntityMetadata()) {
                    if (entry.getIndex() == 9 && entry.getValue() instanceof Number) {
                        entry.setValue(1.0f);
                        modified = true;
                    }
                }
                if (modified) {
                    event.markForReEncode(true);
                }
            } else if (event.getPacketType() == PacketType.Play.Server.UPDATE_ATTRIBUTES) {
                WrapperPlayServerUpdateAttributes wrapper = new WrapperPlayServerUpdateAttributes(event);
                int entityId = wrapper.getEntityId();
                if (entityId == receiver.getEntityId() || !trackedPlayers.contains(entityId)) {
                    return;
                }
                boolean modified = false;
                for (WrapperPlayServerUpdateAttributes.Property property : wrapper.getProperties()) {
                    if ("minecraft:max_health".equalsIgnoreCase(property.getKey())) {
                        property.setValue(2.0);
                        modified = true;
                    }
                }
                if (modified) {
                    event.markForReEncode(true);
                }
            }
        }
    }
}
