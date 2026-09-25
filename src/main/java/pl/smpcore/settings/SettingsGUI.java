package pl.smpcore.settings;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import pl.Main;
import pl.smpcore.util.ItemBuilder;

import java.util.Arrays;
import java.util.List;

public class SettingsGUI implements Listener {
    public static final String TITLE_PAGE_1 = ChatColor.translateAlternateColorCodes('&',
            "&x&8&6&8&6&8&6&lꜱ&x&8&D&8&D&8&D&lᴍ&x&9&3&9&3&9&3&lᴘ &x&A&0&A&0&A&0&lᴄ&x&A&7&A&7&A&7&lᴏ&x&A&D&A&D&A&D&lʀ&x&B&4&B&4&B&4&lᴇ &x&C&1&C&1&C&1&lꜱ&x&C&7&C&7&C&7&lᴇ&x&C&E&C&E&C&E&lᴛ&x&D&4&D&4&D&4&lᴛ&x&D&B&D&B&D&B&lɪ&x&E&1&E&1&E&1&lɴ&x&E&8&E&8&E&8&lɢ&x&E&E&E&E&E&E&lꜱ (1/3)");
    public static final String TITLE_PAGE_2 = ChatColor.translateAlternateColorCodes('&',
            "&x&8&6&8&6&8&6&lꜱ&x&8&D&8&D&8&D&lᴍ&x&9&3&9&3&9&3&lᴘ &x&A&0&A&0&A&0&lᴄ&x&A&7&A&7&A&7&lᴏ&x&A&D&A&D&A&D&lʀ&x&B&4&B&4&B&4&lᴇ &x&C&1&C&1&C&1&lꜱ&x&C&7&C&7&C&7&lᴇ&x&C&E&C&E&C&E&lᴛ&x&D&4&D&4&D&4&lᴛ&x&D&B&D&B&D&B&lɪ&x&E&1&E&1&E&1&lɴ&x&E&8&E&8&E&8&lɢ&x&E&E&E&E&E&E&lꜱ (2/3)");
    public static final String TITLE_PAGE_3 = ChatColor.translateAlternateColorCodes('&',
            "&x&8&6&8&6&8&6&lꜱ&x&8&D&8&D&8&D&lᴍ&x&9&3&9&3&9&3&lᴘ &x&A&0&A&0&A&0&lᴄ&x&A&7&A&7&A&7&lᴏ&x&A&D&A&D&A&D&lʀ&x&B&4&B&4&B&4&lᴇ &x&C&1&C&1&C&1&lꜱ&x&C&7&C&7&C&7&lᴇ&x&C&E&C&E&C&E&lᴛ&x&D&4&D&4&D&4&lᴛ&x&D&B&D&B&D&B&lɪ&x&E&1&E&1&E&1&lɴ&x&E&8&E&8&E&8&lɢ&x&E&E&E&E&E&E&lꜱ (3/3)");

    public static final String TITLE_RITUAL = ChatColor.DARK_AQUA + "Ritual Items";
    public static final String TITLE_ONETIME = ChatColor.GOLD + "One Time Craft Items";
    public static final String TITLE_ENCHANT = ChatColor.DARK_PURPLE + "Enchant Bans";
    public static final String TITLE_POTION = ChatColor.RED + "Potion Bans";
    public static final String TITLE_TIPPED = ChatColor.GREEN + "Tipped Arrow Bans";
    public static final String TITLE_BANNED_ITEMS = ChatColor.DARK_RED + "Banned Items";

    public static void fillBorder(Inventory inv) {
        ItemStack grayGlass = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        int size = inv.getSize();
        int rows = size / 9;
        for (int i = 0; i < 9; i++) inv.setItem(i, grayGlass);
        for (int i = size - 9; i < size; i++) inv.setItem(i, grayGlass);
        for (int r = 1; r < rows - 1; r++) {
            inv.setItem(r * 9, grayGlass);
            inv.setItem(r * 9 + 8, grayGlass);
        }
    }

