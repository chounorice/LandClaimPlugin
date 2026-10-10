package org.ayosynk.landClaimPlugin.managers;

import org.ayosynk.landClaimPlugin.LandClaimPlugin;
import org.ayosynk.landClaimPlugin.models.Warp;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/** Reads legacy warp records only for the one-time profile spawnpoint migration. */
final class WarpManager {

    private final LandClaimPlugin plugin;
    private final Map<UUID, Map<String, Warp>> legacyWarps = new ConcurrentHashMap<>();

    WarpManager(LandClaimPlugin plugin) {
        this.plugin = plugin;
    }

    CompletableFuture<Void> loadFromDatabase() {
        return plugin.getDatabaseManager().getWarpDao().loadAllWarps().thenAccept(warps -> {
            legacyWarps.clear();
            legacyWarps.putAll(warps);
        });
    }

    Map<String, Warp> getWarps(UUID profileId) {
        return legacyWarps.getOrDefault(profileId, Collections.emptyMap());
    }
}
