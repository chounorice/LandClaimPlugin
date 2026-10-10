package org.ayosynk.landClaimPlugin.db;

import org.ayosynk.landClaimPlugin.LandClaimPlugin;
import org.ayosynk.landClaimPlugin.models.ChunkPosition;
import org.ayosynk.landClaimPlugin.models.ClaimProfile;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SQLProfileDao implements ProfileDao {

    private final LandClaimPlugin plugin;
    private final DatabaseManager dbManager;

    public SQLProfileDao(LandClaimPlugin plugin, DatabaseManager dbManager) {
        this.plugin = plugin;
        this.dbManager = dbManager;
    }

    private String prefix() {
        return plugin.getConfigManager().getPluginConfig().database.tablePrefix;
    }

    private boolean isSqlite() {
        return plugin.getConfigManager().getPluginConfig().database.type.equalsIgnoreCase("SQLITE");
    }

    @Override
    public void createTables() {
        String p = prefix();

        String[] sqls = {
                "CREATE TABLE IF NOT EXISTS " + p + "claim_profiles ("
                        + "owner_id VARCHAR(36) PRIMARY KEY,"
                        + "name VARCHAR(64) NOT NULL)",

                "CREATE TABLE IF NOT EXISTS " + p + "claimed_chunks ("
                        + "chunk_id VARCHAR(128) PRIMARY KEY,"
                        + "owner_id VARCHAR(36) NOT NULL)",

                "CREATE TABLE IF NOT EXISTS " + p + "profile_roles ("
                        + "id VARCHAR(36) PRIMARY KEY,"
                        + "owner_id VARCHAR(36) NOT NULL,"
                        + "name VARCHAR(64) NOT NULL,"
                        + "priority INT NOT NULL,"
                        + "flags TEXT NOT NULL)",

                "CREATE TABLE IF NOT EXISTS " + p + "profile_trusted_players ("
                        + "owner_id VARCHAR(36) NOT NULL,"
                        + "player_id VARCHAR(36) NOT NULL,"
                        + "flags TEXT NOT NULL,"
                        + "PRIMARY KEY (owner_id, player_id))",

                "CREATE TABLE IF NOT EXISTS " + p + "profile_visitor_flags ("
                        + "owner_id VARCHAR(36) NOT NULL,"
                        + "flag VARCHAR(64) NOT NULL,"
                        + "PRIMARY KEY (owner_id, flag))",

                "CREATE TABLE IF NOT EXISTS " + p + "profile_category_flags ("
                        + "owner_id VARCHAR(36) NOT NULL,"
                        + "category VARCHAR(16) NOT NULL,"
                        + "flag VARCHAR(64) NOT NULL,"
                        + "PRIMARY KEY (owner_id, category, flag))",

                "CREATE TABLE IF NOT EXISTS " + p + "profile_category_flag_sets ("
                        + "owner_id VARCHAR(36) NOT NULL,"
                        + "category VARCHAR(16) NOT NULL,"
                        + "PRIMARY KEY (owner_id, category))",

                "CREATE TABLE IF NOT EXISTS " + p + "profile_spawnpoints ("
                        + "owner_id VARCHAR(36) PRIMARY KEY,"
                        + "world_name VARCHAR(128) NOT NULL,"
                        + "x DOUBLE NOT NULL,"
                        + "y DOUBLE NOT NULL,"
                        + "z DOUBLE NOT NULL,"
                        + "yaw FLOAT NOT NULL,"
                        + "pitch FLOAT NOT NULL)",

                "CREATE TABLE IF NOT EXISTS " + p + "profile_member_roles ("
                        + "owner_id VARCHAR(36) NOT NULL,"
                        + "player_id VARCHAR(36) NOT NULL,"
                        + "role_name VARCHAR(64) NOT NULL,"
                        + "PRIMARY KEY (owner_id, player_id))",

                "CREATE TABLE IF NOT EXISTS " + p + "profile_legacy_member_roles ("
                        + "owner_id VARCHAR(36) NOT NULL,"
                        + "player_id VARCHAR(36) NOT NULL,"
                        + "role_name VARCHAR(64) NOT NULL,"
                        + "PRIMARY KEY (owner_id, player_id))",

                "CREATE TABLE IF NOT EXISTS " + p + "profile_ally_flags ("
                        + "owner_id VARCHAR(36) NOT NULL,"
                        + "ally_id VARCHAR(36) NOT NULL,"
                        + "flags TEXT NOT NULL,"
                        + "PRIMARY KEY (owner_id, ally_id))",

                "CREATE TABLE IF NOT EXISTS " + p + "profile_banned_players ("
                        + "owner_id VARCHAR(36) NOT NULL,"
                        + "player_id VARCHAR(36) NOT NULL,"
                        + "banned_at BIGINT NOT NULL DEFAULT 0,"
                        + "PRIMARY KEY (owner_id, player_id))"
        };

        try (Connection conn = dbManager.getDatabase().getConnection()) {
            for (String sql : sqls) {
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.executeUpdate();
                }
            }

            String archiveRoles = isSqlite()
                    ? "INSERT OR IGNORE INTO " + p
                            + "profile_legacy_member_roles (owner_id, player_id, role_name) SELECT owner_id, player_id, role_name FROM "
                            + p + "profile_member_roles"
                    : "INSERT IGNORE INTO " + p
                            + "profile_legacy_member_roles (owner_id, player_id, role_name) SELECT owner_id, player_id, role_name FROM "
                            + p + "profile_member_roles";
            try (PreparedStatement stmt = conn.prepareStatement(archiveRoles)) {
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE " + p + "profile_member_roles SET role_name = 'Resident' WHERE LOWER(role_name) <> 'resident'")) {
                stmt.executeUpdate();
            }

            // Migration: add claim_color and vis_mode columns if missing
            String[] alters = {
                    "ALTER TABLE " + p + "claim_profiles ADD COLUMN claim_color VARCHAR(16)",
                    "ALTER TABLE " + p + "claim_profiles ADD COLUMN vis_mode VARCHAR(32) DEFAULT 'DISPLAY_ENTITY'",
                    "ALTER TABLE " + p + "claim_profiles ADD COLUMN title_enabled BOOLEAN DEFAULT FALSE",
                    "ALTER TABLE " + p + "claim_profiles ADD COLUMN enter_title VARCHAR(255)",
                    "ALTER TABLE " + p + "claim_profiles ADD COLUMN leave_title VARCHAR(255)",
                    "ALTER TABLE " + p + "claim_profiles ADD COLUMN enter_title_mode VARCHAR(16) DEFAULT 'TITLE'",
                    "ALTER TABLE " + p + "claim_profiles ADD COLUMN leave_title_mode VARCHAR(16) DEFAULT 'SUBTITLE'",
                    "ALTER TABLE " + p + "claim_profiles ADD COLUMN owner_alias VARCHAR(64)",
                    "ALTER TABLE " + p + "claim_profiles ADD COLUMN pvp_enabled BOOLEAN DEFAULT FALSE",
                    "ALTER TABLE " + p + "claim_profiles ADD COLUMN pvp_timer_end BIGINT DEFAULT 0",
                    "ALTER TABLE " + p + "claim_profiles ADD COLUMN real_owner_id VARCHAR(36) NULL",
                    "ALTER TABLE " + p + "claim_profiles ADD COLUMN bonus_role_slots INT DEFAULT 0",
                    "ALTER TABLE " + p + "claim_profiles ADD COLUMN bonus_member_slots INT DEFAULT 0",
                    "ALTER TABLE " + p + "claim_profiles ADD COLUMN bonus_warp_slots INT DEFAULT 0"
            };
            for (String alter : alters) {
                try (PreparedStatement stmt = conn.prepareStatement(alter)) {
                    stmt.executeUpdate();
                } catch (SQLException ignored) {
                    // Column already exists
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create profile tables.");
            e.printStackTrace();
        }
    }

    @Override
    public CompletableFuture<Void> saveProfile(ClaimProfile profile) {
        return CompletableFuture.runAsync(() -> {
            String p = prefix();
            boolean sqlite = isSqlite();

            String upsertProfile = sqlite
                    ? "INSERT OR REPLACE INTO " + p
                            + "claim_profiles (owner_id, name, claim_color, vis_mode, title_enabled, enter_title, leave_title, enter_title_mode, leave_title_mode, owner_alias, pvp_enabled, pvp_timer_end, real_owner_id, bonus_role_slots, bonus_member_slots, bonus_warp_slots) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                    : "INSERT INTO " + p
                            + "claim_profiles (owner_id, name, claim_color, vis_mode, title_enabled, enter_title, leave_title, enter_title_mode, leave_title_mode, owner_alias, pvp_enabled, pvp_timer_end, real_owner_id, bonus_role_slots, bonus_member_slots, bonus_warp_slots) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE name=VALUES(name), claim_color=VALUES(claim_color), vis_mode=VALUES(vis_mode), title_enabled=VALUES(title_enabled), enter_title=VALUES(enter_title), leave_title=VALUES(leave_title), enter_title_mode=VALUES(enter_title_mode), leave_title_mode=VALUES(leave_title_mode), owner_alias=VALUES(owner_alias), pvp_enabled=VALUES(pvp_enabled), pvp_timer_end=VALUES(pvp_timer_end), real_owner_id=VALUES(real_owner_id), bonus_role_slots=VALUES(bonus_role_slots), bonus_member_slots=VALUES(bonus_member_slots), bonus_warp_slots=VALUES(bonus_warp_slots)";

            Connection conn = null;
            try {
                conn = dbManager.getDatabase().getConnection();
                conn.setAutoCommit(false);

                // 1. Upsert profile
                try (PreparedStatement stmt = conn.prepareStatement(upsertProfile)) {
                    stmt.setString(1, profile.getProfileId().toString());
                    stmt.setString(2, profile.getName());
                    stmt.setString(3, profile.getClaimColor());
                    stmt.setString(4, profile.getVisualizationMode());
                    stmt.setBoolean(5, profile.isEnterTitleEnabled());
                    stmt.setString(6, profile.getEnterTitle());
                    stmt.setString(7, profile.getLeaveTitle());
                    stmt.setString(8, profile.getEnterTitleMode());
                    stmt.setString(9, profile.getLeaveTitleMode());
                    stmt.setString(10, profile.getOwnerAlias());
                    stmt.setBoolean(11, profile.isPvpEnabled());
                    stmt.setLong(12, profile.getPvpTimerEnd());
                    stmt.setString(13, profile.getOwnerId() != null ? profile.getOwnerId().toString() : profile.getProfileId().toString());
                    stmt.setInt(14, profile.getBonusRoleSlots());
                    stmt.setInt(15, profile.getBonusMemberSlots());
                    stmt.setInt(16, profile.getBonusWarpSlots());
                    stmt.executeUpdate();
                }

                String oid = profile.getProfileId().toString();

                // 2. Clear and re-insert chunks
                clearTable(conn, p + "claimed_chunks", "owner_id", oid);
                if (!profile.getOwnedChunks().isEmpty()) {
                    String insertChunk = sqlite
                            ? "INSERT OR REPLACE INTO " + p + "claimed_chunks (chunk_id, owner_id) VALUES (?, ?)"
                            : "INSERT INTO " + p
                                    + "claimed_chunks (chunk_id, owner_id) VALUES (?, ?) ON DUPLICATE KEY UPDATE owner_id=VALUES(owner_id)";
                    try (PreparedStatement stmt = conn.prepareStatement(insertChunk)) {
                        for (ChunkPosition pos : profile.getOwnedChunks()) {
                            stmt.setString(1, pos.world() + ":" + pos.x() + ":" + pos.z());
                            stmt.setString(2, oid);
                            stmt.addBatch();
                        }
                        stmt.executeBatch();
                    }
                }

                // 3. Clear and re-insert visitor flags
                clearTable(conn, p + "profile_visitor_flags", "owner_id", oid);
                if (!profile.getVisitorFlags().isEmpty()) {
                    String insertFlag = sqlite
                            ? "INSERT OR REPLACE INTO " + p + "profile_visitor_flags (owner_id, flag) VALUES (?, ?)"
                            : "INSERT INTO " + p
                                    + "profile_visitor_flags (owner_id, flag) VALUES (?, ?) ON DUPLICATE KEY UPDATE flag=VALUES(flag)";
                    try (PreparedStatement stmt = conn.prepareStatement(insertFlag)) {
                        for (String flag : profile.getVisitorFlags()) {
                            stmt.setString(1, oid);
                            stmt.setString(2, flag);
                            stmt.addBatch();
                        }
                        stmt.executeBatch();
                    }
                }

                clearTable(conn, p + "profile_category_flags", "owner_id", oid);
                clearTable(conn, p + "profile_category_flag_sets", "owner_id", oid);
                String insertCategorySet = sqlite
                        ? "INSERT OR REPLACE INTO " + p
                                + "profile_category_flag_sets (owner_id, category) VALUES (?, ?)"
                        : "INSERT INTO " + p
                                + "profile_category_flag_sets (owner_id, category) VALUES (?, ?) ON DUPLICATE KEY UPDATE category=VALUES(category)";
                try (PreparedStatement stmt = conn.prepareStatement(insertCategorySet)) {
                    for (String category : List.of("resident", "trusted", "visitor")) {
                        stmt.setString(1, oid);
                        stmt.setString(2, category);
                        stmt.addBatch();
                    }
                    stmt.executeBatch();
                }
                String insertCategoryFlag = sqlite
                        ? "INSERT OR REPLACE INTO " + p
                                + "profile_category_flags (owner_id, category, flag) VALUES (?, ?, ?)"
                        : "INSERT INTO " + p
                                + "profile_category_flags (owner_id, category, flag) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE flag=VALUES(flag)";
                try (PreparedStatement stmt = conn.prepareStatement(insertCategoryFlag)) {
                    for (String category : List.of("resident", "trusted")) {
                        for (String flag : profile.getCategoryFlags(category)) {
                            stmt.setString(1, oid);
                            stmt.setString(2, category);
                            stmt.setString(3, flag);
                            stmt.addBatch();
                        }
                    }
                    stmt.executeBatch();
                }

                clearTable(conn, p + "profile_spawnpoints", "owner_id", oid);
                if (profile.getSpawnpointWorldName() != null) {
                    String insertSpawnpoint = sqlite
                            ? "INSERT OR REPLACE INTO " + p
                                    + "profile_spawnpoints (owner_id, world_name, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?)"
                            : "INSERT INTO " + p
                                    + "profile_spawnpoints (owner_id, world_name, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE world_name=VALUES(world_name), x=VALUES(x), y=VALUES(y), z=VALUES(z), yaw=VALUES(yaw), pitch=VALUES(pitch)";
                    try (PreparedStatement stmt = conn.prepareStatement(insertSpawnpoint)) {
                        stmt.setString(1, oid);
                        stmt.setString(2, profile.getSpawnpointWorldName());
                        stmt.setDouble(3, profile.getSpawnpointX());
                        stmt.setDouble(4, profile.getSpawnpointY());
                        stmt.setDouble(5, profile.getSpawnpointZ());
                        stmt.setFloat(6, profile.getSpawnpointYaw());
                        stmt.setFloat(7, profile.getSpawnpointPitch());
                        stmt.executeUpdate();
                    }
                }

                // 4. Clear and re-insert trusted players
                clearTable(conn, p + "profile_trusted_players", "owner_id", oid);
                if (!profile.getTrustedPlayerFlags().isEmpty()) {
                    String insertTrusted = sqlite
                            ? "INSERT OR REPLACE INTO " + p
                                    + "profile_trusted_players (owner_id, player_id, flags) VALUES (?, ?, ?)"
                            : "INSERT INTO " + p
                                    + "profile_trusted_players (owner_id, player_id, flags) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE flags=VALUES(flags)";
                    try (PreparedStatement stmt = conn.prepareStatement(insertTrusted)) {
                        for (Map.Entry<UUID, Set<String>> entry : profile.getTrustedPlayerFlags().entrySet()) {
                            stmt.setString(1, oid);
                            stmt.setString(2, entry.getKey().toString());
                            stmt.setString(3, String.join(",", entry.getValue()));
                            stmt.addBatch();
                        }
                        stmt.executeBatch();
                    }
                }

                // 6. Clear and re-insert member roles
                clearTable(conn, p + "profile_member_roles", "owner_id", oid);
                if (!profile.getMemberRoles().isEmpty()) {
                    String insertMember = sqlite
                            ? "INSERT OR REPLACE INTO " + p
                                    + "profile_member_roles (owner_id, player_id, role_name) VALUES (?, ?, ?)"
                            : "INSERT INTO " + p
                                    + "profile_member_roles (owner_id, player_id, role_name) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE role_name=VALUES(role_name)";
                    try (PreparedStatement stmt = conn.prepareStatement(insertMember)) {
                        for (Map.Entry<UUID, String> entry : profile.getMemberRoles().entrySet()) {
                            stmt.setString(1, oid);
                            stmt.setString(2, entry.getKey().toString());
                            stmt.setString(3, entry.getValue());
                            stmt.addBatch();
                        }
                        stmt.executeBatch();
                    }
                }

                // 7. Clear and re-insert banned players
                clearTable(conn, p + "profile_banned_players", "owner_id", oid);
                if (!profile.getBannedPlayers().isEmpty()) {
                    String insertBanned = sqlite
                            ? "INSERT OR REPLACE INTO " + p
                                    + "profile_banned_players (owner_id, player_id, banned_at) VALUES (?, ?, ?)"
                            : "INSERT INTO " + p
                                    + "profile_banned_players (owner_id, player_id, banned_at) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE banned_at=VALUES(banned_at)";
                    try (PreparedStatement stmt = conn.prepareStatement(insertBanned)) {
                        long now = System.currentTimeMillis();
                        for (UUID bannedId : profile.getBannedPlayers()) {
                            stmt.setString(1, oid);
                            stmt.setString(2, bannedId.toString());
                            stmt.setLong(3, now);
                            stmt.addBatch();
                        }
                        stmt.executeBatch();
                    }
                }

                conn.commit();
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to save profile for " + profile.getProfileId() + ". Rolling back transaction.");
                try {
                    if (conn != null) {
                        conn.rollback();
                        conn.setAutoCommit(true);
                    }
                } catch (SQLException rollbackEx) {
                    plugin.getLogger().severe("Failed to rollback transaction: " + rollbackEx.getMessage());
                }
                e.printStackTrace();
                throw new RuntimeException("Failed to save profile " + profile.getProfileId(), e);
            } finally {
                if (conn != null) {
                    try {
                        conn.close();
                    } catch (SQLException ignored) {}
                }
            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteProfile(UUID ownerId) {
        return CompletableFuture.runAsync(() -> {
            String p = prefix();
            String oid = ownerId.toString();

            try (Connection conn = dbManager.getDatabase().getConnection()) {
                conn.setAutoCommit(false);

                try {
                    clearTable(conn, p + "profile_ally_flags", "owner_id", oid);
                    clearTable(conn, p + "profile_banned_players", "owner_id", oid);
                    clearTable(conn, p + "profile_category_flags", "owner_id", oid);
                    clearTable(conn, p + "profile_category_flag_sets", "owner_id", oid);
                    clearTable(conn, p + "profile_member_roles", "owner_id", oid);
                    clearTable(conn, p + "profile_legacy_member_roles", "owner_id", oid);
                    clearTable(conn, p + "profile_spawnpoints", "owner_id", oid);
                    clearTable(conn, p + "profile_roles", "owner_id", oid);
                    clearTable(conn, p + "profile_trusted_players", "owner_id", oid);
                    clearTable(conn, p + "profile_visitor_flags", "owner_id", oid);
                    clearTable(conn, p + "claimed_chunks", "owner_id", oid);

                    try (PreparedStatement stmt = conn
                            .prepareStatement("DELETE FROM " + p + "claim_profiles WHERE owner_id = ?")) {
                        stmt.setString(1, oid);
                        stmt.executeUpdate();
                    }

                    conn.commit();
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    conn.rollback();
                    conn.setAutoCommit(true);
                    throw e;
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to delete profile for " + ownerId);
                e.printStackTrace();
                throw new RuntimeException("Failed to delete profile " + ownerId, e);
            }
        });
    }

    @Override
    public CompletableFuture<ClaimProfile> getProfile(UUID ownerId) {
        return CompletableFuture.supplyAsync(() -> {
            String p = prefix();
            try (Connection conn = dbManager.getDatabase().getConnection()) {
                String name;
                String claimColor;
                String visMode;
                boolean titleEnabled;
                String enterTitle;
                String leaveTitle;
                String enterTitleMode;
                String leaveTitleMode;
                String ownerAlias;
                boolean pvpEnabled;
                long pvpTimerEnd;
                String realOwnerIdStr;
                int bonusRoleSlots;
                int bonusMemberSlots;
                int bonusWarpSlots;
                try (PreparedStatement stmt = conn
                        .prepareStatement(
                                "SELECT name, claim_color, vis_mode, title_enabled, enter_title, leave_title, enter_title_mode, leave_title_mode, owner_alias, pvp_enabled, pvp_timer_end, real_owner_id, bonus_role_slots, bonus_member_slots, bonus_warp_slots FROM " + p
                                        + "claim_profiles WHERE owner_id = ?")) {
                    stmt.setString(1, ownerId.toString());
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next())
                            return null;
                        name = rs.getString("name");
                        claimColor = rs.getString("claim_color");
                        visMode = rs.getString("vis_mode");
                        titleEnabled = rs.getBoolean("title_enabled");
                        enterTitle = rs.getString("enter_title");
                        leaveTitle = rs.getString("leave_title");
                        enterTitleMode = rs.getString("enter_title_mode");
                        leaveTitleMode = rs.getString("leave_title_mode");
                        ownerAlias = rs.getString("owner_alias");
                        pvpEnabled = rs.getBoolean("pvp_enabled");
                        pvpTimerEnd = rs.getLong("pvp_timer_end");
                        realOwnerIdStr = rs.getString("real_owner_id");
                        bonusRoleSlots = rs.getInt("bonus_role_slots");
                        bonusMemberSlots = rs.getInt("bonus_member_slots");
                        bonusWarpSlots = rs.getInt("bonus_warp_slots");
                    }
                }

                UUID realOwnerId = (realOwnerIdStr != null && !realOwnerIdStr.isEmpty()) ? UUID.fromString(realOwnerIdStr) : ownerId;
                ClaimProfile profile = new ClaimProfile(ownerId, realOwnerId, name);
                profile.setClaimColor(claimColor);
                profile.setVisualizationMode(visMode != null ? visMode : "DISPLAY_ENTITY");
                profile.setEnterTitleEnabled(titleEnabled);
                if (enterTitle != null)
                    profile.setEnterTitle(enterTitle);
                if (leaveTitle != null)
                    profile.setLeaveTitle(leaveTitle);
                if (enterTitleMode != null)
                    profile.setEnterTitleMode(enterTitleMode);
                if (leaveTitleMode != null)
                    profile.setLeaveTitleMode(leaveTitleMode);
                if (ownerAlias != null)
                    profile.setOwnerAlias(ownerAlias);
                profile.setPvpEnabled(pvpEnabled);
                profile.setPvpTimerEnd(pvpTimerEnd);
                profile.setBonusRoleSlots(bonusRoleSlots);
                profile.setBonusMemberSlots(bonusMemberSlots);
                profile.setBonusWarpSlots(bonusWarpSlots);
                loadChunks(conn, p, profile);
                loadVisitorFlags(conn, p, profile);
                loadCategoryFlags(conn, p, profile);
                loadSpawnpoint(conn, p, profile);
                loadTrustedPlayers(conn, p, profile);
                loadMemberRoles(conn, p, profile);
                loadBannedPlayers(conn, p, profile);

                return profile;
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to load profile for " + ownerId);
                e.printStackTrace();
            }
            return null;
        });
    }

    @Override
    public CompletableFuture<List<ClaimProfile>> getAllProfiles() {
        return CompletableFuture.supplyAsync(() -> {
            List<ClaimProfile> profiles = new ArrayList<>();
            String p = prefix();
            try (Connection conn = dbManager.getDatabase().getConnection();
                    PreparedStatement stmt = conn.prepareStatement("SELECT * FROM " + p + "claim_profiles");
                    ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    UUID profileId = UUID.fromString(rs.getString("owner_id"));
                    String realOwnerIdStr = rs.getString("real_owner_id");
                    UUID realOwnerId = (realOwnerIdStr != null && !realOwnerIdStr.isEmpty()) ? UUID.fromString(realOwnerIdStr) : profileId;
                    String name = rs.getString("name");
                    String claimColor = rs.getString("claim_color");
                    String visMode = rs.getString("vis_mode");
                    boolean titleEnabled = rs.getBoolean("title_enabled");
                    String enterTitle = rs.getString("enter_title");
                    String leaveTitle = rs.getString("leave_title");
                    String enterTitleMode = rs.getString("enter_title_mode");
                    String leaveTitleMode = rs.getString("leave_title_mode");
                    String ownerAlias = rs.getString("owner_alias");
                    ClaimProfile profile = new ClaimProfile(profileId, realOwnerId, name);
                    profile.setClaimColor(claimColor);
                    profile.setVisualizationMode(visMode != null ? visMode : "DISPLAY_ENTITY");
                    profile.setEnterTitleEnabled(titleEnabled);
                    if (enterTitle != null)
                        profile.setEnterTitle(enterTitle);
                    if (leaveTitle != null)
                        profile.setLeaveTitle(leaveTitle);
                    if (enterTitleMode != null)
                        profile.setEnterTitleMode(enterTitleMode);
                    if (leaveTitleMode != null)
                        profile.setLeaveTitleMode(leaveTitleMode);
                    if (ownerAlias != null)
                        profile.setOwnerAlias(ownerAlias);
                    profile.setBonusRoleSlots(rs.getInt("bonus_role_slots"));
                    profile.setBonusMemberSlots(rs.getInt("bonus_member_slots"));
                    profile.setBonusWarpSlots(rs.getInt("bonus_warp_slots"));
                    loadChunks(conn, p, profile);
                    loadVisitorFlags(conn, p, profile);
                    loadCategoryFlags(conn, p, profile);
                    loadSpawnpoint(conn, p, profile);
                    loadTrustedPlayers(conn, p, profile);
                    loadMemberRoles(conn, p, profile);
                    loadBannedPlayers(conn, p, profile);

                    profiles.add(profile);
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to load all profiles.");
                e.printStackTrace();
            }
            return profiles;
        });
    }

    @Override
    public CompletableFuture<UUID> getProfileOwnerByMember(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            String p = prefix();
            try (Connection conn = dbManager.getDatabase().getConnection();
                    PreparedStatement stmt = conn.prepareStatement(
                            "SELECT owner_id FROM " + p + "profile_member_roles WHERE player_id = ?")) {
                stmt.setString(1, playerId.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next())
                        return UUID.fromString(rs.getString("owner_id"));
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to lookup member profile for " + playerId);
                e.printStackTrace();
            }
            return null;
        });
    }

    @Override
    public CompletableFuture<UUID> getProfileOwnerByTrusted(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            String p = prefix();
            try (Connection conn = dbManager.getDatabase().getConnection();
                    PreparedStatement stmt = conn.prepareStatement(
                            "SELECT owner_id FROM " + p + "profile_trusted_players WHERE player_id = ?")) {
                stmt.setString(1, playerId.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next())
                        return UUID.fromString(rs.getString("owner_id"));
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to lookup trusted profile for " + playerId);
                e.printStackTrace();
            }
            return null;
        });
    }

    // --- Private loaders ---

    private void loadChunks(Connection conn, String p, ClaimProfile profile) throws SQLException {
        try (PreparedStatement stmt = conn
                .prepareStatement("SELECT chunk_id FROM " + p + "claimed_chunks WHERE owner_id = ?")) {
            stmt.setString(1, profile.getProfileId().toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String[] parts = rs.getString("chunk_id").split(":");
                    if (parts.length == 3) {
                        profile.addChunk(new ChunkPosition(parts[0], Integer.parseInt(parts[1]),
                                Integer.parseInt(parts[2])));
                    }
                }
            }
        }
    }

    private void loadVisitorFlags(Connection conn, String p, ClaimProfile profile) throws SQLException {
        Set<String> flags = new HashSet<>();
        try (PreparedStatement stmt = conn
                .prepareStatement("SELECT flag FROM " + p + "profile_visitor_flags WHERE owner_id = ?")) {
            stmt.setString(1, profile.getProfileId().toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    flags.add(rs.getString("flag").toUpperCase());
                }
            }
        }
        if (!flags.isEmpty() || hasCategoryFlagSet(conn, p, profile.getProfileId(), "visitor")) {
            profile.setCategoryFlags("visitor", flags);
        }
    }

    private void loadCategoryFlags(Connection conn, String p, ClaimProfile profile) throws SQLException {
        Set<String> initialized = new HashSet<>();
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT category FROM " + p + "profile_category_flag_sets WHERE owner_id = ?")) {
            stmt.setString(1, profile.getProfileId().toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    initialized.add(rs.getString("category").toLowerCase());
                }
            }
        }
        Set<String> cleared = new HashSet<>();
        for (String category : List.of("resident", "trusted")) {
            if (initialized.contains(category)) {
                profile.setCategoryFlags(category, Set.of());
                cleared.add(category);
            }
        }
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT category, flag FROM " + p + "profile_category_flags WHERE owner_id = ?")) {
            stmt.setString(1, profile.getProfileId().toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String category = rs.getString("category").toLowerCase();
                    if (!initialized.contains(category) && cleared.add(category)) {
                        profile.setCategoryFlags(category, Set.of());
                    }
                    profile.getCategoryFlags(category).add(rs.getString("flag").toUpperCase());
                }
            }
        }
    }

    private boolean hasCategoryFlagSet(Connection conn, String p, UUID profileId, String category)
            throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT 1 FROM " + p + "profile_category_flag_sets WHERE owner_id = ? AND category = ?")) {
            stmt.setString(1, profileId.toString());
            stmt.setString(2, category);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void loadSpawnpoint(Connection conn, String p, ClaimProfile profile) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT world_name, x, y, z, yaw, pitch FROM " + p + "profile_spawnpoints WHERE owner_id = ?")) {
            stmt.setString(1, profile.getProfileId().toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    profile.setSpawnpointData(rs.getString("world_name"), rs.getDouble("x"),
                            rs.getDouble("y"), rs.getDouble("z"), rs.getFloat("yaw"), rs.getFloat("pitch"));
                }
            }
        }
    }

    private void loadTrustedPlayers(Connection conn, String p, ClaimProfile profile) throws SQLException {
        try (PreparedStatement stmt = conn
                .prepareStatement("SELECT player_id, flags FROM " + p + "profile_trusted_players WHERE owner_id = ?")) {
            stmt.setString(1, profile.getProfileId().toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    UUID playerId = UUID.fromString(rs.getString("player_id"));
                    String flagsStr = rs.getString("flags");
                    Set<String> flags = new HashSet<>();
                    if (flagsStr != null && !flagsStr.isEmpty()) {
                        for (String flag : flagsStr.split(",")) {
                            flags.add(flag.trim().toUpperCase());
                        }
                    }
                    profile.setTrustedFlags(playerId, flags);
                }
            }
        }
    }

    private void loadMemberRoles(Connection conn, String p, ClaimProfile profile) throws SQLException {
        try (PreparedStatement stmt = conn
                .prepareStatement(
                        "SELECT player_id, role_name FROM " + p + "profile_member_roles WHERE owner_id = ?")) {
            stmt.setString(1, profile.getProfileId().toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    profile.setMemberRole(UUID.fromString(rs.getString("player_id")), rs.getString("role_name"));
                }
            }
        }
    }

    private void clearTable(Connection conn, String table, String column, String value) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM " + table + " WHERE " + column + " = ?")) {
            stmt.setString(1, value);
            stmt.executeUpdate();
        }
    }

    private void loadBannedPlayers(Connection conn, String p, ClaimProfile profile) throws SQLException {
        try (PreparedStatement stmt = conn
                .prepareStatement("SELECT player_id FROM " + p + "profile_banned_players WHERE owner_id = ?")) {
            stmt.setString(1, profile.getProfileId().toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    profile.addBannedPlayer(UUID.fromString(rs.getString("player_id")));
                }
            }
        }
    }
}
