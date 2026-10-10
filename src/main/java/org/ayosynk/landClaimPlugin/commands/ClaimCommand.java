package org.ayosynk.landClaimPlugin.commands;

import org.ayosynk.landClaimPlugin.util.FoliaScheduler;
import org.ayosynk.landClaimPlugin.LandClaimPlugin;
import org.ayosynk.landClaimPlugin.gui.*;
import org.ayosynk.landClaimPlugin.managers.ClaimManager;
import org.ayosynk.landClaimPlugin.managers.ConfigManager;
import org.ayosynk.landClaimPlugin.managers.VisualizationManager;
import org.ayosynk.landClaimPlugin.models.ChunkPosition;
import org.ayosynk.landClaimPlugin.models.ClaimProfile;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.paper.PaperCommandManager;
import org.incendo.cloud.paper.util.sender.PlayerSource;
import org.incendo.cloud.paper.util.sender.Source;
import org.incendo.cloud.parser.standard.IntegerParser;
import org.incendo.cloud.parser.standard.StringParser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Handles: /claim, /claim auto, /claim radius, /claim visible, /claim info, /claim menu,
 * /claim menu <subcommand>, /claim rename, /claim color, /claim visualization, /claim unclaimall
 */
public class ClaimCommand implements LandClaimCommand {

    private final LandClaimPlugin plugin;
    private final ClaimManager claimManager;
    private final ConfigManager configManager;
    private final VisualizationManager visualizationManager;

    private final Map<UUID, Boolean> autoClaimPlayers = new HashMap<>();
    private final Map<UUID, Boolean> autoUnclaimPlayers = new HashMap<>();

    public ClaimCommand(LandClaimPlugin plugin, ClaimManager claimManager,
            ConfigManager configManager, VisualizationManager visualizationManager) {
        this.plugin = plugin;
        this.claimManager = claimManager;
        this.configManager = configManager;
        this.visualizationManager = visualizationManager;
    }

