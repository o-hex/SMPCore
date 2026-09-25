package pl.smpcore.mechanics;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import pl.Main;

public class EasyRecipesManager {
    public static void registerEasyGaps() {
        NamespacedKey key = new NamespacedKey(Main.getInstance(), "easy_gap");
        if (Bukkit.getRecipe(key) != null) return;

        ShapedRecipe recipe = new ShapedRecipe(key, new ItemStack(Material.GOLDEN_APPLE));
        recipe.shape("GGG", "GAG", "GGG");
        recipe.setIngredient('G', Material.GOLD_INGOT);
        recipe.setIngredient('A', Material.APPLE);
        Bukkit.addRecipe(recipe);
    }

    public static void registerEasyCobwebs() {
        NamespacedKey key = new NamespacedKey(Main.getInstance(), "easy_cobweb");
        if (Bukkit.getRecipe(key) != null) return;

        ShapedRecipe recipe = new ShapedRecipe(key, new ItemStack(Material.COBWEB));
        recipe.shape(" S ", "SSS", " S ");
        recipe.setIngredient('S', Material.STRING);
        Bukkit.addRecipe(recipe);
    }
}
