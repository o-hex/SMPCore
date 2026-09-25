package pl.smpcore.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;

public final class ChatUtil {
    private ChatUtil() {}

    public static String color(String message) {
        if (message == null) return "";
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static Component component(String message) {
        if (message == null) return Component.empty();
        return LegacyComponentSerializer.legacySection().deserialize(color(message));
    }
}