    @Override
    public void register(PaperCommandManager<Source> manager, Command.Builder<PlayerSource> claimBuilder) {
        // /claim
        manager.command(claimBuilder
                .handler(context -> {
                    Player player = context.sender().source();
                    if (!org.ayosynk.landClaimPlugin.gui.GuiHelper.checkPermission(player, "landclaim.claim", plugin)) return;
                    claimCurrentChunk(player);
                }));

        // /claim radius <radius>
        manager.command(claimBuilder.literal("radius")
                .required("radius", IntegerParser.integerParser(1, 5))
                .handler(context -> {
                    Player player = context.sender().source();
                    if (!org.ayosynk.landClaimPlugin.gui.GuiHelper.checkPermission(player, "landclaim.claim", plugin)) return;
                    int radius = context.get("radius");
                    FoliaScheduler.runForPlayer(plugin, player, () -> {
                        Chunk chunk = player.getLocation().getChunk();
                        claimManager.claimRadius(player, chunk, radius);
                    });
                }));

        // /claim auto
        manager.command(claimBuilder.literal("auto")
                .handler(context -> {
                    Player player = context.sender().source();
                    if (!org.ayosynk.landClaimPlugin.gui.GuiHelper.checkPermission(player, "landclaim.auto", plugin)) return;
                    toggleAutoClaim(player);
                }));

        // /claim visible
        manager.command(claimBuilder.literal("visible")
                .handler(context -> {
                    Player player = context.sender().source();
                    if (!org.ayosynk.landClaimPlugin.gui.GuiHelper.checkPermission(player, "landclaim.visible", plugin)) return;
                    toggleVisibility(player);
                }));

        manager.command(claimBuilder.literal("notify")
                .handler(context -> {
                    Player player = context.sender().source();
                    if (!org.ayosynk.landClaimPlugin.gui.GuiHelper.checkPermission(player, "landclaim.notify", plugin)) return;
                    toggleChatNotify(player);
                }));

        manager.command(claimBuilder.literal("chatnotify")
                .handler(context -> {
                    Player player = context.sender().source();
                    if (!org.ayosynk.landClaimPlugin.gui.GuiHelper.checkPermission(player, "landclaim.notify", plugin)) return;
                    toggleChatNotify(player);
                }));

        // /claim toggle <display_entities|particles|off>
        manager.command(claimBuilder.literal("toggle")
                .required("mode", StringParser.stringParser(), VisualizationModeSuggestions.modes())
                .handler(context -> {
                    Player player = context.sender().source();
                    if (!org.ayosynk.landClaimPlugin.gui.GuiHelper.checkPermission(player, "landclaim.toggle", plugin)) return;
                    String mode = (String) context.get("mode");
                    toggleVisualizationMode(player, mode);
                }));

        // /claim create <name>
        manager.command(claimBuilder.literal("create")
                .required("name", StringParser.greedyStringParser())
                .handler(context -> {
                    Player player = context.sender().source();
                    if (!org.ayosynk.landClaimPlugin.gui.GuiHelper.checkPermission(player, "landclaim.create", plugin)) return;
                    String name = context.get("name");
                    FoliaScheduler.runForPlayer(plugin, player, () -> createProfile(player, name));
                }));

        // /claim menu — prioritize current location if admin, then player's own profile
        manager.command(claimBuilder.literal("menu")
                .handler(context -> {
                    Player player = context.sender().source();
                    if (!org.ayosynk.landClaimPlugin.gui.GuiHelper.checkPermission(player, "landclaim.menu", plugin)) return;
                    org.ayosynk.landClaimPlugin.models.ChunkPosition pos = new org.ayosynk.landClaimPlugin.models.ChunkPosition(player.getLocation());
                    ClaimProfile profileAtLoc = claimManager.getProfileAt(pos);
                    
                    // Priority 1: Current location if they have ADMIN_MENU flag or are owner
                    if (profileAtLoc != null && (profileAtLoc.isOwner(player.getUniqueId()) || 
                            org.ayosynk.landClaimPlugin.managers.PermissionResolver.hasPermission(profileAtLoc, player.getUniqueId(), "ADMIN_MENU"))) {
                        FoliaScheduler.runForPlayer(plugin, player, () -> MainMenuGUI.open(player, profileAtLoc, plugin));
                        return;
                    }

                    // Priority 2: Player's own active profile
                    ClaimProfile ownProfile = claimManager.getActiveProfile(player);
                    if (ownProfile == null) {
                        player.sendMessage(configManager.getMessage("no-profile"));
                        return;
                    }
                    FoliaScheduler.runForPlayer(plugin, player, () -> MainMenuGUI.open(player, ownProfile, plugin));
                }));

        // /claim settings — alias for /claim menu
        manager.command(claimBuilder.literal("settings")
                .handler(context -> {
                    Player player = context.sender().source();
                    if (!org.ayosynk.landClaimPlugin.gui.GuiHelper.checkPermission(player, "landclaim.menu", plugin)) return;
                    org.ayosynk.landClaimPlugin.models.ChunkPosition pos = new org.ayosynk.landClaimPlugin.models.ChunkPosition(player.getLocation());
                    ClaimProfile profileAtLoc = claimManager.getProfileAt(pos);
                    
                    if (profileAtLoc != null && (profileAtLoc.isOwner(player.getUniqueId()) || 
                            org.ayosynk.landClaimPlugin.managers.PermissionResolver.hasPermission(profileAtLoc, player.getUniqueId(), "ADMIN_MENU"))) {
                        FoliaScheduler.runForPlayer(plugin, player, () -> MainMenuGUI.open(player, profileAtLoc, plugin));
                        return;
                    }

                    ClaimProfile ownProfile = claimManager.getActiveProfile(player);
                    if (ownProfile == null) {
                        player.sendMessage(configManager.getMessage("no-profile"));
                        return;
                    }
                    FoliaScheduler.runForPlayer(plugin, player, () -> MainMenuGUI.open(player, ownProfile, plugin));
                }));

        // /claim info
        manager.command(claimBuilder.literal("info")
                .handler(context -> {
                    Player player = context.sender().source();
                    if (!org.ayosynk.landClaimPlugin.gui.GuiHelper.checkPermission(player, "landclaim.info", plugin)) return;
                    FoliaScheduler.runForPlayer(plugin, player, () -> sendClaimInfo(player));
                }));

        // /claim spawnpoint [remove]
        manager.command(claimBuilder.literal("spawnpoint")
                .handler(context -> {
                    Player player = context.sender().source();
                    if (!org.ayosynk.landClaimPlugin.gui.GuiHelper.checkPermission(
                            player, "landclaim.spawnpoint", plugin)) return;
                    FoliaScheduler.runForPlayer(plugin, player, () -> setProfileSpawnpoint(player));
                }));
        manager.command(claimBuilder.literal("spawnpoint").literal("remove")
                .handler(context -> {
                    Player player = context.sender().source();
                    if (!org.ayosynk.landClaimPlugin.gui.GuiHelper.checkPermission(
                            player, "landclaim.spawnpoint", plugin)) return;
                    FoliaScheduler.runForPlayer(plugin, player, () -> removeProfileSpawnpoint(player));
                }));

        // /claim tp "<claim-owner-name>" "<claim>"
        manager.command(claimBuilder.literal("tp")
                .required("owner", StringParser.stringParser())
                .required("claim", StringParser.stringParser())
                .handler(context -> {
                    Player player = context.sender().source();
                    if (!org.ayosynk.landClaimPlugin.gui.GuiHelper.checkPermission(
                            player, "landclaim.teleport", plugin)) return;
                    String ownerName = context.get("owner");
                    String profileName = context.get("claim");
                    FoliaScheduler.runForPlayer(plugin, player,
                            () -> teleportToProfileSpawnpoint(player, ownerName, profileName));
                }));

        // /claim profiles
        manager.command(claimBuilder.literal("profiles")
                .handler(context -> {
                    Player player = context.sender().source();
                    if (!configManager.isMultiProfilesEnabled()) {
                        player.sendMessage(configManager.getMessage("multi-profiles-disabled"));
                        return;
                    }
                    FoliaScheduler.runForPlayer(plugin, player, () -> org.ayosynk.landClaimPlugin.gui.ProfileSelectorGUI.open(player, plugin));
                }));

        // /claim reload (Console + Player)
        manager.command(manager.commandBuilder("claim", "c", "landclaim").literal("reload")
                .permission("landclaim.admin")
                .handler(context -> {
                    org.bukkit.command.CommandSender sender = context.sender().source();
                    plugin.reloadPlugin();
                    sender.sendMessage(configManager.getMessage("reloaded"));
                }));

        // /claim pvp <on|off> [time_seconds]
        manager.command(claimBuilder.literal("pvp")
                .required("state", org.incendo.cloud.parser.standard.StringParser.stringParser(), (context, input) -> java.util.concurrent.CompletableFuture.completedFuture(java.util.Arrays.asList(org.incendo.cloud.suggestion.Suggestion.suggestion("on"), org.incendo.cloud.suggestion.Suggestion.suggestion("off"))))
                .optional("time", org.incendo.cloud.parser.standard.IntegerParser.integerParser(1))
                .handler(context -> {
                    Player player = context.sender().source();
                    if (!org.ayosynk.landClaimPlugin.gui.GuiHelper.checkPermission(player, "landclaim.pvp", plugin)) return;
                    // Refuse the toggle if PvP is force-enabled server-wide.
                    if (configManager.getPluginConfig().pvp.forceEnabled) {
                        player.sendMessage(configManager.getMessage("pvp-force-locked"));
                        return;
                    }
                    String state = context.get("state");
                    Integer time = context.getOrDefault("time", null);
                    togglePvp(player, state.equalsIgnoreCase("on"), time);
                }));

        // ========== /claim menu <subcommand> shortcuts ==========

        // /claim menu settings
        manager.command(claimBuilder.literal("menu").literal("settings")
                .handler(context -> {
                    Player player = context.sender().source();
                    ClaimProfile profile = resolveProfileForMenu(player);
                    if (profile == null) return;
                    if (!checkMenuPermission(player, profile, "MANAGE_SETTINGS")) return;
                    FoliaScheduler.runForPlayer(plugin, player, () -> ClaimSettingsGUI.open(player, profile, plugin));
                }));

        // /claim menu manage
        manager.command(claimBuilder.literal("menu").literal("manage")
                .handler(context -> {
                    Player player = context.sender().source();
                    ClaimProfile profile = resolveProfileForMenu(player);
                    if (profile == null) return;
                    if (!checkMenuPermission(player, profile, "MANAGE_MEMBERS")) return;
                    FoliaScheduler.runForPlayer(plugin, player, () -> ManageGUI.open(player, profile, plugin));
                }));

        // /claim menu flags
        manager.command(claimBuilder.literal("menu").literal("flags")
                .handler(context -> {
                    Player player = context.sender().source();
                    ClaimProfile profile = resolveProfileForMenu(player);
                    if (profile == null) return;
                    if (!checkMenuPermission(player, profile, "MANAGE_SETTINGS")) return;
                    FoliaScheduler.runForPlayer(plugin, player,
                            () -> VisitorSettingsGUI.open(player, profile, plugin, "visitor"));
                }));

        // ========== /claim rename <name> ==========
        manager.command(claimBuilder.literal("rename")
                .required("name", StringParser.greedyStringParser())
                .handler(context -> {
                    Player player = context.sender().source();
                    if (!org.ayosynk.landClaimPlugin.gui.GuiHelper.checkPermission(player, "landclaim.rename", plugin)) return;
                    String name = context.get("name");
                    FoliaScheduler.runForPlayer(plugin, player, () -> renameClaim(player, name));
                }));

        // ========== /claim color <color> ==========
        manager.command(claimBuilder.literal("color")
                .required("color", StringParser.greedyStringParser(),
                        (ctx, input) -> java.util.concurrent.CompletableFuture.completedFuture(
                                java.util.Arrays.asList(
                                        org.incendo.cloud.suggestion.Suggestion.suggestion("red"),
                                        org.incendo.cloud.suggestion.Suggestion.suggestion("blue"),
                                        org.incendo.cloud.suggestion.Suggestion.suggestion("green"),
                                        org.incendo.cloud.suggestion.Suggestion.suggestion("yellow"),
                                        org.incendo.cloud.suggestion.Suggestion.suggestion("orange"),
                                        org.incendo.cloud.suggestion.Suggestion.suggestion("purple"),
                                        org.incendo.cloud.suggestion.Suggestion.suggestion("cyan"),
                                        org.incendo.cloud.suggestion.Suggestion.suggestion("pink"),
                                        org.incendo.cloud.suggestion.Suggestion.suggestion("lime"),
                                        org.incendo.cloud.suggestion.Suggestion.suggestion("white"),
                                        org.incendo.cloud.suggestion.Suggestion.suggestion("black"),
                                        org.incendo.cloud.suggestion.Suggestion.suggestion("gray"),
                                        org.incendo.cloud.suggestion.Suggestion.suggestion("brown"),
                                        org.incendo.cloud.suggestion.Suggestion.suggestion("magenta"),
                                        org.incendo.cloud.suggestion.Suggestion.suggestion("light_blue"),
                                        org.incendo.cloud.suggestion.Suggestion.suggestion("light_gray"),
                                        org.incendo.cloud.suggestion.Suggestion.suggestion("#")
                                )))
                .handler(context -> {
                    Player player = context.sender().source();
                    if (!org.ayosynk.landClaimPlugin.gui.GuiHelper.checkPermission(player, "landclaim.color", plugin)) return;
                    String color = context.get("color");
                    FoliaScheduler.runForPlayer(plugin, player, () -> changeClaimColor(player, color));
                }));

        // ========== /claim visualization <mode> — alias for /claim toggle ==========
        manager.command(claimBuilder.literal("visualization")
                .required("mode", StringParser.stringParser(), VisualizationModeSuggestions.modes())
                .handler(context -> {
                    Player player = context.sender().source();
                    String mode = (String) context.get("mode");
                    toggleVisualizationMode(player, mode);
                }));

        // ========== /claim leave <claim name> ==========
        manager.command(claimBuilder.literal("leave")
                .required("name", StringParser.greedyStringParser(), ClaimLeaveSuggestions.get(plugin))
                .handler(context -> {
                    Player player = context.sender().source();
                    if (!org.ayosynk.landClaimPlugin.gui.GuiHelper.checkPermission(player, "landclaim.leave", plugin)) return;
                    String name = context.get("name");
                    FoliaScheduler.runForPlayer(plugin, player, () -> leaveClaim(player, name));
                }));
    }

