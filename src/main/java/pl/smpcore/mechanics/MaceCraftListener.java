package pl.smpcore.mechanics;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import pl.Main;

import java.io.File;
import java.io.IOException;

public class MaceCraftListener implements Listener {
    private static File maceFile;
    private static YamlConfiguration maceConfig;

    private static void init() {
        if (maceFile == null) {
            maceFile = new File(Main.getInstance().getDataFolder(), "mace.yml");
            if (!maceFile.exists()) {
                try {
                    maceFile.createNewFile();
                } catch (IOException ignored) {}
            }
            maceConfig = YamlConfiguration.loadConfiguration(maceFile);
        }
    }

    private static void save() {
        try {
            maceConfig.save(maceFile);
        } catch (IOException ignored) {}
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareMaceCraft(PrepareItemCraftEvent event) {
        ItemStack result = event.getInventory().getResult();
        if (result != null && result.getType() == Material.MACE) {
            if (Main.getInstance().getConfig().getBoolean("rules.ban_mace", false)) {
                event.getInventory().setResult(new ItemStack(Material.AIR));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMaceCraft(CraftItemEvent event) {
        ItemStack result = event.getCurrentItem();
        if (result != null && result.getType() == Material.MACE) {
            if (Main.getInstance().getConfig().getBoolean("rules.ban_mace", false)) {
                event.setCancelled(true);
                if (event.getWhoClicked() instanceof Player) {
                    ((Player) event.getWhoClicked()).sendMessage(ChatColor.RED + "The Mace is banned on this server!");
                }
                return;
            }

            int maceLimit = Main.getInstance().getConfig().getInt("rules.mace_limit", 0);
            if (maceLimit > 0) {
                init();
                int craftedCount = maceConfig.getInt("crafted_total", 0);
                if (craftedCount >= maceLimit) {
                    event.setCancelled(true);
                    if (event.getWhoClicked() instanceof Player) {
                        ((Player) event.getWhoClicked()).sendMessage(ChatColor.RED + "The server mace crafting limit (" + maceLimit + ") has been reached!");
                    }
                    return;
                }
                maceConfig.set("crafted_total", craftedCount + 1);
                save();
            }
        }
    }
}
