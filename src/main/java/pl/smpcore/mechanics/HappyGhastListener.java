package pl.smpcore.mechanics;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import pl.Main;

public class HappyGhastListener implements Listener {

    public static boolean isSupported() {
        try {
            Class.forName("org.bukkit.entity.HappyGhast");
            return true;
        } catch (ClassNotFoundException e) {
            // Check if entity type exists
            try {
                org.bukkit.entity.EntityType.valueOf("HAPPY_GHAST");
                return true;
            } catch (IllegalArgumentException ignored) {
                return false;
            }
        }
    }

    private boolean isHappyGhast(Entity entity) {
        if (entity == null) return false;
        if ("HAPPY_GHAST".equalsIgnoreCase(entity.getType().name())) {
            return true;
        }
        return entity.getClass().getSimpleName().equalsIgnoreCase("HappyGhast");
    }

    @EventHandler
    public void onVehicleEnter(VehicleEnterEvent event) {
        Vehicle vehicle = event.getVehicle();
        if (isHappyGhast(vehicle) && vehicle instanceof LivingEntity living) {
            double speed = Main.getInstance().getConfig().getDouble("rules.happyGhastSpeed", 0.05);
            AttributeInstance attr = getFlyingSpeedAttribute(living);
            if (attr != null) {
                attr.setBaseValue(speed);
            }
        }
    }

    @EventHandler
    public void onVehicleExit(VehicleExitEvent event) {
        Vehicle vehicle = event.getVehicle();
        if (isHappyGhast(vehicle) && vehicle instanceof LivingEntity living) {
            AttributeInstance attr = getFlyingSpeedAttribute(living);
            if (attr != null) {
                attr.setBaseValue(0.05);
            }
        }
    }

    private AttributeInstance getFlyingSpeedAttribute(LivingEntity living) {
        try {
            Attribute attr = Attribute.valueOf("GENERIC_FLYING_SPEED");
            return living.getAttribute(attr);
        } catch (Exception e) {
            try {
                Attribute attr = Attribute.valueOf("FLYING_SPEED");
                return living.getAttribute(attr);
            } catch (Exception ignored) {
                return null;
            }
        }
    }
}