    // --- State Management ---

    public boolean isAutoClaimEnabled(UUID playerId) {
        return autoClaimPlayers.getOrDefault(playerId, false);
    }

    public boolean isAutoUnclaimEnabled(UUID playerId) {
        return autoUnclaimPlayers.getOrDefault(playerId, false);
    }

    public void cleanupPlayer(UUID playerId) {
        autoClaimPlayers.remove(playerId);
        autoUnclaimPlayers.remove(playerId);
    }

    private void toggleChatNotify(Player player) {
        var listenerManager = plugin.getListenerManager();
        if (listenerManager == null) return;
        var eventListener = listenerManager.getEventListener();
        if (eventListener == null) return;
        UUID playerId = player.getUniqueId();
        boolean enabled = !eventListener.isChatNotifyEnabled(playerId);
        eventListener.setChatNotifyEnabled(playerId, enabled);
        if (enabled) {
            player.sendMessage(configManager.getMessage("claim-notify-enabled"));
        } else {
            player.sendMessage(configManager.getMessage("claim-notify-disabled"));
        }
    }

    // --- Private Helpers ---

    private void claimCurrentChunk(Player player) {
        FoliaScheduler.runForPlayer(plugin, player, () -> {
            Chunk chunk = player.getLocation().getChunk();
            if (claimManager.claimChunk(player, chunk)) {
                ClaimProfile profile = claimManager.getActiveProfile(player);
                int count = profile != null ? profile.getOwnedChunks().size() : 1;
                int limit = claimManager.getClaimLimit(player);
                player.sendMessage(configManager.getMessage("chunk-claimed",
                        "<chunks>", String.valueOf(count),
                        "<limit>", String.valueOf(limit)));
                plugin.getVisualizationManager().showTemporary(player);
                // Update action bar cache so EventListener sends correct message
                plugin.getListenerManager().getEventListener().updatePlayerClaimCache(player);
                // Also send immediate action bar update
                updateActionBarInstant(player);
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
            }
        });
    }

