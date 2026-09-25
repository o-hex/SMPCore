package pl.smpcore.mechanics;

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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SecurityListener implements Listener {

    private static File altsFile;
    private static YamlConfiguration altsConfig;

    private static final Map<String, VpnCacheEntry> vpnCache = new ConcurrentHashMap<>();

    private record VpnCacheEntry(boolean blocked, long expiresAt) {
        boolean expired() {
            return System.currentTimeMillis() >= expiresAt;
        }
    }

    private static synchronized void initAlts() {
        if (altsFile != null) return;

        altsFile = new File(Main.getInstance().getDataFolder(), "alts.yml");
        if (!altsFile.exists()) {
            try {
                altsFile.createNewFile();
            } catch (IOException e) {
            }
        }
        altsConfig = YamlConfiguration.loadConfiguration(altsFile);
    }

    private static synchronized void saveAlts() {
        try {
            altsConfig.save(altsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        String ip = event.getAddress().getHostAddress();
        String name = event.getName();

        if (isAntiVpnEnabled() && !isLocalIp(ip) && isVpnOrProxy(ip)) {
            event.disallow(
                    AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    ChatColor.RED + "VPN or Proxy connections are not permitted on this server!"
            );
            return;
        }

        if (isAntiAltEnabled()) {
            initAlts();

            String safeIp = ip.replace('.', '_');
            List<String> accounts = altsConfig.getStringList("ips." + safeIp);

            if (!accounts.contains(name)) {
                int maxAlts = Main.getInstance().getConfig().contains("security.max_alts_per_ip")
                        ? Main.getInstance().getConfig().getInt("security.max_alts_per_ip", 1)
                        : Main.getInstance().getConfig().getInt("rules.max_alts_per_ip", 1);

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

        boolean fairplay = Main.getInstance().getConfig().contains("rules.fairplay_minimap")
                ? Main.getInstance().getConfig().getBoolean("rules.fairplay_minimap", false)
                : Main.getInstance().getConfig().getBoolean("rules.minimap_fair", false);

        if (fairplay) {
            player.sendMessage("§3§6§3§6§3§6§e");
        }
    }

    private boolean isAntiVpnEnabled() {
        return Main.getInstance().getConfig().contains("security.anti_vpn")
                ? Main.getInstance().getConfig().getBoolean("security.anti_vpn", false)
                : Main.getInstance().getConfig().getBoolean("rules.anti_vpn", false);
    }

    private boolean isAntiAltEnabled() {
        return Main.getInstance().getConfig().contains("security.anti_alt")
                ? Main.getInstance().getConfig().getBoolean("security.anti_alt", false)
                : Main.getInstance().getConfig().getBoolean("rules.anti_alt", false);
    }

    private boolean isLocalIp(String ip) {
        return ip.equals("127.0.0.1") || ip.startsWith("192.168.") || ip.startsWith("10.");
    }

    private boolean isVpnOrProxy(String ip) {
        VpnCacheEntry cached = vpnCache.get(ip);
        if (cached != null && !cached.expired()) {
            return cached.blocked();
        }

        boolean blocked = false;
        try {
            URL url = new URL("http://ip-api.com/json/" + ip + "?fields=status,proxy,hosting");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(1500);
            conn.setReadTimeout(1500);
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() == 200) {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        sb.append(line);
                    }

                    JSONObject json = new JSONObject(sb.toString());
                    if ("success".equalsIgnoreCase(json.optString("status"))) {
                        blocked = json.optBoolean("proxy", false) || json.optBoolean("hosting", false);
                    }
                }
            }
        } catch (Exception e) {
            return false;
        }

        long cacheTime = blocked ? 6 * 60 * 60 * 1000L : 24 * 60 * 60 * 1000L;
        vpnCache.put(ip, new VpnCacheEntry(blocked, System.currentTimeMillis() + cacheTime));
        return blocked;
    }
}