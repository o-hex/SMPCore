package pl.smpcore.altars;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import pl.Main;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class AltarConfigManager {
    private final Main plugin;
    private final File file;
    private FileConfiguration altarsYml;

    public AltarConfigManager(Main plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "altars.yml");
        load();
    }

    public void load() {
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException ignored) {}
        }
        altarsYml = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        try {
            altarsYml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save altars.yml: " + e.getMessage());
        }
    }

    public Set<String> getAltarTypes() {
        Set<String> types = new HashSet<>();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("altars");
        if (section != null) {
            types.addAll(section.getKeys(false));
        }
        return types;
    }

    public boolean hasAltarType(String type) {
        if (type == null) return false;
        return plugin.getConfig().contains("altars." + type.toLowerCase())
                || plugin.getConfig().contains("altars." + type);
    }

    public Altar loadAltarType(String type, Location location) {
        String path = "altars." + type.toLowerCase();
        FileConfiguration cfg = plugin.getConfig();
        if (!cfg.contains(path)) {
            path = "altars." + type;
            if (!cfg.contains(path)) {
                return null;
            }
        }

        Altar altar = new Altar(type, location);
        altar.setName(cfg.getString(path + ".name", type));
        altar.setHologramHeight(cfg.getDouble(path + ".hologram-height", 2.5));
        altar.setItemHeight(cfg.getDouble(path + ".item_height", 1.75));
        altar.setMessage(cfg.getString(path + ".message", "§f§l<player> §6§lhas crafted the §e§l<item>§6!"));
        altar.setEnabled(cfg.getBoolean(path + ".enabled", true));
        altar.setConsumeMaterials(cfg.getBoolean(path + ".consume_materials", true));
        altar.setRitual(cfg.getBoolean(path + ".ritual", false));

        ItemStack reward = cfg.getItemStack(path + ".reward");
        if (reward != null && reward.getType() != Material.AIR) {
            altar.setReward(reward.clone());
            altar.setItem(reward.clone());
        } else {
            // Default reward if not configured
            altar.setReward(new ItemStack(Material.NETHER_STAR));
            altar.setItem(new ItemStack(Material.NETHER_STAR));
        }

        List<AltarIngredient> ingredients = new ArrayList<>();
        List<Map<?, ?>> ingList = cfg.getMapList(path + ".recipe.ingredients");
        if (ingList != null && !ingList.isEmpty()) {
            for (Map<?, ?> map : ingList) {
                try {
                    String matName = String.valueOf(map.get("material"));
                    Material mat = Material.matchMaterial(matName);
                    int amount = map.containsKey("amount") ? Integer.parseInt(String.valueOf(map.get("amount"))) : 1;
                    int cmd = map.containsKey("custom_model_data") ? Integer.parseInt(String.valueOf(map.get("custom_model_data"))) : 0;
                    if (mat != null) {
                        ingredients.add(new AltarIngredient(mat, amount, cmd));
                    }
                } catch (Exception ignored) {}
            }
        }

        // If example altar and ingredients list was empty, set default 64 glowstone dust & 12 player heads
        if (ingredients.isEmpty() && type.equalsIgnoreCase("example")) {
            ingredients.add(new AltarIngredient(Material.GLOWSTONE_DUST, 64, 0));
            ingredients.add(new AltarIngredient(Material.PLAYER_HEAD, 12, 0));
        }

        altar.setIngredients(ingredients);
        return altar;
    }

    public void saveSpawnedAltar(Location loc, String type) {
        if (loc == null || loc.getWorld() == null) return;
        String key = "altars." + loc.getWorld().getName() + "_" + loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ();
        altarsYml.set(key + ".world", loc.getWorld().getName());
        altarsYml.set(key + ".x", loc.getBlockX());
        altarsYml.set(key + ".y", loc.getBlockY());
        altarsYml.set(key + ".z", loc.getBlockZ());
        altarsYml.set(key + ".type", type);
        save();
    }

    public void removeSpawnedAltar(Location loc) {
        if (loc == null || loc.getWorld() == null) return;
        String key = "altars." + loc.getWorld().getName() + "_" + loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ();
        altarsYml.set(key, null);
        save();
    }

    public Map<Location, String> getSpawnedAltars() {
        Map<Location, String> map = new HashMap<>();
        ConfigurationSection sec = altarsYml.getConfigurationSection("altars");
        if (sec == null) return map;

        for (String key : sec.getKeys(false)) {
            String worldName = sec.getString(key + ".world");
            if (worldName == null) continue;
            World world = Bukkit.getWorld(worldName);
            if (world == null) continue;

            int x = sec.getInt(key + ".x");
            int y = sec.getInt(key + ".y");
            int z = sec.getInt(key + ".z");
            String type = sec.getString(key + ".type", "example");

            map.put(new Location(world, x, y, z), type);
        }
        return map;
    }

    public void saveAltar(Altar altar) {
        if (altar == null) return;
        String path = "altars." + altar.getType().toLowerCase();
        FileConfiguration cfg = plugin.getConfig();
        cfg.set(path + ".name", altar.getName());
        cfg.set(path + ".hologram-height", altar.getHologramHeight());
        cfg.set(path + ".item_height", altar.getItemHeight());
        cfg.set(path + ".message", altar.getMessage());
        cfg.set(path + ".enabled", altar.isEnabled());
        cfg.set(path + ".consume_materials", altar.isConsumeMaterials());
        cfg.set(path + ".ritual", altar.isRitual());
        cfg.set(path + ".reward", altar.getReward());
        plugin.saveConfig();
    }
}
