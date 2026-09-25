package pl.smpcore.commands;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.Main;
import pl.smpcore.cooldown.CooldownManager;

public class StringCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!Main.getInstance().getConfig().getBoolean("rules.slash_string", false)) {
            sender.sendMessage(ChatColor.RED + "/string is disabled");
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        if (CooldownManager.hasCooldown(player.getUniqueId(), "string")) {
            double remaining = CooldownManager.getCooldownRemaining(player.getUniqueId(), "string");
            player.sendMessage(ChatColor.RED + "You must wait " + ChatColor.YELLOW + remaining + " " + ChatColor.RED + "seconds before you can use /string again");
            return true;
        }

        int cooldown = Main.getInstance().getConfig().getInt("rules.slash_string_cooldown", 30);
        CooldownManager.setCooldown(player.getUniqueId(), "string", cooldown);

        for (int slot = 0; slot < 36; slot++) {
            if (player.getInventory().getItem(slot) == null) {
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                player.getInventory().setItem(slot, new ItemStack(Material.STRING, 64));
            }
        }
        return true;
    }
}