    private void updateActionBarInstant(Player player) {
        if (!configManager.isActionBarEnabled()) {
            return;
        }
        org.ayosynk.landClaimPlugin.models.ChunkPosition pos = new org.ayosynk.landClaimPlugin.models.ChunkPosition(player.getLocation().getChunk());
        org.ayosynk.landClaimPlugin.models.ClaimProfile profile = claimManager.getProfileAt(pos);

        String message;
        UUID playerId = player.getUniqueId();

        if (profile != null) {
            UUID ownerId = claimManager.getChunkOwner(pos);
            String ownerName = profile.getColoredOwnerName();
            String claimName = profile.getColoredName();

            if (playerId.equals(ownerId)) {
                message = configManager.getActionBarMessage("actionbar-owned-by-you")
                        .replace("<claim>", claimName)
                        .replace("<name>", claimName);
            } else {
                String status = org.ayosynk.landClaimPlugin.managers.PermissionResolver.getPlayerStatus(profile, playerId);
                if (status.equals("resident") || status.equals("trusted")) {
                    message = configManager.getActionBarMessage("actionbar-trusted")
                            .replace("<owner>", ownerName)
                            .replace("<claim>", claimName)
                            .replace("<name>", claimName);
                } else if (player.hasPermission("landclaim.admin")) {
                    message = configManager.getActionBarMessage("actionbar-admin")
                            .replace("<owner>", ownerName)
                            .replace("<claim>", claimName)
                            .replace("<name>", claimName);
                } else {
                    message = configManager.getActionBarMessage("actionbar-owned-by-other")
                            .replace("<owner>", ownerName)
                            .replace("<claim>", claimName)
                            .replace("<name>", claimName);
                }
            }
        } else {
            message = configManager.getActionBarMessage("actionbar-wilderness");
        }

        player.sendActionBar(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(message));
    }

