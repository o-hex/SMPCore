package pl;

import io.papermc.paper.event.block.VaultChangeStateEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.util.Vector;
import pl.smpcore.altars.AltarCommand;
import pl.smpcore.altars.AltarConfigManager;
import pl.smpcore.altars.AltarEditGUI;
import pl.smpcore.altars.AltarListener;
import pl.smpcore.altars.AltarManager;
import pl.smpcore.combat.CombatDisplayTask;
import pl.smpcore.combat.CombatListener;
import pl.smpcore.commands.*;
import pl.smpcore.cooldown.CooldownManager;
import pl.smpcore.mechanics.*;
import pl.smpcore.packet.PacketEventsHook;
import pl.smpcore.recipes.CustomRecipesManager;
import pl.smpcore.settings.SettingsCommand;
import pl.smpcore.settings.SettingsGUI;
import pl.smpcore.util.ChatUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

public final class Main extends JavaPlugin implements Listener {

    private static Main instance;
    public static NamespacedKey glowing;
    public int before_start_border;
    public static AltarManager altarManager;
    public AltarConfigManager configManager;
    public static FileConfiguration config;
    private CombatDisplayTask combatDisplayTask;
    private BukkitTask graceTask;
    private BukkitTask cooldownPruneTask;

    public static Main getInstance() {
        return instance;
    }

    public static AltarManager getAltarManager() {
        return altarManager;
    }

    public AltarConfigManager getConfigManager() {
        return configManager;
    }

    @Override
    public void onLoad() {
        if (getServer().getPluginManager().getPlugin("packetevents") != null) {
            try {
                PacketEventsHook.onLoad(this);
            } catch (Throwable t) {
                getLogger().warning("Failed to initialize PacketEvents: " + t.getMessage());
            }
        }
    }

