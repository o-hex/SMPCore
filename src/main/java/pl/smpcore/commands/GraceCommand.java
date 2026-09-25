package pl.smpcore.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.util.Vector;
import pl.Main;
import pl.smpcore.cooldown.CooldownManager;

import java.util.List;

public class GraceCommand implements CommandExecutor {
    private static BukkitTask graceTask = null;

    public static void startGraceTask() {
        if (graceTask != null) {
            graceTask.cancel();
            graceTask = null;
        }

        graceTask = new BukkitRunnable() {
            @Override
            public void run() {
                double remaining = CooldownManager.getGlobalCooldownRemaining("grace");
                if (remaining <= 0.0) {
                    cancel();
                    graceTask = null;

                    String title = Main.getInstance().getConfig().getString("config.grace_end_title", "§aGRACE IS OVER!");
                    String subtitle = Main.getInstance().getConfig().getString("config.grace_end_subtitle", "§7PvP has been enabled");
                    String msg = Main.getInstance().getConfig().getString("config.grace_end_message", "");

                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                        p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
                        p.sendTitle(title, subtitle, 10, 70, 20);
                        if (!msg.isEmpty()) p.sendMessage(msg);
                    }

                    Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
                    Objective obj = sb.getObjective("grace");
                    if (obj != null) {
                        obj.unregister();
                    }
                    return;
                }

                int mins = (int)(remaining / 60.0);
                int secs = (int)(remaining % 60.0);
                String timeStr = String.format("%02d:%02d", mins, secs);

                Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
                Objective obj = sb.getObjective("grace");
                if (obj == null) {
                    obj = sb.registerNewObjective("grace", "dummy", ChatColor.GREEN + "ɢʀᴀᴄᴇ");
                    obj.setDisplaySlot(DisplaySlot.SIDEBAR);
                } else {
                    for (String entry : sb.getEntries()) {
                        sb.resetScores(entry);
                    }
                }
                obj.getScore(ChatColor.WHITE + "⌚ " + timeStr).setScore(0);
            }
        }.runTaskTimer(Main.getInstance(), 0L, 10L);
    }

    public static void stopGrace() {
        CooldownManager.setGlobalCooldown("grace", 0.0);
        if (graceTask != null) {
            graceTask.cancel();
            graceTask = null;
        }
        Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
        Objective obj = sb.getObjective("grace");
        if (obj != null) {
            obj.unregister();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmd = command.getName().toLowerCase();

        if (cmd.equals("stopgrace")) {
            if (!sender.hasPermission("smpcore.start")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }
            stopGrace();
            sender.sendMessage(ChatColor.GREEN + "Grace period has been stopped!");
            return true;
        }

        if (cmd.equals("start")) {
            if (!sender.hasPermission("smpcore.start")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }

            boolean launch = Main.getInstance().getConfig().getBoolean("config.launch", false);
            int launchStrength = Main.getInstance().getConfig().getInt("config.launch_strength", 4);
            int graceDuration = Main.getInstance().getConfig().getInt("config.grace_duration", 40);
            int borderSize = Main.getInstance().getConfig().getInt("config.start_border_size", 40);
            int borderSpeed = Main.getInstance().getConfig().getInt("config.start_border_speed", 60);

            List<String> startCmds = Main.getInstance().getConfig().getStringList("config.start_commands");
            for (String c : startCmds) {
                c = c.replace("<sender>", sender.getName());
                Bukkit.dispatchCommand(sender, c);
            }

            String title = Main.getInstance().getConfig().getString("config.start_smp_title", ChatColor.GREEN + "GRACE")
                    .replace("%time%", String.valueOf(graceDuration));
            String subtitle = Main.getInstance().getConfig().getString("config.start_smp_subtitle", ChatColor.GREEN + "GRACE")
                    .replace("%time%", String.valueOf(graceDuration));
            String startMsg = Main.getInstance().getConfig().getString("config.start_smp_message", "");

            CooldownManager.setGlobalCooldown("grace", graceDuration * 60);
            startGraceTask();

            int steakCount = Main.getInstance().getConfig().getInt("config.start_steak", 0);

            for (Player player : Bukkit.getOnlinePlayers()) {
                if (steakCount > 0) {
                    player.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, steakCount));
                }
                if (!startMsg.isEmpty()) {
                    player.sendMessage(startMsg);
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
                            CooldownManager.setCooldown(player.getUniqueId(), "gliding", 0.2);
                            if (player.isOnGround() || player.isInWater() || player.isInLava()) {
                                cancel();
                            }
                        }
                    }.runTaskTimer(Main.getInstance(), 5L, 1L);
                }

                player.getWorld().getWorldBorder().setSize(borderSize, borderSpeed);
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
                player.sendTitle(title, subtitle, 10, 70, 20);
            }

            return true;
        }

        return false;
    }
}
