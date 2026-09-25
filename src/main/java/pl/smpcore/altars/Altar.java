package pl.smpcore.altars;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class Altar {
    private final String type;
    private String name;
    private String message;
    private boolean enabled;
    private double hologramHeight;
    private double itemHeight;
    private ItemStack reward;
    private ItemStack displayItem;
    private boolean consumeMaterials;
    private boolean ritual;
    private List<AltarIngredient> ingredients = new ArrayList<>();
    private Location location;

    private ItemDisplay itemDisplay;
    private final List<TextDisplay> textDisplays = new ArrayList<>();

    public Altar(String type, Location location) {
        this.type = type;
        this.location = location;
        this.name = type;
        this.message = "§f§l<player> §6§lhas crafted the §e§l<item>§6!";
        this.enabled = true;
        this.hologramHeight = 2.5;
        this.itemHeight = 1.75;
        this.reward = new ItemStack(Material.NETHER_STAR, 1);
        this.displayItem = new ItemStack(Material.NETHER_STAR, 1);
        this.consumeMaterials = true;
        this.ritual = false;
    }

    public String getType() { return type; }

    public String getName() { return name != null ? name : type; }
    public void setName(String name) { this.name = name; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public double getHologramHeight() { return hologramHeight; }
    public void setHologramHeight(double hologramHeight) { this.hologramHeight = hologramHeight; }

    public double getItemHeight() { return itemHeight; }
    public void setItemHeight(double itemHeight) { this.itemHeight = itemHeight; }

    public ItemStack getReward() { return reward != null ? reward : new ItemStack(Material.NETHER_STAR); }
    public void setReward(ItemStack reward) { this.reward = reward; }

    public ItemStack getItem() { return displayItem != null ? displayItem : getReward(); }
    public void setItem(ItemStack item) { this.displayItem = item; }

    public boolean isConsumeMaterials() { return consumeMaterials; }
    public void setConsumeMaterials(boolean consumeMaterials) { this.consumeMaterials = consumeMaterials; }

    public boolean isRitual() { return ritual; }
    public void setRitual(boolean ritual) { this.ritual = ritual; }

    public List<AltarIngredient> getIngredients() { return ingredients; }
    public void setIngredients(List<AltarIngredient> ingredients) { this.ingredients = ingredients; }

    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }

    public ItemDisplay getItemDisplay() { return itemDisplay; }
    public void setItemDisplay(ItemDisplay itemDisplay) { this.itemDisplay = itemDisplay; }

    public List<TextDisplay> getTextDisplays() { return textDisplays; }
    public void addTextDisplay(TextDisplay textDisplay) {
        if (textDisplay != null) {
            this.textDisplays.add(textDisplay);
        }
    }

    public void removeDisplays() {
        if (itemDisplay != null && itemDisplay.isValid()) {
            itemDisplay.remove();
        }
        itemDisplay = null;

        for (TextDisplay td : textDisplays) {
            if (td != null && td.isValid()) {
                td.remove();
            }
        }
        textDisplays.clear();
    }
}