    private void createProfile(Player player, String name) {
        UUID playerId = player.getUniqueId();

        if (!claimManager.canCreateProfile(playerId)) {
            player.sendMessage(configManager.getMessage("cannot-claim-as-member"));
            return;
        }

        if (!configManager.isMultiProfilesEnabled()) {
            ClaimProfile existing = claimManager.getProfile(playerId);
            if (existing != null) {
                player.sendMessage(configManager.getMessage("already-has-profile"));
                return;
            }
        } else {
            java.util.List<ClaimProfile> owned = claimManager.getOwnedProfiles(playerId);
            if (owned.size() >= configManager.getMaxProfilesPerPlayer() && !player.hasPermission("landclaim.admin")) {
                player.sendMessage(configManager.getMessage("profile-limit-reached", "<limit>", String.valueOf(configManager.getMaxProfilesPerPlayer())));
                return;
            }
        }

        if (configManager.isBannedClaimName(name)) {
            player.sendMessage(configManager.getMessage("banned-claim-name"));
            return;
        }

        if (!claimManager.isClaimNameUnique(name)) {
            player.sendMessage(configManager.getMessage("name-already-in-use"));
            return;
        }

        UUID profileId = configManager.isMultiProfilesEnabled() ? UUID.randomUUID() : playerId;
        ClaimProfile profile = new ClaimProfile(profileId, playerId, name);
        plugin.getCacheManager().getProfileCache().put(profileId, profile);
        claimManager.saveAndSync(profile);

        // Set as active
        org.ayosynk.landClaimPlugin.models.ClaimPlayer cp = plugin.getCacheManager().getPlayerCache().getIfPresent(playerId);
        if (cp != null) {
            cp.setActiveProfileId(profileId);
            plugin.getDatabaseManager().getPlayerDao().savePlayer(cp);
        }

        player.sendMessage(configManager.getMessage("profile-created", "<name>", name));
    }

    public void toggleAutoClaim(Player player) {
        boolean current = autoClaimPlayers.getOrDefault(player.getUniqueId(), false);
        boolean newValue = !current;
        autoClaimPlayers.put(player.getUniqueId(), newValue);
        player.sendMessage(configManager.getMessage(newValue ? "auto-claim-enabled" : "auto-claim-disabled"));
    }

    public void toggleAutoUnclaim(Player player) {
        boolean current = autoUnclaimPlayers.getOrDefault(player.getUniqueId(), false);
        boolean newValue = !current;
        autoUnclaimPlayers.put(player.getUniqueId(), newValue);
        player.sendMessage(configManager.getMessage(newValue ? "auto-unclaim-enabled" : "auto-unclaim-disabled"));
    }

    private void toggleVisibility(Player player) {
        FoliaScheduler.runForPlayer(plugin, player, () -> {
            boolean isVisible = visualizationManager.toggleVisualization(player);
            if (isVisible) {
                player.sendMessage(configManager.getMessage("claim-visibility-toggled"));
            } else {
                player.sendMessage(configManager.getMessage("claim-visibility-toggled"));
            }
        });
    }

    private void toggleVisualizationMode(Player player, String mode) {
        FoliaScheduler.runForPlayer(plugin, player, () -> {
            ClaimProfile profile = claimManager.getActiveProfile(player);
            if (profile == null) {
                player.sendMessage(configManager.getMessage("no-profile"));
                return;
            }

            String newMode;
            switch (mode) {
                case "display_entities":
                case "display-entities":
                case "entity":
                case "entities":
                    newMode = "DISPLAY_ENTITY";
                    break;
                case "particles":
                case "particle":
                    newMode = "PARTICLE";
                    break;
                case "off":
                    newMode = "OFF";
                    break;
                default:
                    player.sendMessage(configManager.getMessage("invalid-visualization-mode"));
                    return;
            }

            String oldMode = profile.getVisualizationMode();
            profile.setVisualizationMode(newMode);
            claimManager.saveAndSync(profile);

            // Update visualization immediately
            visualizationManager.handlePlayerJoin(player);
            visualizationManager.redrawDisplaysForPlayer(player);

            String modeName = newMode.equals("DISPLAY_ENTITY") ? "Display Entities" :
                              newMode.equals("PARTICLE") ? "Particles" : "Off";
            player.sendMessage(configManager.getMessage("visualization-mode-changed", "<mode>", modeName));
        });
    }