    private static ItemStack createToggle(String name, String configKey, Material material, boolean def) {
        boolean val;
        if (configKey.equals("rules.disable_nether")) {
            val = pl.smpcore.mechanics.DimensionListener.isNetherDisabled();
        } else if (configKey.equals("rules.disable_end")) {
            val = pl.smpcore.mechanics.DimensionListener.isEndDisabled();
        } else {
            val = Main.getInstance().getConfig().getBoolean(configKey, def);
        }
        return new ItemBuilder(material)
                .name(ChatColor.GOLD + name + ": " + (val ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED"))
                .lore(ChatColor.GRAY + "Config: " + ChatColor.DARK_GRAY + configKey,
                        ChatColor.YELLOW + "Click to toggle.")
                .hideAll()
                .build();
    }

    private static ItemStack createNumber(String name, String configKey, Material material, int def, int min, int max) {
        int val = Main.getInstance().getConfig().getInt(configKey, def);
        return new ItemBuilder(material)
                .name(ChatColor.GOLD + name + ": " + ChatColor.YELLOW + val)
                .lore(ChatColor.GRAY + "Config: " + ChatColor.DARK_GRAY + configKey,
                        ChatColor.GREEN + "Left Click: +1",
                        ChatColor.RED + "Right Click: -1")
                .hideAll()
                .build();
    }

    public static void openPage1(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_PAGE_1);
        fillBorder(inv);

        // Controls / Toggles
        inv.setItem(10, createToggle("PvP", "rules.pvp", Material.DIAMOND_SWORD, true));
        inv.setItem(11, createToggle("Combat System", "rules.combat_system", Material.NETHERITE_SWORD, true));
        inv.setItem(12, createToggle("Crystal PvP", "rules.ban_crystal_pvp", Material.END_CRYSTAL, true));
        inv.setItem(13, createToggle("Bed Bombing", "rules.ban_bed_bombing", Material.RED_BED, false));
        inv.setItem(14, createToggle("Anchor Exploding", "rules.ban_anchor_exploding", Material.RESPAWN_ANCHOR, false));
        inv.setItem(15, createToggle("Minecart Exploding", "rules.ban_carts", Material.TNT_MINECART, false));
        inv.setItem(16, createToggle("Mace Ban", "rules.ban_mace", Material.MACE, false));

        inv.setItem(19, createToggle("Mace Stun Shield", "rules.mace_stun_shield", Material.SHIELD, false));
        inv.setItem(20, createToggle("Breach Swapping", "rules.ban_breach_swapping", Material.ENCHANTED_BOOK, false));
        inv.setItem(21, createNumber("Mace Damage Cap", "rules.mace_damage_limiter", Material.ANVIL, 0, 0, 100));
        inv.setItem(22, createNumber("Crystal Damage Cap", "rules.crystal_damage_limiter", Material.OBSIDIAN, 0, 0, 100));
        inv.setItem(23, createNumber("TNT Damage Cap", "rules.tnt_damage_limiter", Material.TNT, 0, 0, 100));
        inv.setItem(24, createNumber("Cart Damage Cap", "rules.cart_damage_limiter", Material.MINECART, 0, 0, 100));
        inv.setItem(25, createNumber("Arrow Damage Cap", "rules.arrow_damage_limiter", Material.ARROW, 0, 0, 100));

        inv.setItem(28, createNumber("Fall Damage Cap", "rules.fall_damage_limiter", Material.FEATHER, 0, 0, 100));
        inv.setItem(29, createNumber("Extra Damage", "rules.extra_damage", Material.BLAZE_POWDER, 0, 0, 20));
        inv.setItem(30, createToggle("Warden Drop", "rules.warden", Material.ECHO_SHARD, true));
        inv.setItem(31, createToggle("Cobweb Decay", "rules.cobweb_decay", Material.COBWEB, false));
        inv.setItem(32, createToggle("Pearl Catching", "rules.better_pearl_catching_hitbox", Material.ENDER_PEARL, true));
        inv.setItem(33, createToggle("Shield 5-Tick Delay Fix", "shield_tweaks.5_tick_delay_fix", Material.SHIELD, false));
        inv.setItem(34, createToggle("Shield Skip Vanilla Tick", "shield_tweaks.skip-vanilla-damage-tick-when-shield-blocked", Material.IRON_BLOCK, false));

        inv.setItem(37, createToggle("Item Drops Lifetime", "rules.drops", Material.CHEST, true));
        inv.setItem(38, createToggle("Immortal Item Drops", "rules.immortal_item", Material.NETHER_STAR, false));
        inv.setItem(39, createToggle("Custom Items Glow", "rules.make_custom_items_glow", Material.GLOWSTONE_DUST, false));
        inv.setItem(40, createToggle("Disable Vaults", "rules.disable_vaults", Material.VAULT, false));

        // Submenus
        inv.setItem(41, new ItemBuilder(Material.BEACON).name(ChatColor.AQUA + "Ritual Items").lore(ChatColor.GRAY + "Click to configure ritual items").build());
        inv.setItem(42, new ItemBuilder(Material.CRAFTER).name(ChatColor.GOLD + "One Time Craft Items").lore(ChatColor.GRAY + "Click to configure 1-time crafts").build());
        inv.setItem(43, new ItemBuilder(Material.BARRIER).name(ChatColor.RED + "Banned Items").lore(ChatColor.GRAY + "Click to configure banned items").build());

        // Nav
        inv.setItem(48, new ItemBuilder(Material.ARROW).name(ChatColor.GRAY + "No Previous Page").build());
        inv.setItem(49, new ItemBuilder(Material.PAPER).name(ChatColor.YELLOW + "Page 1 / 3").build());
        inv.setItem(50, new ItemBuilder(Material.ARROW).name(ChatColor.GREEN + "Next Page ->").build());

        player.openInventory(inv);
    }

    public static void openPage2(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_PAGE_2);
        fillBorder(inv);

        inv.setItem(10, createToggle("Netherite Ban", "rules.ban_netherite", Material.NETHERITE_INGOT, true));
        inv.setItem(11, createToggle("All Tipped Arrows Ban", "rules.ban_all_tipped_arrows", Material.TIPPED_ARROW, false));
        inv.setItem(12, createToggle("Tier 2 Potions Ban", "rules.ban_tier_2_potions", Material.SPLASH_POTION, false));
        inv.setItem(13, createToggle("Xaero Minimap Ban", "rules.ban_minimap", Material.MAP, true));
        inv.setItem(14, createToggle("Minimap Fairplay", "rules.fairplay_minimap", Material.COMPASS, false));
        inv.setItem(15, createToggle("Seed Crack Protection", "rules.ban_seed_cracking", Material.WHEAT_SEEDS, true));
        inv.setItem(16, createToggle("Anti-Health Indicators", "rules.anti_health_indicators", Material.RED_DYE, true));

        inv.setItem(19, createToggle("Always Health Indicators", "rules.always_health_indicators", Material.PINK_DYE, false));
        inv.setItem(20, createToggle("Naked Protection", "rules.naked_protection", Material.LEATHER_CHESTPLATE, false));
        inv.setItem(21, createToggle("AFK Protection", "rules.afk_protection", Material.CLOCK, false));
        inv.setItem(22, createToggle("Anti Draining", "rules.anti_draining", Material.WATER_BUCKET, true));
        inv.setItem(23, createToggle("Anti Stasis Chamber", "rules.anti_stasis_chamber", Material.ENDER_EYE, true));
        inv.setItem(24, createToggle("Death Ban", "rules.death_ban", Material.SKELETON_SKULL, false));
        inv.setItem(25, createToggle("Death Spectator", "rules.spectator", Material.ENDER_CHEST, false));

        inv.setItem(28, createToggle("Hide Killer Name", "rules.hide_killer", Material.NAME_TAG, false));
        inv.setItem(29, createToggle("Hide Killed Name", "rules.hide_killed", Material.NAME_TAG, false));
        inv.setItem(30, createToggle("Disable Nether", "rules.disable_nether", Material.NETHERRACK, false));
        inv.setItem(31, createToggle("Disable End", "rules.disable_end", Material.END_STONE, false));
        inv.setItem(32, createToggle("String Dupers Fix", "rules.string_dupers", Material.STRING, false));
        inv.setItem(33, createToggle("/string Command", "rules.slash_string", Material.COBWEB, false));
        inv.setItem(34, createNumber("/string Cooldown", "rules.slash_string_cooldown", Material.CLOCK, 30, 0, 300));

        // Submenus
        inv.setItem(41, new ItemBuilder(Material.ENCHANTED_BOOK).name(ChatColor.LIGHT_PURPLE + "Enchant Bans").lore(ChatColor.GRAY + "Configure Prot & Sharp limits").build());
        inv.setItem(42, new ItemBuilder(Material.BREWING_STAND).name(ChatColor.DARK_PURPLE + "Potion Bans").lore(ChatColor.GRAY + "Configure potion effect bans").build());
        inv.setItem(43, new ItemBuilder(Material.SPECTRAL_ARROW).name(ChatColor.GREEN + "Tipped Arrow Bans").lore(ChatColor.GRAY + "Configure arrow bans").build());

        // Nav
        inv.setItem(48, new ItemBuilder(Material.ARROW).name(ChatColor.GREEN + "<- Previous Page").build());
        inv.setItem(49, new ItemBuilder(Material.PAPER).name(ChatColor.YELLOW + "Page 2 / 3").build());
        inv.setItem(50, new ItemBuilder(Material.ARROW).name(ChatColor.GREEN + "Next Page ->").build());

        player.openInventory(inv);
    }

