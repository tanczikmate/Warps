package me.tanczikmate.warps;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.Map;

public record Warp(
        String name,
        String worldName,
        double x,
        double y,
        double z,
        float yaw,
        float pitch
) {

    public Location toLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        return new Location(world, x, y, z, yaw, pitch);
    }

    public static Warp fromLocation(String name, Location location) {
        String worldName = location.getWorld() != null ? location.getWorld().getName() : "world";
        return new Warp(
                name.trim(),
                worldName,
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch()
        );
    }

    public Map<String, Object> serialize() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", name);
        map.put("world", worldName);
        map.put("x", x);
        map.put("y", y);
        map.put("z", z);
        map.put("yaw", yaw);
        map.put("pitch", pitch);
        return map;
    }

    public static Warp deserialize(String key, ConfigurationSection section) {
        String name = section.getString("name", key);
        String world = section.getString("world", "world");
        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        float yaw = (float) section.getDouble("yaw");
        float pitch = (float) section.getDouble("pitch");
        return new Warp(name, world, x, y, z, yaw, pitch);
    }
}