    private void togglePvp(Player player, boolean enable, Integer time) {
        FoliaScheduler.runAsync(plugin, () -> {
            org.ayosynk.landClaimPlugin.models.ChunkPosition pos = new org.ayosynk.landClaimPlugin.models.ChunkPosition(player.getLocation());
            ClaimProfile profile = claimManager.getProfileAt(pos);

            if (profile == null) {
                player.sendMessage(configManager.getMessage("not-in-claim"));
                return;
            }

            // Settings permissions are assigned through the profile's category flags.
            if (!profile.canManage(player)
                    && !org.ayosynk.landClaimPlugin.managers.PermissionResolver.hasPermission(
                            profile, player.getUniqueId(), "MANAGE_SETTINGS")) {
                player.sendMessage(configManager.getMessage("no-permission"));
                return;
            }

            profile.setPvpEnabled(enable);
            if (enable && time != null) {
                long endTime = System.currentTimeMillis() + (time * 1000L);
                profile.setPvpTimerEnd(endTime);

                // Schedule broadcast for when timer expires
                FoliaScheduler.runAsyncLater(plugin, () -> {
                    // Check if PvP is still enabled and timer hasn't been renewed/changed
                    if (profile.isPvpEnabled() && profile.getPvpTimerEnd() == endTime) {
                        profile.setPvpEnabled(false);
                        profile.setPvpTimerEnd(0L);
                        plugin.getDatabaseManager().getProfileDao().saveProfile(profile);

                        String expireMsg = configManager.getMessage("pvp-disabled");
                        // Folia: player.getLocation() requires the player's region thread.
                        // Dispatch per-player on the global region (Paper) or per-player
                        // region (Folia) so the location read is safe.
                        for (Player p2 : Bukkit.getOnlinePlayers()) {
                            final Player target = p2;
                            FoliaScheduler.runForPlayer(plugin, target, () -> {
                                org.ayosynk.landClaimPlugin.models.ChunkPosition pPos =
                                        new org.ayosynk.landClaimPlugin.models.ChunkPosition(target.getLocation());
                                if (profile.ownsChunk(pPos)) {
                                    target.sendMessage(expireMsg);
                                }
                            });
                        }
                    }
                }, time * 20L); // Convert seconds to ticks (1s = 20 ticks)
            } else {
                profile.setPvpTimerEnd(0L);
            }

            plugin.getDatabaseManager().getProfileDao().saveProfile(profile);

            // Broadcast to players in the claim
            String messageKey = enable ? (time != null ? "pvp-enabled-temp" : "pvp-enabled") : "pvp-disabled";
            String msg = configManager.getMessage(messageKey);
            if (time != null) {
                msg = msg.replace("<time>", String.valueOf(time));
            }
            final String rawMessage = msg;

            // Folia: player.getLocation() requires the player's region thread.
            // Dispatch per-player so the location read is safe.
            for (Player p : Bukkit.getOnlinePlayers()) {
                final Player target = p;
                FoliaScheduler.runForPlayer(plugin, target, () -> {
                    org.ayosynk.landClaimPlugin.models.ChunkPosition pPos =
                            new org.ayosynk.landClaimPlugin.models.ChunkPosition(target.getLocation());
                    if (profile.ownsChunk(pPos)) {
                        target.sendMessage(rawMessage);
                    }
                });
            }
        });
    }

    private void abandonClaim(Player player) {
        ChunkPosition pos = new ChunkPosition(player.getLocation().getChunk());
        ClaimProfile profile = claimManager.getProfileAt(pos);

        if (profile == null) {
            player.sendMessage(configManager.getMessage("not-in-claim"));
            return;
        }

        if (!profile.getProfileId().equals(player.getUniqueId())) {
            player.sendMessage(configManager.getMessage("not-owner"));
            return;
        }

        claimManager.unclaimChunk(player.getLocation().getChunk());
        player.sendMessage(configManager.getMessage("claim-abandoned"));
    }

    private void sendClaimInfo(Player player) {
        ChunkPosition pos = new ChunkPosition(player.getLocation().getChunk());
        ClaimProfile profile = claimManager.getProfileAt(pos);

        if (profile == null) {
            player.sendMessage(configManager.getMessage("not-in-claim"));
            return;
        }

        String ownerName = profile.getColoredOwnerName();

        player.sendMessage(configManager.getMessage("claim-info-owner", "<owner>", ownerName));
    }

