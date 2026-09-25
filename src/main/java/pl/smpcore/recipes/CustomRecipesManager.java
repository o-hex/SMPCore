package pl.smpcore.recipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import pl.Main;

import java.util.List;
import java.util.Map;

public class CustomRecipesManager {

    public static void registerRecipes() {
        FileConfiguration config = Main.getInstance().getConfig();
        List<Map<?, ?>> customCraftItems = config.getMapList("custom_craft_items");
        if (customCraftItems.isEmpty()) {
            return;
        }

        for (int i = 0; i < 54; i++) {
            List<?> ingredientList = config.getList("custom_recipes." + i);
            if (ingredientList == null || ingredientList.isEmpty()) {
                continue;
            }

            if (i >= customCraftItems.size()) {
                continue;
            }

            ItemStack resultItem = null;
            try {
                Map<?, ?> map = customCraftItems.get(i);
                @SuppressWarnings("unchecked")
                Map<String, Object> castMap = (Map<String, Object>) map;
                resultItem = ItemStack.deserialize(castMap);
            } catch (Exception e) {
                Main.getInstance().getLogger().warning("Failed to deserialize custom craft item index " + i);
                continue;
            }

            if (resultItem == null || resultItem.getType() == Material.AIR) {
                continue;
            }

            NamespacedKey key = new NamespacedKey(Main.getInstance(), "custom_recipe_" + i);
            try {
                // Remove existing if any
                Bukkit.removeRecipe(key);

                ShapedRecipe recipe = new ShapedRecipe(key, resultItem);
                recipe.shape("ABC", "DEF", "GHI");

                boolean hasIngredient = false;
                char[] chars = new char[]{'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I'};
                for (int slot = 0; slot < 9 && slot < ingredientList.size(); slot++) {
                    Object entry = ingredientList.get(slot);
                    ItemStack ing = null;
                    if (entry instanceof ItemStack itemStack) {
                        ing = itemStack;
                    } else if (entry instanceof Map<?, ?> ingMap) {
                        try {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> castMap = (Map<String, Object>) ingMap;
                            ing = ItemStack.deserialize(castMap);
                        } catch (Exception ignored) {}
                    } else if (entry instanceof String matName) {
                        Material mat = Material.matchMaterial(matName);
                        if (mat != null) {
                            ing = new ItemStack(mat);
                        }
                    }

                    if (ing != null && ing.getType() != Material.AIR) {
                        recipe.setIngredient(chars[slot], ing.getType());
                        hasIngredient = true;
                    }
                }

                if (hasIngredient) {
                    Bukkit.addRecipe(recipe);
                }
            } catch (Exception e) {
                Main.getInstance().getLogger().warning("Could not register custom recipe: " + key + " - " + e.getMessage());
            }
        }
    }

    public static void unregisterRecipes() {
        for (int i = 0; i < 54; i++) {
            NamespacedKey key = new NamespacedKey(Main.getInstance(), "custom_recipe_" + i);
            try {
                Bukkit.removeRecipe(key);
            } catch (Exception ignored) {}
        }
    }
}
