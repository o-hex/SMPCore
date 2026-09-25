package pl.smpcore.altars;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class AltarIngredient {
    private final Material material;
    private final int amount;
    private final int customModelData;

    public AltarIngredient(Material material, int amount, int customModelData) {
        this.material = material;
        this.amount = amount;
        this.customModelData = customModelData;
    }

    public Material getMaterial() {
        return material;
    }

    public int getAmount() {
        return amount;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public String getFormattedName() {
        if (material == null) return "Unknown";
        String[] parts = material.name().toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isEmpty()) continue;
            sb.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i].substring(1));
            if (i < parts.length - 1) sb.append(" ");
        }
        return sb.toString();
    }

    public boolean matches(ItemStack stack) {
        if (stack == null || stack.getType() != material) return false;
        if (customModelData > 0) {
            ItemMeta meta = stack.getItemMeta();
            if (meta == null || !meta.hasCustomModelData() || meta.getCustomModelData() != customModelData) {
                return false;
            }
        }
        return true;
    }
}