    private void setProfileSpawnpoint(Player player) {
        ClaimProfile profile = claimManager.getActiveProfile(player);
        if (profile == null) {
            player.sendMessage(configManager.getMessage("no-profile"));
            return;
        }
        if (!profile.canManage(player)) {
            player.sendMessage(configManager.getMessage("not-owner"));
            return;
        }
        ChunkPosition currentChunk = new ChunkPosition(player.getLocation().getChunk());
        ClaimProfile atLocation = claimManager.getProfileAt(currentChunk);
        if (atLocation == null || !atLocation.getProfileId().equals(profile.getProfileId())) {
            player.sendMessage(configManager.getMessage("spawnpoint-must-be-in-claim"));
            return;
        }
        profile.setSpawnpoint(player.getLocation());
        claimManager.saveAndSync(profile);
        player.sendMessage(configManager.getMessage("spawnpoint-set"));
    }

    private void removeProfileSpawnpoint(Player player) {
        ClaimProfile profile = claimManager.getActiveProfile(player);
        if (profile == null) {
            player.sendMessage(configManager.getMessage("no-profile"));
            return;
        }
        if (!profile.canManage(player)) {
            player.sendMessage(configManager.getMessage("not-owner"));
            return;
        }
        if (profile.getSpawnpointWorldName() == null) {
            player.sendMessage(configManager.getMessage("spawnpoint-not-set"));
            return;
        }
        profile.setSpawnpoint(null);
        claimManager.saveAndSync(profile);
        player.sendMessage(configManager.getMessage("spawnpoint-removed"));
    }

    private void teleportToProfileSpawnpoint(Player player, String ownerName, String profileName) {
        if (plugin.getCombatManager().isInCombat(player)) {
            player.sendMessage(configManager.getMessage("in-combat"));
            return;
        }
        ClaimProfile profile = claimManager.getProfileByName(profileName);
        if (profile == null || !matchesOwner(profile, ownerName)) {
            player.sendMessage(configManager.getMessage("profile-spawnpoint-not-found"));
            return;
        }
        boolean isMember = profile.isMember(player.getUniqueId());
        boolean isTrusted = profile.isTrusted(player.getUniqueId());
        boolean isAdmin = player.hasPermission("landclaim.admin");
        if (profile.isBanned(player.getUniqueId()) || (!isAdmin && !profile.isOwner(player.getUniqueId())
                && !isMember && !isTrusted)) {
            player.sendMessage(configManager.getMessage("no-permission"));
            return;
        }
        org.bukkit.Location destination = profile.getSpawnpoint();
        if (destination == null || !profile.ownsChunk(new ChunkPosition(destination))) {
            player.sendMessage(configManager.getMessage("spawnpoint-not-set"));
            return;
        }
        player.teleportAsync(destination).thenAccept(success ->
                FoliaScheduler.runForPlayer(plugin, player, () -> {
                    if (success) player.sendMessage(configManager.getMessage("spawnpoint-teleported"));
                    else player.sendMessage(configManager.getMessage("spawnpoint-teleport-failed"));
                }));
    }

    private boolean matchesOwner(ClaimProfile profile, String ownerName) {
        if (profile.getDisplayOwnerName().equalsIgnoreCase(ownerName)) return true;
        String actualName = Bukkit.getOfflinePlayer(profile.getOwnerId()).getName();
        return actualName != null && actualName.equalsIgnoreCase(ownerName);
    }

