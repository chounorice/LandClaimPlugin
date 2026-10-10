package org.ayosynk.landClaimPlugin.models;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;

/**
 * A single global claim profile per player.
 * All claimed land, permissions, membership categories, and visitor flags
 * are attached to this profile. A player may own at most one ClaimProfile.
 */
public class ClaimProfile {
    // Use UUID based on namespace to guarantee no collision with real player UUIDs
    public static final UUID ADMIN_PROFILE_ID = UUID.nameUUIDFromBytes("landclaim.admin.profile".getBytes(java.nio.charset.StandardCharsets.UTF_8));

    private final UUID profileId; // The unique ID of the profile (maps to owner_id in DB for backwards compatibility)
    private UUID realOwnerId; // The actual player UUID who owns the profile
    private String name;
    private String ownerAlias;

    private final Set<ChunkPosition> ownedChunks = new HashSet<>();
    private final Set<String> visitorFlags = new HashSet<>();
    private final Map<UUID, Set<String>> trustedPlayerFlags = new HashMap<>();
    private final Map<UUID, String> memberRoles = new HashMap<>();
    private final Map<String, Set<String>> categoryFlags = new HashMap<>();
    private final Set<UUID> bannedPlayers = new HashSet<>();
    private String spawnpointWorld;
    private double spawnpointX;
    private double spawnpointY;
    private double spawnpointZ;
    private float spawnpointYaw;
    private float spawnpointPitch;
    private String claimColor; // Hex color string, e.g. "#00FF00", nullable (falls back to default)
    private String visualizationMode = "DISPLAY_ENTITY"; // "DISPLAY_ENTITY" or "PARTICLE"

    // Title settings
    private boolean enterTitleEnabled = false;
    private String enterTitle = "<gold>Entering <owner>'s Claim";
    private String enterTitleMode = "TITLE"; // "TITLE" or "SUBTITLE"
    private String leaveTitle = "<yellow>Leaving <owner>'s Claim";
    private String leaveTitleMode = "SUBTITLE"; // "TITLE" or "SUBTITLE"

    // PvP settings
    private boolean pvpEnabled = false;
    private long pvpTimerEnd = 0L;

    // Bonus slots from purchasing
    private int bonusRoleSlots = 0;
    private int bonusMemberSlots = 0;
    private int bonusWarpSlots = 0;

    public ClaimProfile(UUID profileId, UUID realOwnerId, String name) {
        this.profileId = profileId;
        this.realOwnerId = realOwnerId;
        this.name = name;
        setupDefaultVisitorFlags();
        setupDefaultCategoryFlags();
    }

    public ClaimProfile(UUID ownerId, String name) {
        this.profileId = ownerId;
        this.realOwnerId = ownerId;
        this.name = name;
        setupDefaultVisitorFlags();
        setupDefaultCategoryFlags();
    }

    private void setupDefaultVisitorFlags() {
        org.ayosynk.landClaimPlugin.LandClaimPlugin plugin = org.ayosynk.landClaimPlugin.LandClaimPlugin.getInstance();
        if (plugin != null && plugin.getConfigManager() != null) {
            this.visitorFlags.addAll(plugin.getConfigManager().getDefaultVisitorFlags());
        } else {
            this.visitorFlags.add("DAMAGE_MONSTERS");
        }
    }

    private void setupDefaultCategoryFlags() {
        this.categoryFlags.put("visitor", new HashSet<>(visitorFlags));
        this.categoryFlags.put("resident", new HashSet<>(Set.of(
                "USE_DOORS", "USE_TRAPDOORS", "USE_FENCE_GATES", "USE_CONTAINERS",
                "USE_WORKSTATIONS", "USE_BEDS", "USE_REDSTONE", "DAMAGE_MONSTERS",
                "DAMAGE_ANIMALS")));
        this.categoryFlags.put("trusted", new HashSet<>(Set.of(
                "USE_DOORS", "USE_TRAPDOORS", "USE_FENCE_GATES", "USE_CONTAINERS",
                "USE_WORKSTATIONS", "USE_BEDS", "USE_REDSTONE")));
    }

    // --- Owner ---
    
    public boolean canManage(org.bukkit.entity.Player player) {
        if (player.hasPermission("landclaim.admin")) return true;
        return isOwner(player.getUniqueId());
    }

    public UUID getProfileId() {
        return profileId;
    }

    public UUID getOwnerId() {
        return realOwnerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.realOwnerId = ownerId;
    }

