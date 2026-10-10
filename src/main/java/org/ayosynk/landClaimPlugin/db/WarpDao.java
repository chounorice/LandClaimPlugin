package org.ayosynk.landClaimPlugin.db;

import org.ayosynk.landClaimPlugin.models.Warp;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Data access object for {@link Warp} persistence.
 * Warps are stored per-profile owner with case-insensitive names.
 */
public interface WarpDao {

    /** Create or migrate warp tables. */
    void createTables();

    /**
     * Load all warps for all players.
     *
     * @return future yielding owner UUID → (warp name → Warp) mapping
     */
    CompletableFuture<Map<UUID, Map<String, Warp>>> loadAllWarps();

}
