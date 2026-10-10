package org.ayosynk.landClaimPlugin.db;

import org.ayosynk.landClaimPlugin.LandClaimPlugin;
import org.ayosynk.landClaimPlugin.models.Warp;
import org.bukkit.Material;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SQLWarpDao implements WarpDao {

    private final LandClaimPlugin plugin;
    private final DatabaseManager dbManager;

    public SQLWarpDao(LandClaimPlugin plugin, DatabaseManager dbManager) {
        this.plugin = plugin;
        this.dbManager = dbManager;
    }

    @Override
    public void createTables() {
        String tablePrefix = plugin.getConfigManager().getPluginConfig().database.tablePrefix;

        String sql = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "warps (" +
                "owner_id VARCHAR(36) NOT NULL," +
                "name VARCHAR(64) NOT NULL," +
                "world VARCHAR(64) NOT NULL," +
                "x DOUBLE NOT NULL," +
                "y DOUBLE NOT NULL," +
                "z DOUBLE NOT NULL," +
                "yaw FLOAT NOT NULL," +
                "pitch FLOAT NOT NULL," +
                "icon VARCHAR(64) NOT NULL," +
                "is_public BOOLEAN NOT NULL DEFAULT FALSE," +
                "PRIMARY KEY (owner_id, name))";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.execute();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create warps table: " + e.getMessage());
        }

        // Migration step: rename player_id to owner_id if upgrading from pre-v2
        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn
                        .prepareStatement("ALTER TABLE " + tablePrefix + "warps RENAME COLUMN player_id TO owner_id")) {
            stmt.execute();
        } catch (SQLException ignored) {
        }

        // Migration step: add icon column if upgrading from pre-v2
        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "ALTER TABLE " + tablePrefix + "warps ADD COLUMN icon VARCHAR(64) DEFAULT 'ENDER_PEARL'")) {
            stmt.execute();
        } catch (SQLException ignored) {
        }

        // Migration step: add is_public column for the public-warps feature.
        // Existing rows get the DEFAULT (false), so no data loss.
        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "ALTER TABLE " + tablePrefix + "warps ADD COLUMN is_public BOOLEAN NOT NULL DEFAULT FALSE")) {
            stmt.execute();
        } catch (SQLException ignored) {
        }
    }

    @Override
    public CompletableFuture<Map<UUID, Map<String, Warp>>> loadAllWarps() {
        return CompletableFuture.supplyAsync(() -> {
            Map<UUID, Map<String, Warp>> allWarps = new HashMap<>();
            String tablePrefix = plugin.getConfigManager().getPluginConfig().database.tablePrefix;
            String sql = "SELECT owner_id, name, world, x, y, z, yaw, pitch, icon, is_public FROM " + tablePrefix + "warps";

            try (Connection conn = dbManager.getConnection();
                    PreparedStatement stmt = conn.prepareStatement(sql);
                    ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    UUID ownerId = UUID.fromString(rs.getString("owner_id"));
                    String name = rs.getString("name");
                    String worldName = rs.getString("world");
                    double x = rs.getDouble("x");
                    double y = rs.getDouble("y");
                    double z = rs.getDouble("z");
                    float yaw = rs.getFloat("yaw");
                    float pitch = rs.getFloat("pitch");
                    String iconName = rs.getString("icon");
                    Material icon = Material.getMaterial(iconName != null ? iconName : "");
                    if (icon == null) {
                        icon = Material.ENDER_PEARL;
                    }
                    boolean isPublic = rs.getBoolean("is_public");

                    Warp warp = new Warp(name, worldName, x, y, z, yaw, pitch, icon, isPublic);

                    allWarps.computeIfAbsent(ownerId, k -> new HashMap<>()).put(name.toLowerCase(), warp);
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to load warps from database: " + e.getMessage());
            }
            return allWarps;
        });
    }

}
