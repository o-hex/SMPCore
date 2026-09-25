package pl.smpcore.mechanics;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.json.JSONObject;
import pl.Main;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class SecurityListener implements Listener {
    private static File altsFile;
    private static YamlConfiguration altsConfig;

    private static synchronized void initAlts() {
        if (altsFile == null) {
            altsFile = new File(Main.getInstance().getDataFolder(), "alts.yml");
            if (!altsFile.exists()) {
                try {
                    altsFile.createNewFile();
                } catch (IOException ignored) {}
            }
            altsConfig = YamlConfiguration.loadConfiguration(altsFile);
        }
    }

    private static synchronized void saveAlts() {
        try {
            altsConfig.save(altsFile);
        } catch (IOException ignored) {}
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        String ip = event.getAddress().getHostAddress();
        String name = event.getName();

        // 1. Anti-VPN Check
        if (Main.getInstance().getConfig().getBoolean("rules.anti_vpn", false)) {
            if (!ip.equals("127.0.0.1") && !ip.startsWith("192.168.") && !ip.startsWith("10.")) {
                try {
                    URL url = new URL("http://ip-api.com/json/" + ip + "?fields=status,proxy,hosting");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(2000);
                    conn.setReadTimeout(2000);
                    conn.setRequestMethod("GET");

                    if (conn.getResponseCode() == 200) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = in.readLine()) != null) sb.append(line);
                        in.close();

                        JSONObject json = new JSONObject(sb.toString());
                        if (json.optString("status").equals("success")) {
                            boolean proxy = json.optBoolean("proxy", false);
                            boolean hosting = json.optBoolean("hosting", false);
                            if (proxy || hosting) {
                                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                                        ChatColor.RED + "VPN or Proxy connections are not permitted on this server!");
                                return;
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

        // 2. Anti-Alt Check
        if (Main.getInstance().getConfig().getBoolean("rules.anti_alt", false)) {
            initAlts();
            String safeIp = ip.replace('.', '_');
            List<String> accounts = altsConfig.getStringList("ips." + safeIp);

            if (!accounts.contains(name)) {
                int maxAlts = Main.getInstance().getConfig().getInt("rules.max_alts_per_ip", 1);
                if (accounts.size() >= maxAlts) {
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                            ChatColor.RED + "You have reached the maximum number of accounts allowed for your IP address!");
                    return;
                }
                accounts.add(name);
                altsConfig.set("ips." + safeIp, accounts);
                saveAlts();
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Minimap fairplay message (Xaero Minimap entity radar & cave disable protocol)
        if (Main.getInstance().getConfig().getBoolean("rules.fairplay_minimap", false)) {
            player.sendMessage("§3§6§3§6§3§6§e");
        }
    }
}