    public static void openPage3(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_PAGE_3);
        fillBorder(inv);

        inv.setItem(10, createToggle("Attribute Swapping", "rules.attributeSwapping", Material.GOLDEN_HELMET, false));
        inv.setItem(11, createToggle("Hide Durability", "anticheat.obfuscation.items.hide-durability", Material.DIAMOND_PICKAXE, false));
        inv.setItem(12, createToggle("Anti-Xray Enabled", "anticheat.anti-xray.enabled", Material.DIAMOND_ORE, true));
        inv.setItem(13, createNumber("Anti-Xray Engine Mode", "anticheat.anti-xray.engine-mode", Material.DEEPSLATE_DIAMOND_ORE, 1, 1, 2));
        inv.setItem(14, createToggle("Anti-Alt System", "rules.anti_alt", Material.PLAYER_HEAD, true));
        inv.setItem(15, createToggle("Anti-VPN System", "rules.anti_vpn", Material.TRIPWIRE_HOOK, true));
        inv.setItem(16, createToggle("Protect Villagers", "rules.ban_killing_villagers", Material.EMERALD, false));

        inv.setItem(19, createToggle("Click Villager Shovel", "rules.clickVillager", Material.IRON_SHOVEL, false));
        inv.setItem(20, createToggle("Click Villager Anchor", "rules.clickVillager_anchor", Material.ANVIL, false));
        inv.setItem(21, createToggle("Infinite Villager Restock", "rules.infinite_restock", Material.VILLAGER_SPAWN_EGG, false));
        inv.setItem(22, createToggle("Easy Gapples Recipe", "easy_recipes.easy_gaps", Material.GOLDEN_APPLE, false));
        inv.setItem(23, createToggle("Easy Cobwebs Recipe", "easy_recipes.easy_cobwebs", Material.COBWEB, false));
        inv.setItem(24, createToggle("Continuous Item Limit", "heavy.continuous_item_limit", Material.HOPPER, true));
        inv.setItem(25, createToggle("Item Limit Enabled", "heavy.item_limit_enabled", Material.CHEST_MINECART, true));

