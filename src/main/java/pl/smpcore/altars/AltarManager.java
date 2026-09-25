package pl.smpcore.altars;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import pl.Main;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AltarManager implements Listener {
    private final Main plugin;
    private final Map<Location, Altar> activeAltars = new ConcurrentHashMap<>();
    private BukkitTask rotationTask;
    private float currentAngle = 0.0f;

    public AltarManager(Main plugin) {
        this.plugin = plugin;
        startRotationTask();
    }

    private void startRotationTask() {
        if (rotationTask != null) {
            rotationTask.cancel();
        }
        rotationTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            int speed = plugin.getConfig().getInt("altarSettings.rotationSpeed", 2);
            currentAngle += (float) Math.toRadians(speed);
            if (currentAngle >= (float) (2 * Math.PI)) {
                currentAngle = 0.0f;
            }

            for (Altar altar : activeAltars.values()) {
                ItemDisplay id = altar.getItemDisplay();
                if (id != null && id.isValid()) {
                    Transformation t = id.getTransformation();
                    t.getLeftRotation().set(new AxisAngle4f(currentAngle, 0.0f, 1.0f, 0.0f));
                    id.setInterpolationDelay(0);
                    id.setInterpolationDuration(1);
                    id.setTransformation(t);
                }
            }
        }, 1L, 1L);
    }

    public void loadAltars() {
        cleanup();
        AltarConfigManager configManager = plugin.getConfigManager();
        if (configManager == null) return;

        Map<Location, String> spawned = configManager.getSpawnedAltars();
        for (Map.Entry<Location, String> entry : spawned.entrySet()) {
            Location loc = entry.getKey();
            String type = entry.getValue();
            Altar altar = configManager.loadAltarType(type, loc);
            if (altar != null && loc.getWorld() != null) {
                // Set block type
                String blockTypeName = plugin.getConfig().getString("altarSettings.blocktype", "LODESTONE");
                Material mat = Material.matchMaterial(blockTypeName);
                if (mat == null) mat = Material.LODESTONE;
                loc.getBlock().setType(mat);

                spawnDisplays(altar);
                activeAltars.put(loc.getBlock().getLocation(), altar);
            }
        }
    }

    public boolean spawnAltar(Location location, String type) {
        if (location == null || location.getWorld() == null) return false;
        AltarConfigManager configManager = plugin.getConfigManager();
        if (configManager == null) return false;

        if (!configManager.hasAltarType(type)) {
            return false;
        }

        Location blockLoc = location.getBlock().getLocation();
        // Remove any existing altar at this location
        removeAltar(blockLoc, false);

        Altar altar = configManager.loadAltarType(type, blockLoc);
        if (altar == null) return false;

        // Place altar block
        String blockTypeName = plugin.getConfig().getString("altarSettings.blocktype", "LODESTONE");
        Material mat = Material.matchMaterial(blockTypeName);
        if (mat == null) mat = Material.LODESTONE;
        blockLoc.getBlock().setType(mat);

        spawnDisplays(altar);
        activeAltars.put(blockLoc, altar);
        configManager.saveSpawnedAltar(blockLoc, type);
        return true;
    }

    public void spawnDisplays(Altar altar) {
        if (altar == null || altar.getLocation() == null || altar.getLocation().getWorld() == null) return;
        altar.removeDisplays();

        Location base = altar.getLocation().clone().add(0.5, 0, 0.5);

        // 1. ItemDisplay
        Location itemLoc = base.clone().add(0, altar.getItemHeight(), 0);
        ItemDisplay itemDisplay = (ItemDisplay) itemLoc.getWorld().spawn(itemLoc, ItemDisplay.class, id -> {
            id.setItemStack(altar.getReward());
            id.setInvulnerable(true);
            id.setGravity(false);
            Transformation t = id.getTransformation();
            t.getScale().set(1.0f, 1.0f, 1.0f);
            id.setTransformation(t);
        });
        altar.setItemDisplay(itemDisplay);

        // 2. Title TextDisplay (hologram-height - 0.25)
        ItemStack reward = altar.getReward();
        String displayName;
        if (reward != null && reward.hasItemMeta() && reward.getItemMeta().hasDisplayName()) {
            displayName = reward.getItemMeta().getDisplayName();
        } else if (reward != null && reward.getType() != Material.AIR) {
            displayName = formatMaterialName(reward.getType());
        } else {
            displayName = altar.getName();
        }

        Location titleLoc = base.clone().add(0, altar.getHologramHeight() - 0.25, 0);
        TextDisplay titleDisplay = (TextDisplay) titleLoc.getWorld().spawn(titleLoc, TextDisplay.class, td -> {
            td.setText("§6" + displayName);
            td.setShadowed(false);
            td.setBillboard(Display.Billboard.CENTER);
            td.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            td.setGravity(false);
        });
        altar.addTextDisplay(titleDisplay);

        // 3. Ingredient TextDisplays (hologram-height + i * 0.25)
        for (int i = 0; i < altar.getIngredients().size(); i++) {
            AltarIngredient ing = altar.getIngredients().get(i);
            String lineText = "§7" + ing.getAmount() + "x §f" + ing.getFormattedName();
            Location ingLoc = base.clone().add(0, altar.getHologramHeight() + (i * 0.25), 0);
            TextDisplay ingDisplay = (TextDisplay) ingLoc.getWorld().spawn(ingLoc, TextDisplay.class, td -> {
                td.setText(lineText);
                td.setShadowed(false);
                td.setBillboard(Display.Billboard.CENTER);
                td.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
                td.setGravity(false);
            });
            altar.addTextDisplay(ingDisplay);
        }
    }

    public static String formatMaterialName(Material mat) {
        if (mat == null) return "Unknown";
        String[] parts = mat.name().toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isEmpty()) continue;
            sb.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i].substring(1));
            if (i < parts.length - 1) sb.append(" ");
        }
        return sb.toString();
    }

    public Altar getAltarAt(Location location) {
        if (location == null) return null;
        return activeAltars.get(location.getBlock().getLocation());
    }

    public Altar getAltar(String type) {
        if (type == null) return null;
        for (Altar altar : activeAltars.values()) {
            if (altar.getType().equalsIgnoreCase(type)) {
                return altar;
            }
        }
        return null;
    }

    public Collection<Altar> getAllAltars() {
        return activeAltars.values();
    }

    public void removeAltar(Location location, boolean removeBlock) {
        if (location == null) return;
        Location blockLoc = location.getBlock().getLocation();
        Altar altar = activeAltars.remove(blockLoc);
        if (altar != null) {
            altar.removeDisplays();
        }
        if (removeBlock) {
            blockLoc.getBlock().setType(Material.AIR);
        }
        if (plugin.getConfigManager() != null) {
            plugin.getConfigManager().removeSpawnedAltar(blockLoc);
        }
    }

    public void broadcastCraft(Player player, Altar altar) {
        if (player == null || altar == null) return;
        String rawMsg = altar.getMessage();
        if (rawMsg == null || rawMsg.isEmpty()) return;

        ItemStack reward = altar.getReward();
        String itemName = reward != null && reward.hasItemMeta() && reward.getItemMeta().hasDisplayName()
                ? reward.getItemMeta().getDisplayName()
                : (reward != null ? formatMaterialName(reward.getType()) : altar.getName());

        Location loc = altar.getLocation() != null ? altar.getLocation() : player.getLocation();
        String locStr = loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ();
        String worldStr = loc.getWorld() != null ? loc.getWorld().getName() : "world";

        String msg = rawMsg.replace("<player>", player.getName())
                .replace("<item>", itemName)
                .replace("<location>", locStr)
                .replace("<world>", worldStr);

        Bukkit.broadcastMessage(msg);
    }

    public void cleanup() {
        for (Altar altar : activeAltars.values()) {
            altar.removeDisplays();
        }
        activeAltars.clear();
    }

    public void shutdown() {
        if (rotationTask != null) {
            rotationTask.cancel();
            rotationTask = null;
        }
        cleanup();
    }
}
