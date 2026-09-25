package pl.smpcore.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import pl.Main;

import java.util.ArrayList;
import java.util.List;

public class RitualManager {
    public static final List<BossBar> activeBars = new ArrayList<>();
    public static BukkitTask currentTask = null;

    public static Color parseColor(String colorName) {
        if (colorName == null) return Color.BLUE;
        switch (colorName.toLowerCase()) {
            case "red": return Color.RED;
            case "fuchsia":
            case "pink": return Color.FUCHSIA;
            case "gray": return Color.GRAY;
            case "black": return Color.BLACK;
            case "white": return Color.WHITE;
            case "purple": return Color.PURPLE;
            case "orange": return Color.ORANGE;
            case "lime": return Color.LIME;
            case "aqua": return Color.AQUA;
            case "yellow": return Color.YELLOW;
            case "green": return Color.GREEN;
            case "blue":
            default: return Color.BLUE;
        }
    }

    public static BarColor parseBarColor(String colorName) {
        if (colorName == null) return BarColor.BLUE;
        switch (colorName.toLowerCase()) {
            case "red": return BarColor.RED;
            case "pink":
            case "fuchsia": return BarColor.PINK;
            case "purple": return BarColor.PURPLE;
            case "green":
            case "lime": return BarColor.GREEN;
            case "yellow": return BarColor.YELLOW;
            case "white": return BarColor.WHITE;
            default: return BarColor.BLUE;
        }
    }

    public static void startRitual(Location center) {
        cancelRitual();

        int duration = Main.getInstance().getConfig().getInt("config.ritual_duration", 60);
        int radius = Main.getInstance().getConfig().getInt("config.ritual_radius", 5);
        String colorStr = Main.getInstance().getConfig().getString("config.ritual_particle_color", "blue");
        Color dustColor = parseColor(colorStr);
        Particle.DustOptions dust = new Particle.DustOptions(dustColor, 1.5f);

        BossBar bar = Bukkit.createBossBar(ChatColor.AQUA + "" + ChatColor.BOLD + "Ritual Countdown", parseBarColor(colorStr), BarStyle.SOLID);
        for (Player p : Bukkit.getOnlinePlayers()) {
            bar.addPlayer(p);
        }
        activeBars.add(bar);

        final int totalTicks = duration * 20;

        currentTask = new BukkitRunnable() {
            int ticksLeft = totalTicks;

            @Override
            public void run() {
                if (ticksLeft <= 0) {
                    cancel();
                    activeBars.remove(bar);
                    bar.removeAll();

                    center.getWorld().strikeLightningEffect(center);
                    center.getWorld().playSound(center, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                    center.getWorld().dropItemNaturally(center.clone().add(0, 1, 0), new ItemStack(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 1));
                    Bukkit.broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "The ritual is complete! The reward has appeared!");
                    return;
                }

                // Particle circle
                double step = Math.PI / 16.0;
                for (double angle = 0; angle < 2 * Math.PI; angle += step) {
                    double x = center.getX() + radius * Math.cos(angle);
                    double z = center.getZ() + radius * Math.sin(angle);
                    center.getWorld().spawnParticle(Particle.DUST, x, center.getY() + 0.2, z, 1, dust);
                }

                // Update BossBar
                int secsRemaining = (ticksLeft + 19) / 20;
                int mins = secsRemaining / 60;
                int secs = secsRemaining % 60;
                bar.setTitle(ChatColor.AQUA + "" + ChatColor.BOLD + "Ritual: " + ChatColor.WHITE + String.format("%02d:%02d", mins, secs));
                bar.setProgress(Math.max(0.0, Math.min(1.0, (double) ticksLeft / totalTicks)));

                ticksLeft -= 5;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 5L);
    }

    public static void cancelRitual() {
        if (currentTask != null) {
            currentTask.cancel();
            currentTask = null;
        }
        for (BossBar bar : activeBars) {
            bar.removeAll();
        }
        activeBars.clear();
    }

    public static void cleanup() {
        cancelRitual();
    }
}