        inv.setItem(28, createNumber("Pearl Cooldown", "rules.ender_pearl", Material.ENDER_PEARL, 0, 0, 60));
        inv.setItem(29, createNumber("Gapple Cooldown", "rules.gap", Material.GOLDEN_APPLE, 0, 0, 60));
        inv.setItem(30, createNumber("Wind Charge Cooldown", "rules.wind_charge", Material.WIND_CHARGE, 0, 0, 60));
        inv.setItem(31, createNumber("Trident Cooldown", "rules.trident", Material.TRIDENT, 0, 0, 60));
        inv.setItem(32, createNumber("Shield Cooldown", "rules.shield_cooldown", Material.SHIELD, 0, 0, 60));
        inv.setItem(33, createNumber("Mace Cooldown", "rules.mace", Material.MACE, 0, 0, 60));

        // Nav
        inv.setItem(48, new ItemBuilder(Material.ARROW).name(ChatColor.GREEN + "<- Previous Page").build());
        inv.setItem(49, new ItemBuilder(Material.PAPER).name(ChatColor.YELLOW + "Page 3 / 3").build());
        inv.setItem(50, new ItemBuilder(Material.ARROW).name(ChatColor.GRAY + "No Next Page").build());

        player.openInventory(inv);
    }

    public static void openEnchantBans(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_ENCHANT);
        fillBorder(inv);

        inv.setItem(11, createNumber("Max Protection", "rules.protection", Material.ENCHANTED_BOOK, 4, 1, 4));
        inv.setItem(13, createNumber("Max Sharpness", "rules.sharpness", Material.ENCHANTED_BOOK, 5, 1, 5));
        inv.setItem(15, createToggle("Allow Anvil Exceed Limit", "config.allow_anvils_to_exceed_enchant_limit", Material.ANVIL, false));

        inv.setItem(22, new ItemBuilder(Material.ARROW).name(ChatColor.RED + "<- Back to Settings").build());
        player.openInventory(inv);
    }

    public static void openPotionBans(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_POTION);
        fillBorder(inv);

        inv.setItem(11, createToggle("Ban Tier 2 Potions", "rules.ban_tier_2_potions", Material.POTION, false));
        inv.setItem(13, createToggle("Ban Strength", "banned-tier1-effects.strength", Material.BLAZE_POWDER, false));
        inv.setItem(14, createToggle("Ban Speed", "banned-tier1-effects.speed", Material.SUGAR, false));
        inv.setItem(15, createToggle("Ban Invisibility", "banned-tier1-effects.invisibility", Material.FERMENTED_SPIDER_EYE, false));

        inv.setItem(22, new ItemBuilder(Material.ARROW).name(ChatColor.RED + "<- Back to Settings").build());
        player.openInventory(inv);
    }

    public static void openTippedArrowBans(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_TIPPED);
        fillBorder(inv);

        inv.setItem(11, createToggle("Ban All Tipped Arrows", "rules.ban_all_tipped_arrows", Material.TIPPED_ARROW, false));
        inv.setItem(13, createToggle("Ban Slowness Arrows", "banned-tipped.slowness", Material.ARROW, false));
        inv.setItem(14, createToggle("Ban Weakness Arrows", "banned-tipped.weakness", Material.ARROW, false));
        inv.setItem(15, createToggle("Ban Harming Arrows", "banned-tipped.harming", Material.ARROW, false));

        inv.setItem(22, new ItemBuilder(Material.ARROW).name(ChatColor.RED + "<- Back to Settings").build());
        player.openInventory(inv);
    }

    public static void openRitualItems(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_RITUAL);
        fillBorder(inv);

        inv.setItem(11, createNumber("Ritual Duration", "config.ritual_duration", Material.CLOCK, 60, 10, 300));
        inv.setItem(13, createNumber("Ritual Radius", "config.ritual_radius", Material.COMPASS, 5, 2, 20));
        inv.setItem(15, new ItemBuilder(Material.FIREWORK_STAR).name(ChatColor.GOLD + "Color: " + Main.getInstance().getConfig().getString("config.ritual_particle_color", "blue")).lore(ChatColor.GRAY + "Click to cycle colors").build());

        inv.setItem(22, new ItemBuilder(Material.ARROW).name(ChatColor.RED + "<- Back to Settings").build());
        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        if (title.equals(TITLE_PAGE_1) || title.equals(TITLE_PAGE_2) || title.equals(TITLE_PAGE_3) ||
                title.equals(TITLE_ENCHANT) || title.equals(TITLE_POTION) || title.equals(TITLE_TIPPED) ||
                title.equals(TITLE_RITUAL)) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) {
                return;
            }

            int slot = event.getRawSlot();

            // Back button
            if (slot == 22 && (title.equals(TITLE_ENCHANT) || title.equals(TITLE_POTION) || title.equals(TITLE_TIPPED) || title.equals(TITLE_RITUAL))) {
                player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_DISPENSE, 1.0f, 1.0f);
                openPage1(player);
                return;
            }

            // Navigation on main pages
            if (slot == 48) {
                if (title.equals(TITLE_PAGE_2)) {
                    player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_DISPENSE, 1.0f, 1.0f);
                    openPage1(player);
                } else if (title.equals(TITLE_PAGE_3)) {
                    player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_DISPENSE, 1.0f, 1.0f);
                    openPage2(player);
                }
                return;
            }
            if (slot == 50) {
                if (title.equals(TITLE_PAGE_1)) {
                    player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_DISPENSE, 1.0f, 1.0f);
                    openPage2(player);
                } else if (title.equals(TITLE_PAGE_2)) {
                    player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_DISPENSE, 1.0f, 1.0f);
                    openPage3(player);
                }
                return;
            }

            // Open Submenus from Page 1
            if (title.equals(TITLE_PAGE_1)) {
                if (slot == 41) { openRitualItems(player); return; }
                if (slot == 42) { player.closeInventory(); player.performCommand("saltar"); return; }
                if (slot == 43) { player.closeInventory(); player.performCommand("banitem"); return; }
            }
            // Open Submenus from Page 2
            if (title.equals(TITLE_PAGE_2)) {
                if (slot == 41) { openEnchantBans(player); return; }
                if (slot == 42) { openPotionBans(player); return; }
                if (slot == 43) { openTippedArrowBans(player); return; }
            }

            // Cycle ritual particle color
            if (title.equals(TITLE_RITUAL) && slot == 15) {
                List<String> colors = Arrays.asList("blue", "red", "fuchsia", "gray", "black", "white", "purple", "orange", "lime", "aqua", "yellow", "green");
                String cur = Main.getInstance().getConfig().getString("config.ritual_particle_color", "blue");
                int idx = colors.indexOf(cur.toLowerCase());
                String next = colors.get((idx + 1) % colors.size());
                Main.getInstance().getConfig().set("config.ritual_particle_color", next);
                Main.getInstance().saveConfig();
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.2f);
                openRitualItems(player);
                return;
            }

            // Extract configKey from lore
            if (clicked.hasItemMeta() && clicked.getItemMeta().hasLore()) {
                List<String> lore = clicked.getItemMeta().getLore();
                if (lore != null && !lore.isEmpty()) {
                    for (String line : lore) {
                        String plain = ChatColor.stripColor(line);
                        if (plain.startsWith("Config: ")) {
                            String key = plain.substring("Config: ".length()).trim();
                            FileConfiguration cfg = Main.getInstance().getConfig();

                            String displayName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
                            boolean isToggle = displayName.contains(": ENABLED") || displayName.contains(": DISABLED");
                            boolean isNumber = false;
                            for (String l : lore) {
                                if (ChatColor.stripColor(l).contains("Left Click: +1")) {
                                    isNumber = true;
                                    break;
                                }
                            }

                            if (isToggle) {
                                if (key.equals("rules.anti_health_indicators") && Bukkit.getPluginManager().getPlugin("packetevents") == null) {
                                    player.sendMessage("§cYou need PacketEvents for Anti Health Indicators to work");
                                    return;
                                }
                                if (key.equals("rules.ban_seed_cracking") && Bukkit.getPluginManager().getPlugin("packetevents") == null) {
                                    player.sendMessage("§cYou need PacketEvents for Anti Seed Cracker to work");
                                    return;
                                }

                                boolean current = cfg.getBoolean(key, false);
                                if (key.equals("rules.disable_nether")) {
                                    current = pl.smpcore.mechanics.DimensionListener.isNetherDisabled();
                                } else if (key.equals("rules.disable_end")) {
                                    current = pl.smpcore.mechanics.DimensionListener.isEndDisabled();
                                }
                                boolean newVal = !current;
                                cfg.set(key, newVal);
                                if (key.equals("rules.disable_nether")) {
                                    cfg.set("dimensions.allow_nether", !newVal);
                                } else if (key.equals("rules.disable_end")) {
                                    cfg.set("dimensions.allow_end", !newVal);
                                } else if (key.equals("rules.ban_carts")) {
                                    cfg.set("rules.ban_cart_exploding", newVal);
                                } else if (key.equals("rules.anti_alt")) {
                                    cfg.set("security.anti_alt", newVal);
                                } else if (key.equals("rules.anti_vpn")) {
                                    cfg.set("security.anti_vpn", newVal);
                                } else if (key.equals("rules.fairplay_minimap")) {
                                    cfg.set("rules.minimap_fair", newVal);
                                } else if (key.equals("rules.immortal_item")) {
                                    cfg.set("config.immortal_item", newVal);
                                } else if (key.equals("anticheat.obfuscation.items.hide-durability")) {
                                    cfg.set("rules.hide_armor_durability", newVal);
                                }
                                Main.getInstance().saveConfig();
                                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, newVal ? 1.2f : 0.8f);

                                if (key.equals("rules.always_health_indicators")) {
                                    Main.getInstance().setScoreboardHealthIndicator(newVal);
                                } else if (key.equals("rules.anti_health_indicators") || key.equals("rules.ban_seed_cracking")) {
                                    if (Bukkit.getPluginManager().getPlugin("packetevents") != null) {
                                        pl.smpcore.packet.PacketEventsHook.updateListeners();
                                    }
                                } else if (key.equals("shield_tweaks.skip-vanilla-damage-tick-when-shield-blocked")) {
                                    Main.shieldStun();
                                } else if (key.equals("rules.attributeSwapping")) {
                                    Main.attributeSwapping();
                                } else if (key.equals("anticheat.anti-xray.enabled")) {
                                    Main.getInstance().antixray();
                                } else if (key.equals("anticheat.obfuscation.items.hide-durability")) {
                                    Main.getInstance().hideitemMeta();
                                }

                                // Refresh page
                                if (title.equals(TITLE_PAGE_1)) openPage1(player);
                                else if (title.equals(TITLE_PAGE_2)) openPage2(player);
                                else if (title.equals(TITLE_PAGE_3)) openPage3(player);
                                else if (title.equals(TITLE_ENCHANT)) openEnchantBans(player);
                                else if (title.equals(TITLE_POTION)) openPotionBans(player);
                                else if (title.equals(TITLE_TIPPED)) openTippedArrowBans(player);
                                return;
                            } else if (isNumber || cfg.isInt(key) || cfg.isDouble(key)) {
                                int current = cfg.getInt(key, 0);
                                int delta = event.isRightClick() ? -1 : 1;
                                int newVal = Math.max(0, current + delta);
                                cfg.set(key, newVal);
                                Main.getInstance().saveConfig();
                                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);

                                // Refresh page
                                if (title.equals(TITLE_PAGE_1)) openPage1(player);
                                else if (title.equals(TITLE_PAGE_2)) openPage2(player);
                                else if (title.equals(TITLE_PAGE_3)) openPage3(player);
                                else if (title.equals(TITLE_ENCHANT)) openEnchantBans(player);
                                else if (title.equals(TITLE_RITUAL)) openRitualItems(player);
                                return;
                            }
                        }
                    }
                }
            }
        }
    }
}