    public boolean isOwner(UUID playerId) {
        return realOwnerId.equals(playerId);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwnerAlias() {
        return ownerAlias;
    }

    public void setOwnerAlias(String ownerAlias) {
        this.ownerAlias = ownerAlias;
    }

    public String getDisplayOwnerName() {
        if (realOwnerId.equals(ADMIN_PROFILE_ID)) {
            return "Admin";
        }
        if (ownerAlias != null && !ownerAlias.isEmpty()) {
            return ownerAlias;
        }
        org.bukkit.OfflinePlayer op = org.bukkit.Bukkit.getOfflinePlayer(realOwnerId);
        return op.getName() != null ? op.getName() : realOwnerId.toString();
    }

    // --- PvP Settings ---

    public boolean isPvpEnabled() {
        return pvpEnabled;
    }

    public void setPvpEnabled(boolean pvpEnabled) {
        this.pvpEnabled = pvpEnabled;
    }

    public long getPvpTimerEnd() {
        return pvpTimerEnd;
    }

    public void setPvpTimerEnd(long pvpTimerEnd) {
        this.pvpTimerEnd = pvpTimerEnd;
    }

    // --- Chunks ---

    public Set<ChunkPosition> getOwnedChunks() {
        return ownedChunks;
    }

    public void addChunk(ChunkPosition pos) {
        ownedChunks.add(pos);
    }

    public void removeChunk(ChunkPosition pos) {
        ownedChunks.remove(pos);
    }

    public boolean ownsChunk(ChunkPosition pos) {
        return ownedChunks.contains(pos);
    }

    // --- Visitor Flags (base permission layer) ---

    public Set<String> getVisitorFlags() {
        return visitorFlags;
    }

    public boolean hasVisitorFlag(String flag) {
        return visitorFlags.contains(flag.toUpperCase());
    }

    public void addVisitorFlag(String flag) {
        visitorFlags.add(flag.toUpperCase());
    }

    public void removeVisitorFlag(String flag) {
        visitorFlags.remove(flag.toUpperCase());
    }

    // --- Trusted Players (per-player permission overrides) ---

    public Map<UUID, Set<String>> getTrustedPlayerFlags() {
        return trustedPlayerFlags;
    }

    public boolean isTrusted(UUID playerId) {
        return trustedPlayerFlags.containsKey(playerId);
    }

    public Set<String> getTrustedFlags(UUID playerId) {
        return trustedPlayerFlags.get(playerId);
    }

    public void setTrustedFlags(UUID playerId, Set<String> flags) {
        trustedPlayerFlags.put(playerId, flags);
    }

    public void addTrustedPlayer(UUID playerId) {
        trustedPlayerFlags.putIfAbsent(playerId, new HashSet<>());
    }

    public void removeTrustedPlayer(UUID playerId) {
        trustedPlayerFlags.remove(playerId);
    }

    // --- Member Roles (player → role assignment) ---

    public Map<UUID, String> getMemberRoles() {
        return memberRoles;
    }

    public boolean isMember(UUID playerId) {
        return memberRoles.containsKey(playerId);
    }

    public String getMemberRole(UUID playerId) {
        return memberRoles.containsKey(playerId) ? "Resident" : null;
    }

    public void setMemberRole(UUID playerId, String roleName) {
        if (roleName == null) {
            memberRoles.remove(playerId);
        } else {
            memberRoles.put(playerId, "Resident");
        }
    }

    public void removeMember(UUID playerId) {
        memberRoles.remove(playerId);
    }

    public Set<String> getCategoryFlags(String category) {
        String key = normalizeCategory(category);
        if (key.equals("visitor")) return visitorFlags;
        return categoryFlags.computeIfAbsent(key, ignored -> new HashSet<>());
    }

    public void setCategoryFlags(String category, Set<String> flags) {
        String key = normalizeCategory(category);
        Set<String> normalized = new HashSet<>();
        if (flags != null) {
            for (String flag : flags) normalized.add(flag.toUpperCase());
        }
        if (key.equals("visitor")) {
            visitorFlags.clear();
            visitorFlags.addAll(normalized);
        } else {
            categoryFlags.put(key, normalized);
        }
    }

    public boolean hasCategoryFlag(String category, String flag) {
        return getCategoryFlags(category).contains(flag.toUpperCase());
    }

    private String normalizeCategory(String category) {
        if (category == null) throw new IllegalArgumentException("Role category cannot be null");
        String normalized = category.toLowerCase();
        if (!normalized.equals("resident") && !normalized.equals("trusted") && !normalized.equals("visitor")) {
            throw new IllegalArgumentException("Unknown role category: " + category);
        }
        return normalized;
    }

    public Location getSpawnpoint() {
        if (spawnpointWorld == null) return null;
        org.bukkit.World world = Bukkit.getWorld(spawnpointWorld);
        return world == null ? null : new Location(world, spawnpointX, spawnpointY, spawnpointZ, spawnpointYaw, spawnpointPitch);
    }

    public void setSpawnpoint(Location spawnpoint) {
        if (spawnpoint == null || spawnpoint.getWorld() == null) {
            spawnpointWorld = null;
            return;
        }
        this.spawnpointWorld = spawnpoint.getWorld().getName();
        this.spawnpointX = spawnpoint.getX();
        this.spawnpointY = spawnpoint.getY();
        this.spawnpointZ = spawnpoint.getZ();
        this.spawnpointYaw = spawnpoint.getYaw();
        this.spawnpointPitch = spawnpoint.getPitch();
    }

    public String getSpawnpointWorldName() {
        return spawnpointWorld;
    }

    public void setSpawnpointData(String world, double x, double y, double z, float yaw, float pitch) {
        this.spawnpointWorld = world;
        this.spawnpointX = x;
        this.spawnpointY = y;
        this.spawnpointZ = z;
        this.spawnpointYaw = yaw;
        this.spawnpointPitch = pitch;
    }

    public double getSpawnpointX() { return spawnpointX; }
    public double getSpawnpointY() { return spawnpointY; }
    public double getSpawnpointZ() { return spawnpointZ; }
    public float getSpawnpointYaw() { return spawnpointYaw; }
    public float getSpawnpointPitch() { return spawnpointPitch; }

    // --- Banned Players (hard entry denial) ---

    /**
     * Banned players are denied every flag and physically prevented from entering any
     * chunk owned by this profile. Unlike removing trust or membership, a ban persists
     * until the owner explicitly unbans the player — even if the player re-invites
     * themselves through a different path.
     */
    public Set<UUID> getBannedPlayers() {
        return bannedPlayers;
    }

    public boolean isBanned(UUID playerId) {
        return playerId != null && bannedPlayers.contains(playerId);
    }

    public void addBannedPlayer(UUID playerId) {
        if (playerId == null) return;
        bannedPlayers.add(playerId);
    }

    public void removeBannedPlayer(UUID playerId) {
        if (playerId == null) return;
        bannedPlayers.remove(playerId);
    }

    // --- Claim Color ---

    public String getColoredName() {
        if (claimColor == null || claimColor.isEmpty()) {
            return name;
        }
        return "<" + claimColor + ">" + name + "</" + claimColor + ">";
    }

    public String getColoredOwnerName() {
        String ownerName = getDisplayOwnerName();
        if (claimColor == null || claimColor.isEmpty()) {
            return ownerName;
        }
        return "<" + claimColor + ">" + ownerName + "</" + claimColor + ">";
    }

    public String getLegacyColoredName() {
        if (claimColor == null || claimColor.isEmpty()) {
            return name;
        }
        return "&" + claimColor + name;
    }

    public String getLegacyColoredOwnerName() {
        String ownerName = getDisplayOwnerName();
        if (claimColor == null || claimColor.isEmpty()) {
            return ownerName;
        }
        return "&" + claimColor + ownerName;
    }

    public String getClaimColor() {
        return claimColor;
    }

    public void setClaimColor(String claimColor) {
        this.claimColor = claimColor;
    }

    // --- Visualization Mode ---

    public String getVisualizationMode() {
        return visualizationMode;
    }

    public void setVisualizationMode(String visualizationMode) {
        this.visualizationMode = visualizationMode;
    }

    // --- Title Settings ---

    public boolean isEnterTitleEnabled() {
        return enterTitleEnabled;
    }

    public void setEnterTitleEnabled(boolean enterTitleEnabled) {
        this.enterTitleEnabled = enterTitleEnabled;
    }

    public String getEnterTitle() {
        return enterTitle;
    }

    public void setEnterTitle(String enterTitle) {
        this.enterTitle = enterTitle;
    }

    public String getLeaveTitle() {
        return leaveTitle;
    }

    public void setLeaveTitle(String leaveTitle) {
        this.leaveTitle = leaveTitle;
    }

    public String getEnterTitleMode() {
        return enterTitleMode;
    }

    public void setEnterTitleMode(String enterTitleMode) {
        this.enterTitleMode = enterTitleMode;
    }

    public String getLeaveTitleMode() {
        return leaveTitleMode;
    }

    public void setLeaveTitleMode(String leaveTitleMode) {
        this.leaveTitleMode = leaveTitleMode;
    }

    // Getters and setters for bonus slots
    public int getBonusRoleSlots() {
        return bonusRoleSlots;
    }

    public void setBonusRoleSlots(int bonusRoleSlots) {
        this.bonusRoleSlots = bonusRoleSlots;
    }

    public int getBonusMemberSlots() {
        return bonusMemberSlots;
    }

    public void setBonusMemberSlots(int bonusMemberSlots) {
        this.bonusMemberSlots = bonusMemberSlots;
    }

    public int getBonusWarpSlots() {
        return bonusWarpSlots;
    }

    public void setBonusWarpSlots(int bonusWarpSlots) {
        this.bonusWarpSlots = bonusWarpSlots;
    }
}