    @Override
    public void onEnable() {
        instance = this;
        glowing = new NamespacedKey(this, "glowing");
        config = getConfig();
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        before_start_border = getConfig().getInt("config.before_start_border", 50);

        setupEventListeners();
        setupCommands();
        Bukkit.getPluginManager().registerEvents(this, this);

        antixray();
        attributeSwapping();
        shieldStun();
        hideitemMeta();

        boolean healthIndicators = getConfig().getBoolean("rules.always_health_indicators", false);
        setScoreboardHealthIndicator(healthIndicators);

        if (getServer().getPluginManager().getPlugin("packetevents") != null) {
            try {
                PacketEventsHook.onEnable(this);
            } catch (Throwable t) {
                getLogger().warning("Failed to enable PacketEvents hook: " + t.getMessage());
            }
        }

        if (getConfig().getBoolean("altar_enabled", true)) {
            altarManager = new AltarManager(this);
            configManager = new AltarConfigManager(this);
            Bukkit.getPluginManager().registerEvents(altarManager, this);
            Bukkit.getPluginManager().registerEvents(new AltarListener(altarManager), this);
            Bukkit.getPluginManager().registerEvents(new AltarEditGUI(), this);
            if (getCommand("saltar") != null) {
                AltarCommand altarCmd = new AltarCommand(altarManager, configManager);
                getCommand("saltar").setExecutor(altarCmd);
                getCommand("saltar").setTabCompleter(altarCmd);
            }
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (altarManager != null) {
                    altarManager.loadAltars();
                }
            }, 40L);
        }

        // /string command executor
        if (getCommand("string") != null) {
            getCommand("string").setExecutor((commandSender, command, label, args) -> {
                if (!Main.getInstance().getConfig().getBoolean("rules.slash_string", false)) {
                    commandSender.sendMessage("§c/string is disabled");
                    return true;
                }
                if (commandSender instanceof Player player) {
                    if (CooldownManager.hasCooldown(player.getUniqueId(), "string")) {
                        double rem = CooldownManager.getRemainingSeconds(player.getUniqueId(), "string");
                        player.sendMessage("§cYou must wait §e" + rem + " §cseconds before you can use /string again");
                        return true;
                    }
                    int cd = Main.getInstance().getConfig().getInt("rules.slash_string_cooldown", 30);
                    CooldownManager.setCooldown(player.getUniqueId(), "string", cd);
                    for (int i = 0; i < 36; i++) {
                        if (player.getInventory().getItem(i) == null) {
                            player.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                            player.getInventory().setItem(i, new ItemStack(Material.STRING, 64));
                        }
                    }
                }
                return true;
            });
        }

        CustomRecipesManager.registerRecipes();

        if (getConfig().getBoolean("easy_recipes.easy_gaps", true)) {
            EasyRecipesManager.registerEasyGaps();
        }
        if (getConfig().getBoolean("easy_recipes.easy_cobwebs", true)) {
            EasyRecipesManager.registerEasyCobwebs();
        }

        this.cooldownPruneTask = Bukkit.getScheduler().runTaskTimer(this, CooldownManager::prune, 200L, 200L);

        if (getConfig().getBoolean("heavy.combat_actionorbossbar", true)) {
            this.combatDisplayTask = new CombatDisplayTask();
            this.combatDisplayTask.runTaskTimer(this, 0L, 20L);
        }
    }

    @Override
    public void onDisable() {
        if (getServer().getPluginManager().getPlugin("packetevents") != null) {
            try {
                PacketEventsHook.onDisable();
            } catch (Throwable t) {
                getLogger().warning("Failed to disable PacketEvents hook: " + t.getMessage());
            }
        }

        if (this.cooldownPruneTask != null) {
            this.cooldownPruneTask.cancel();
            this.cooldownPruneTask = null;
        }
        if (this.graceTask != null) {
            this.graceTask.cancel();
            this.graceTask = null;
        }
        if (this.combatDisplayTask != null) {
            this.combatDisplayTask.cancel();
            this.combatDisplayTask = null;
        }

        CooldownManager.clearAll();

        if (altarManager != null) {
            altarManager.shutdown();
        }

        RitualManager.cleanup();
        CustomRecipesManager.unregisterRecipes();
        ItemLimitCommand.cleanup();
    }

    public void setScoreboardHealthIndicator(boolean enable) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Objective objective = scoreboard.getObjective("health");
        if (enable) {
            if (objective == null) {
                objective = scoreboard.registerNewObjective("health", Criteria.HEALTH, net.kyori.adventure.text.Component.text("❤", net.kyori.adventure.text.format.NamedTextColor.RED));
                objective.setDisplaySlot(DisplaySlot.BELOW_NAME);
            }
        } else {
            if (objective != null) {
                objective.unregister();
            }
        }
    }

    private void setupCommands() {
        if (getCommand("settings") != null) {
            getCommand("settings").setExecutor(new SettingsCommand());
        }

        BanItemCommand banItemCmd = new BanItemCommand();
        if (getCommand("banitem") != null) {
            getCommand("banitem").setExecutor(banItemCmd);
            getCommand("banitem").setTabCompleter(banItemCmd);
        }

        if (getCommand("ritual") != null) {
            getCommand("ritual").setExecutor(new RitualCommand());
        }

        EnchantCommand enchantCmd = new EnchantCommand();
        if (getCommand("enchant") != null) {
            getCommand("enchant").setExecutor(enchantCmd);
            getCommand("enchant").setTabCompleter(enchantCmd);
        }

        SpawnCommands spawnCmd = new SpawnCommands();
        if (getCommand("setcustomspawn") != null) {
            getCommand("setcustomspawn").setExecutor(spawnCmd);
            getCommand("setcustomspawn").setTabCompleter(spawnCmd);
        }
        if (getCommand("setrespawnspawn") != null) {
            getCommand("setrespawnspawn").setExecutor(spawnCmd);
            getCommand("setrespawnspawn").setTabCompleter(spawnCmd);
        }
        Bukkit.getPluginManager().registerEvents(spawnCmd, this);

        WorldTpCommand worldTpCmd = new WorldTpCommand();
        if (getCommand("worldtp") != null) {
            getCommand("worldtp").setExecutor(worldTpCmd);
            getCommand("worldtp").setTabCompleter(worldTpCmd);
        }

        InvseeCommand invseeCmd = new InvseeCommand();
        if (getCommand("invsee") != null) {
            getCommand("invsee").setExecutor(invseeCmd);
        }
        if (getCommand("endersee") != null) {
            getCommand("endersee").setExecutor(invseeCmd);
        }
        Bukkit.getPluginManager().registerEvents(invseeCmd, this);

        RollbackCommand rollbackCmd = new RollbackCommand();
        if (getCommand("rollback") != null) {
            getCommand("rollback").setExecutor(rollbackCmd);
            getCommand("rollback").setTabCompleter(rollbackCmd);
        }
        Bukkit.getPluginManager().registerEvents(rollbackCmd, this);

        VanishCommand vanishCmd = new VanishCommand();
        if (getCommand("vanish") != null) {
            getCommand("vanish").setExecutor(vanishCmd);
        }
        Bukkit.getPluginManager().registerEvents(vanishCmd, this);

        ReplyCommand replyCmd = new ReplyCommand();
        if (getCommand("reply") != null) {
            getCommand("reply").setExecutor(replyCmd);
        }
        Bukkit.getPluginManager().registerEvents(replyCmd, this);

        if (getCommand("sbroadcast") != null) {
            getCommand("sbroadcast").setExecutor(new SBroadcastCommand());
        }

        if (getCommand("whitelistplus") != null) {
            getCommand("whitelistplus").setExecutor(new WhitelistPlusCommand());
        }

        KitCommand kitCmd = new KitCommand();
        if (getCommand("sckit") != null) {
            getCommand("sckit").setExecutor(kitCmd);
            getCommand("sckit").setTabCompleter(kitCmd);
        }
        Bukkit.getPluginManager().registerEvents(kitCmd, this);

        if (getConfig().getBoolean("heavy.item_limit_enabled", true)) {
            ItemLimitCommand limitCmd = new ItemLimitCommand();
            if (getCommand("itemlimit") != null) {
                getCommand("itemlimit").setExecutor(limitCmd);
            }
            Bukkit.getPluginManager().registerEvents(limitCmd, this);
        }

        // Grace commands: /start and /stopgrace
        if (getCommand("start") != null) {
            getCommand("start").setExecutor((sender, command, label, args) -> {
                boolean launch = getConfig().getBoolean("config.launch", false);
                int launchStrength = getConfig().getInt("config.launch_strength", 4);
                int duration = getConfig().getInt("config.grace_duration", 40);
                int borderSize = getConfig().getInt("config.start_border_size", 40);
                int borderSpeed = getConfig().getInt("config.start_border_speed", 60);

                List<String> startCommands = getConfig().getStringList("config.start_commands");
                for (String cmd : startCommands) {
                    cmd = cmd.replace("<sender>", sender.getName());
                    Bukkit.dispatchCommand(sender, cmd);
                }

                String title = getConfig().getString("config.start_smp_title", "§aGRACE").replace("%time%", String.valueOf(duration));
                String subtitle = getConfig().getString("config.start_smp_subtitle", "§aGRACE").replace("%time%", String.valueOf(duration));
                String msg = getConfig().getString("config.start_smp_message", "");

                CooldownManager.setGlobalCooldown("grace", duration * 60L);
                startGrace();

                for (Player player : Bukkit.getOnlinePlayers()) {
                    int steak = getConfig().getInt("config.start_steak", 0);
                    if (steak > 0) {
                        player.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, steak));
                    }
                    if (!msg.isEmpty()) {
                        player.sendMessage(ChatUtil.color(msg));
                    }
                    if (launch) {
                        player.setVelocity(new Vector(0, launchStrength, 0));
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (!player.isOnline()) {
                                    cancel();
                                    return;
                                }
                                player.setNoDamageTicks(10);
                                player.setFallDistance(0.0f);
                                CooldownManager.setCooldown(player, "gliding", 0.2);
                                if (player.isOnGround() || player.isInWater() || player.isInLava()) {
                                    cancel();
                                }
                            }
                        }.runTaskTimer(this, 5L, 1L);
                    }
                    player.getWorld().getWorldBorder().setSize(borderSize, borderSpeed);
                    player.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                    player.playSound(player, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
                    player.sendTitle(title, subtitle, 10, 70, 20);
                }
                return true;
            });
        }

        if (getCommand("stopgrace") != null) {
            getCommand("stopgrace").setExecutor((sender, command, label, args) -> {
                CooldownManager.setGlobalCooldown("grace", 0.0);
                return true;
            });
        }
    }

    private void setupEventListeners() {
        Bukkit.getPluginManager().registerEvents(new SettingsGUI(), this);
        Bukkit.getPluginManager().registerEvents(new CombatListener(), this);
        Bukkit.getPluginManager().registerEvents(new DamageLimiterListener(), this);
        Bukkit.getPluginManager().registerEvents(new ExplosionRulesListener(), this);
        Bukkit.getPluginManager().registerEvents(new EnchantNetheriteListener(), this);
        Bukkit.getPluginManager().registerEvents(new MaceCraftListener(), this);
        Bukkit.getPluginManager().registerEvents(new BreachSwapListener(), this);
        Bukkit.getPluginManager().registerEvents(new AntiStasisListener(), this);
        Bukkit.getPluginManager().registerEvents(new ItemCooldownListener(), this);
        Bukkit.getPluginManager().registerEvents(new NakedProtectionListener(), this);
        Bukkit.getPluginManager().registerEvents(new AfkProtectionListener(), this);
        Bukkit.getPluginManager().registerEvents(new GoldenHeadListener(), this);
        Bukkit.getPluginManager().registerEvents(new CobwebDecayListener(), this);
        Bukkit.getPluginManager().registerEvents(new PearlCatchListener(), this);
        Bukkit.getPluginManager().registerEvents(new VillagerListener(), this);
        Bukkit.getPluginManager().registerEvents(new DimensionListener(), this);
        Bukkit.getPluginManager().registerEvents(new SecurityListener(), this);
        Bukkit.getPluginManager().registerEvents(new BannedItemsListener(), this);
        Bukkit.getPluginManager().registerEvents(new ShieldTweaksListener(), this);
        Bukkit.getPluginManager().registerEvents(new AntiDrainingListener(), this);

        if (getConfig().getBoolean("rules.string_dupers", false)) {
            Bukkit.getPluginManager().registerEvents(new StringDuperListener(), this);
        }

        if (HappyGhastListener.isSupported()) {
            Bukkit.getPluginManager().registerEvents(new HappyGhastListener(), this);
        }
    }

    private void startGrace() {
        if (this.graceTask != null) {
            this.graceTask.cancel();
            this.graceTask = null;
        }
        this.graceTask = new BukkitRunnable() {
            @Override
            public void run() {
                double remaining = CooldownManager.getGlobalCooldown("grace");
                if (remaining <= 0.0) {
                    cancel();
                    graceTask = null;
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        String title = getConfig().getString("config.grace_end_title", "§aGRACE IS OVER!");
                        String subtitle = getConfig().getString("config.grace_end_subtitle", "§7PvP has been enabled");
                        String message = getConfig().getString("config.grace_end_message", "");
                        p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                        p.playSound(p, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
                        p.sendTitle(ChatColor.GREEN + title, subtitle, 10, 70, 20);
                        if (!message.isEmpty()) {
                            p.sendMessage(ChatUtil.color(message));
                        }
                    }
                    Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
                    Objective obj = scoreboard.getObjective("grace");
                    if (obj != null) {
                        obj.unregister();
                    }
                    return;
                }

                int min = (int) (remaining / 60.0);
                int sec = (int) (remaining % 60.0);
                String formatted = String.format("%02d:%02d", min, sec);
                Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
                Objective obj = scoreboard.getObjective("grace");
                if (obj == null) {
                    obj = scoreboard.registerNewObjective("grace", Criteria.DUMMY, Component.text("§aɢʀᴀᴄᴇ"));
                    obj.setDisplaySlot(DisplaySlot.SIDEBAR);
                } else {
                    for (String entry : scoreboard.getEntries()) {
                        scoreboard.resetScores(entry);
                    }
                }
                obj.getScore("§f⌚ " + formatted).setScore(0);
            }
        }.runTaskTimer(this, 0L, 10L);
    }

    public long randomizeHashedSeed(long seed) {
        int length = Long.toString(seed).length();
        if (length > 18) {
            length = 18;
        }
        long min = (long) Math.pow(10.0, length - 1);
        long max = (long) (Math.pow(10.0, length) - 1.0);
        return ThreadLocalRandom.current().nextLong(min, max + 1L);
    }

    public void sendTellrawAsync(String string, Player player) {
        CompletableFuture.runAsync(() -> Bukkit.getScheduler().runTask(this, () -> Bukkit.getScheduler().runTaskLater(this, () -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw " + player.getName() + " \"" + string + "\"");
        }, 10L)));
    }

    public static ItemStack getWardenHeart() {
        ItemStack item = new ItemStack(Material.ECHO_SHARD);
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§1Warden Heart");
            meta.setCustomModelData(9999);
            meta.setEnchantmentGlintOverride(true);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onWardenDeath(EntityDeathEvent event) {
        if (event.getEntity().getType() == EntityType.WARDEN) {
            Location loc = event.getEntity().getLocation();
            if (config.getBoolean("rules.warden", true)) {
                loc.getWorld().dropItemNaturally(loc, getWardenHeart());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (event.isCancelled()) return;
        boolean dropsUnlimited = getConfig().getBoolean("rules.drops", true);
        boolean immortal = getConfig().contains("rules.immortal_item")
                ? getConfig().getBoolean("rules.immortal_item", false)
                : getConfig().getBoolean("config.immortal_item", false);
        if (!dropsUnlimited && !immortal) return;

        List<ItemStack> drops = new ArrayList<>(event.getDrops());
        event.getDrops().clear();
        Location loc = event.getEntity().getLocation();
        for (ItemStack drop : drops) {
            Item item = loc.getWorld().dropItemNaturally(loc, drop);
            if (dropsUnlimited) {
                item.setUnlimitedLifetime(true);
            }
            if (immortal) {
                item.setInvulnerable(true);
            }
            if (drop.getItemMeta() != null && drop.getItemMeta().getPersistentDataContainer().has(glowing, PersistentDataType.BOOLEAN)) {
                item.setGlowing(true);
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Item item && item.isInvulnerable()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onGraceDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.getWorld().getWorldBorder().getSize() < (double) this.before_start_border) {
            event.setCancelled(true);
        }
        boolean immunity = getConfig().getBoolean("config.immunity_during_grace", true);
        if (CooldownManager.hasGlobalCooldown("grace") && immunity) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onGracePvP(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) return;
        boolean pvp = getConfig().getBoolean("rules.pvp", true);
        if (CooldownManager.hasGlobalCooldown("grace") || !pvp) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onGraceProjectiles(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (!(event.getDamager() instanceof Projectile projectile)) return;
        if (!(projectile.getShooter() instanceof Player)) return;
        boolean pvp = getConfig().getBoolean("rules.pvp", true);
        if (CooldownManager.hasGlobalCooldown("grace") || !pvp) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onExtraDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) return;
        int extra = getConfig().getInt("rules.extra_damage", 0);
        if (!event.isCancelled() && event.getDamage() > 1.0 && extra > 0) {
            event.setDamage(event.getDamage() + (double) extra);
        }
    }

    @EventHandler
    public void onShieldBreak(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player) || !(event.getEntity() instanceof Player victim)) return;
        Vector toDamager = player.getLocation().toVector().subtract(victim.getLocation().toVector()).normalize();
        Vector victimDir = victim.getLocation().getDirection().normalize();
        if (victimDir.dot(toDamager) < 0.0) return;

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (!getConfig().getBoolean("rules.mace_stun_shield", false)) return;
        if (mainHand.getType() != Material.MACE) return;

        if (victim.isBlocking()) {
            event.setCancelled(true);
            victim.clearActiveItem();
            victim.setCooldown(Material.SHIELD, 100);
            victim.getWorld().playSound(victim.getLocation(), Sound.ITEM_SHIELD_BREAK, 1.0f, 1.0f);
        }
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player && player.getWorld().getWorldBorder().getSize() < (double) this.before_start_border) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onVaultChange(VaultChangeStateEvent event) {
        if (getConfig().getBoolean("rules.disable_vaults", false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String sound = getConfig().getString("custom_join_sound", "");
        float pitch = (float) getConfig().getDouble("custom_join_sound_pitch", 1.0);
        if (sound != null && !sound.isEmpty()) {
            Bukkit.getScheduler().runTaskLater(this, () -> {
                player.getWorld().playSound(player, sound, 1.0f, pitch);
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.getWorld().playSound(p, sound, 1.0f, pitch);
                }
            }, 5L);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String sound = getConfig().getString("custom_leave_sound", "");
        float pitch = (float) getConfig().getDouble("custom_leave_sound_pitch", 1.0);
        if (sound != null && !sound.isEmpty()) {
            player.getWorld().playSound(player, sound, 1.0f, pitch);
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.getWorld().playSound(p, sound, 1.0f, pitch);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeathSound(PlayerDeathEvent event) {
        if (event.isCancelled()) return;
        float pitch = (float) getConfig().getDouble("rules.death_sound_pitch", 1.0);
        String sound = getConfig().getString("rules.death_sound", "minecraft:entity.player.death");
        event.getPlayer().getWorld().playSound(event.getPlayer().getLocation(), sound, 1.0f, pitch);

        String msg = getConfig().getString("death_message", null);
        String killerName = event.getPlayer().getKiller() != null ? event.getPlayer().getKiller().getName() : "unknown";
        if (msg != null && !msg.equals("null") && !msg.isEmpty()) {
            msg = msg.replace("<attacker>", killerName).replace("<victim>", event.getPlayer().getName());
            event.setDeathMessage(ChatUtil.color(msg));
        }
    }

    public void hideitemMeta() {
        File file = new File(Bukkit.getServer().getWorldContainer(), "config/paper-world-defaults.yml");
        if (!file.exists()) return;
        try {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            String path = "anticheat.obfuscation.items.hide-durability";
            boolean desired = getConfig().contains(path)
                    ? getConfig().getBoolean(path, false)
                    : getConfig().getBoolean("rules.hide_armor_durability", false);
            boolean current = yaml.getBoolean(path, true);
            if (current != desired) {
                yaml.set(path, desired);
                yaml.save(file);
                getLogger().info("[HideDurability] 'hide-durability' updated to " + desired + ". Server restart required.");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "restart");
            }
        } catch (Exception e) {
            getLogger().warning("[HideDurability] Failed to modify paper-world-defaults.yml: " + e.getMessage());
        }
    }

    public static void shieldStun() {
        File file = new File(Bukkit.getServer().getWorldContainer(), "config/paper-global.yml");
        if (!file.exists()) return;
        try {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            String globalPath = "unsupported-settings.skip-vanilla-damage-tick-when-shield-blocked";
            String cfgPath = "shield_tweaks.skip-vanilla-damage-tick-when-shield-blocked";
            boolean desired = config.getBoolean(cfgPath, false);
            boolean current = yaml.getBoolean(globalPath, false);
            if (current != desired) {
                yaml.set(globalPath, desired);
                yaml.save(file);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "paper reload");
            }
        } catch (Exception e) {
            Main.getInstance().getLogger().warning("[ShieldStuns] Failed to modify paper-global.yml: " + e.getMessage());
        }
    }

    public static void attributeSwapping() {
        File file = new File(Bukkit.getServer().getWorldContainer(), "config/paper-global.yml");
        if (!file.exists()) return;
        try {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            String globalPath = "unsupported-settings.update-equipment-on-player-actions";
            String cfgPath = "rules.attributeSwapping";
            boolean desired = !config.getBoolean(cfgPath, true);
            boolean current = yaml.getBoolean(globalPath, false);
            if (current != desired) {
                yaml.set(globalPath, desired);
                yaml.save(file);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "paper reload");
            }
        } catch (Exception e) {
            Main.getInstance().getLogger().warning("[AttributeSwapping] Failed to modify paper-global.yml: " + e.getMessage());
        }
    }

    public void antixray() {
        File file = new File(Bukkit.getServer().getWorldContainer(), "config/paper-world-defaults.yml");
        if (!file.exists()) return;
        try {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            String enabledPath = "anticheat.anti-xray.enabled";
            String enginePath = "anticheat.anti-xray.engine-mode";
            boolean desiredEnabled = getConfig().getBoolean(enabledPath, false);
            int desiredEngine = getConfig().getInt(enginePath, 1);

            boolean currentEnabled = yaml.getBoolean(enabledPath, true);
            int currentEngine = yaml.getInt(enginePath, 1);
            boolean changed = false;

            if (currentEnabled != desiredEnabled) {
                yaml.set(enabledPath, desiredEnabled);
                changed = true;
                getLogger().info("[AntiXray] 'enabled' updated to " + desiredEnabled + ". Server restart required.");
            }
            if (currentEngine != desiredEngine) {
                yaml.set(enginePath, desiredEngine);
                changed = true;
            }
            if (changed) {
                yaml.save(file);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "restart");
            }
        } catch (Exception e) {
            getLogger().warning("[AntiXray] Failed to modify paper-world-defaults.yml: " + e.getMessage());
        }
    }
}
