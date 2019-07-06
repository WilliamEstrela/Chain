package br.com.craftlife.chain.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;

public class LocationUtils {

    public static String serialize(Location location) {
        return location.getWorld().getName() + ";"
                + location.getX() + ";"
                + location.getY() + ";"
                + location.getZ() + ";"
                + location.getYaw() + ";"
                + location.getPitch();
    }

    public static Location deserialize(String locationSerialized) {
        String[] args = locationSerialized.split(";");
        return new Location(
                Bukkit.getWorld(args[0]),
                Double.valueOf(args[1]),
                Double.valueOf(args[2]),
                Double.valueOf(args[3]),
                Float.valueOf(args[4]),
                Float.valueOf(args[5]));
    }
}
