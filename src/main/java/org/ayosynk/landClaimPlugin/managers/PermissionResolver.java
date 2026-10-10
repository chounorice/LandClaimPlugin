package org.ayosynk.landClaimPlugin.managers;

import org.ayosynk.landClaimPlugin.LandClaimPlugin;
import org.ayosynk.landClaimPlugin.models.ClaimProfile;

import java.util.UUID;

/**
 * Resolves a player's effective claim flags using Owner > Resident > Trusted > Visitor.
 */
public class PermissionResolver {

    public static boolean hasPermission(ClaimProfile profile, UUID playerId, String flag) {
        if (profile == null)
            return true;
        if (playerId == null)
            return false;
        if (profile.isBanned(playerId))
            return false;
        if (profile.isOwner(playerId))
            return true;

        String normalizedFlag = flag.toUpperCase();
        if (profile.isMember(playerId))
            return profile.hasCategoryFlag("resident", normalizedFlag);

        if (profile.isTrusted(playerId)) {
            return profile.hasCategoryFlag("trusted", normalizedFlag);
        }

        if (profile.hasCategoryFlag("visitor", normalizedFlag))
            return true;

        LandClaimPlugin plugin = LandClaimPlugin.getInstance();
        return plugin != null && plugin.getConfigManager() != null
                && plugin.getConfigManager().isVisitorSettingsLocked()
                && plugin.getConfigManager().hasDefaultVisitorFlag(normalizedFlag);
    }

    public static String getPlayerStatus(ClaimProfile profile, UUID playerId) {
        if (profile == null)
            return "wilderness";
        if (profile.isBanned(playerId))
            return "banned";
        if (profile.isOwner(playerId))
            return "owner";
        if (profile.isMember(playerId))
            return "resident";
        if (profile.isTrusted(playerId))
            return "trusted";
        return "visitor";
    }
}