    // ========== Helpers for new CLI commands ==========

    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9 ]{3,32}$");
    private static final Pattern HEX_PATTERN = Pattern.compile("^#[0-9A-Fa-f]{6}$");

    // Named color -> hex mapping (same as ChangeClaimColorGUI)
    private static final Map<String, String> NAMED_COLORS = Map.ofEntries(
            Map.entry("black", "#1D1D21"),
            Map.entry("blue", "#3C44AA"),
            Map.entry("brown", "#835432"),
            Map.entry("cyan", "#169C9C"),
            Map.entry("gray", "#474F52"),
            Map.entry("grey", "#474F52"),
            Map.entry("green", "#5E7C16"),
            Map.entry("light_blue", "#3AB3DA"),
            Map.entry("lightblue", "#3AB3DA"),
            Map.entry("lime", "#80C71F"),
            Map.entry("light_gray", "#9D9D97"),
            Map.entry("lightgray", "#9D9D97"),
            Map.entry("light_grey", "#9D9D97"),
            Map.entry("lightgrey", "#9D9D97"),
            Map.entry("magenta", "#C74EBD"),
            Map.entry("orange", "#F9801D"),
            Map.entry("pink", "#F38BAA"),
            Map.entry("purple", "#8932B8"),
            Map.entry("red", "#B02E26"),
            Map.entry("white", "#F9FFFE"),
            Map.entry("yellow", "#FED83D")
    );

    /**
     * Resolve the profile for /claim menu subcommands.
     * Priority: location-based profile (if owner/ADMIN_MENU), then active profile.
     */
    private ClaimProfile resolveProfileForMenu(Player player) {
        ChunkPosition pos = new ChunkPosition(player.getLocation());
        ClaimProfile profileAtLoc = claimManager.getProfileAt(pos);

        if (profileAtLoc != null && (profileAtLoc.isOwner(player.getUniqueId()) ||
                org.ayosynk.landClaimPlugin.managers.PermissionResolver.hasPermission(
                        profileAtLoc, player.getUniqueId(), "ADMIN_MENU"))) {
            return profileAtLoc;
        }

        ClaimProfile ownProfile = claimManager.getActiveProfile(player);
        if (ownProfile == null) {
            player.sendMessage(configManager.getMessage("no-profile"));
        }
        return ownProfile;
    }

    /**
     * Check if a player has permission to access a menu subcommand.
     * Owners and admins always pass; members need the specific flag.
     */
    private boolean checkMenuPermission(Player player, ClaimProfile profile, String flag) {
        boolean canManage = profile.canManage(player);
        boolean hasFlag = canManage ||
                org.ayosynk.landClaimPlugin.managers.PermissionResolver.hasPermission(
                        profile, player.getUniqueId(), flag);
        if (!hasFlag) {
            player.sendMessage(configManager.getMessage("no-permission"));
            return false;
        }
        return true;
    }

    private void renameClaim(Player player, String name) {
        ClaimProfile profile = claimManager.getActiveProfile(player);
        if (profile == null) {
            player.sendMessage(configManager.getMessage("no-profile"));
            return;
        }

        if (!profile.canManage(player)) {
            player.sendMessage(configManager.getMessage("not-owner"));
            return;
        }

        if (!NAME_PATTERN.matcher(name).matches()) {
            player.sendMessage(configManager.getMessage("claim-name-invalid"));
            return;
        }

        if (configManager.isBannedClaimName(name)) {
            player.sendMessage(configManager.getMessage("banned-claim-name"));
            return;
        }

        // Check uniqueness
        boolean unique = claimManager.getAllProfiles().stream()
                .noneMatch(cp -> cp.getName().equalsIgnoreCase(name)
                        && !cp.getProfileId().equals(profile.getProfileId()));

        if (!unique) {
            player.sendMessage(configManager.getMessage("name-already-in-use"));
            return;
        }

        profile.setName(name);
        claimManager.saveAndSync(profile);
        player.sendMessage(configManager.getMessage("claim-renamed", "<name>", name));
    }

    private void changeClaimColor(Player player, String colorInput) {
        ClaimProfile profile = claimManager.getActiveProfile(player);
        if (profile == null) {
            player.sendMessage(configManager.getMessage("no-profile"));
            return;
        }

        boolean canManageSettings = profile.canManage(player) ||
                org.ayosynk.landClaimPlugin.managers.PermissionResolver.hasPermission(
                        profile, player.getUniqueId(), "MANAGE_SETTINGS");
        if (!canManageSettings) {
            player.sendMessage(configManager.getMessage("no-permission"));
            return;
        }

        // Resolve color: try named color first, then hex
        String hex = NAMED_COLORS.get(colorInput.toLowerCase());
        if (hex == null) {
            // Try as hex code
            hex = colorInput.startsWith("#") ? colorInput : "#" + colorInput;
            if (!HEX_PATTERN.matcher(hex).matches()) {
                player.sendMessage(configManager.getMessage("claim-color-invalid"));
                return;
            }
        }

        profile.setClaimColor(hex.toUpperCase());
        claimManager.saveAndSync(profile);

        // Refresh hooks and visualization
        plugin.getHookManager().refreshMapHooks();
        plugin.getVisualizationManager().invalidateCache(profile.getProfileId());

        player.sendMessage(configManager.getMessage("claim-color-changed"));
    }

    private void leaveClaim(Player player, String claimName) {
        UUID playerId = player.getUniqueId();
        ClaimProfile profile = claimManager.getProfileByName(claimName);
        if (profile == null) {
            player.sendMessage(configManager.getMessage("not-in-that-claim"));
            return;
        }

        boolean isMember = profile.isMember(playerId);
        boolean isTrusted = profile.isTrusted(playerId);

        if (!isMember && !isTrusted) {
            player.sendMessage(configManager.getMessage("not-in-that-claim"));
            return;
        }

        if (isMember) {
            profile.removeMember(playerId);
        } else {
            profile.removeTrustedPlayer(playerId);
        }

        claimManager.saveAndSync(profile);

        player.sendMessage(configManager.getMessage("claim-left", 
            "<name>", profile.getColoredName(),
            "<owner>", profile.getColoredOwnerName()
        ));

        // Update player's action bar cache
        plugin.getListenerManager().getEventListener().updatePlayerClaimCache(player);

        // Notify the owner if online
        Player owner = Bukkit.getPlayer(profile.getOwnerId());
        if (owner != null && owner.isOnline()) {
            owner.sendMessage(configManager.getMessage("member-left-claim", "<player>", player.getName()));
        }
    }
}
