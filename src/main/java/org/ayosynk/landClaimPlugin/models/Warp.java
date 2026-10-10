package org.ayosynk.landClaimPlugin.models;

import org.bukkit.Location;
import org.bukkit.Material;

public class Warp {
    private String name;
    private Location location;
    private String worldName;
    private Material icon;
    private boolean isPublic;

    public Warp(String name, Location location, Material icon) {
        this(name, location, icon, false);
    }

    public Warp(String name, Location location, Material icon, boolean isPublic) {
        this.name = name;
        this.location = location;
        this.worldName = location != null && location.getWorld() != null ? location.getWorld().getName() : null;
        this.icon = icon;
        this.isPublic = isPublic;
    }

    public Warp(String name, String worldName, double x, double y, double z, float yaw, float pitch,
            Material icon, boolean isPublic) {
        this.name = name;
        this.worldName = worldName;
        this.location = new Location(null, x, y, z, yaw, pitch);
        this.icon = icon;
        this.isPublic = isPublic;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
        this.worldName = location != null && location.getWorld() != null ? location.getWorld().getName() : null;
    }

    public String getWorldName() {
        return worldName;
    }

    public Material getIcon() {
        return icon;
    }

    public void setIcon(Material icon) {
        this.icon = icon;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }
}
