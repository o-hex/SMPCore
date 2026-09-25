package pl.smpcore.mechanics;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Tripwire;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.inventory.ItemStack;
import pl.Main;

public class StringDuperListener implements Listener {
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onWaterFlow(BlockFromToEvent event) {
        if (!Main.getInstance().getConfig().getBoolean("rules.string_dupers", false)) return;

        Block toBlock = event.getToBlock();
        if (toBlock.getType() == Material.TRIPWIRE || toBlock.getBlockData() instanceof Tripwire) {
            int amount = Main.getInstance().getConfig().getInt("config.string_duper_string_per_break", 2);
            event.setCancelled(true);
            toBlock.setType(Material.AIR);
            if (amount > 0) {
                toBlock.getWorld().dropItemNaturally(toBlock.getLocation(), new ItemStack(Material.STRING, amount));
            }
        }
    }
}
