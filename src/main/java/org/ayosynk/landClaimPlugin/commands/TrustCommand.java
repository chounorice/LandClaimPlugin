package org.ayosynk.landClaimPlugin.commands;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.ayosynk.landClaimPlugin.LandClaimPlugin;
import org.ayosynk.landClaimPlugin.managers.ClaimManager;
import org.ayosynk.landClaimPlugin.managers.ConfigManager;
import org.ayosynk.landClaimPlugin.models.ClaimProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.paper.PaperCommandManager;
import org.incendo.cloud.paper.util.sender.PlayerSource;
import org.incendo.cloud.paper.util.sender.Source;
import org.incendo.cloud.parser.standard.StringParser;

import java.util.Set;
import java.util.UUID;

/** Handles direct Trusted membership management. */
public class TrustCommand implements LandClaimCommand {

    private final LandClaimPlugin plugin;
    private final ClaimManager claimManager;
    private final ConfigManager configManager;

    public TrustCommand(LandClaimPlugin plugin, ClaimManager claimManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.claimManager = claimManager;
        this.configManager = configManager;
    }

    @Override
    public void register(PaperCommandManager<Source> manager, Command.Builder<PlayerSource> claimBuilder) {
        Command.Builder<PlayerSource> trustBuilder = claimBuilder.literal("trust");

        manager.command(trustBuilder.literal("list").handler(context -> {
            Player player = context.sender().source();
            if (!org.ayosynk.landClaimPlugin.gui.GuiHelper.checkPermission(player, "landclaim.trust", plugin)) return;
            ClaimProfile profile = claimManager.getActiveProfile(player);
            if (profile == null) {
                player.sendMessage(configManager.getMessage("no-profile"));
                return;
            }
            var trusted = profile.getTrustedPlayerFlags();
            if (trusted.isEmpty()) {
                player.sendMessage(configManager.getMessage("trust-list-empty"));
                return;
            }
            player.sendMessage(configManager.getMessage("trust-list-header"));
            for (UUID trustedId : trusted.keySet()) {
                String name = Bukkit.getOfflinePlayer(trustedId).getName();
                Set<String> flags = profile.getCategoryFlags("trusted");
                player.sendMessage(configManager.getMessage("trust-list-entry",
                        "<player>", name != null ? name : trustedId.toString(),
                        "<flags>", String.join(", ", flags)));
            }
        }));

        manager.command(trustBuilder.literal("add")
                .required("player", StringParser.stringParser(), OfflinePlayerSuggestions.all())
                .handler(context -> {
                    Player player = context.sender().source();
                    if (!org.ayosynk.landClaimPlugin.gui.GuiHelper.checkPermission(player, "landclaim.trust", plugin)) return;
                    ClaimProfile profile = claimManager.getActiveProfile(player);
                    if (profile == null) {
                        player.sendMessage(configManager.getMessage("no-profile"));
                        return;
                    }
                    if (!profile.canManage(player)) {
                        player.sendMessage(configManager.getMessage("not-owner"));
                        return;
                    }
                    String targetName = context.get("player");
                    Player onlineTarget = Bukkit.getPlayer(targetName);
                    @SuppressWarnings("deprecation")
                    var offlineTarget = onlineTarget == null ? Bukkit.getOfflinePlayer(targetName) : null;
                    UUID targetId = onlineTarget != null ? onlineTarget.getUniqueId() : offlineTarget.getUniqueId();

                    if (profile.isOwner(targetId)) {
                        player.sendMessage(configManager.getMessage("cannot-trust-self"));
                        return;
                    }
                    if (profile.isMember(targetId)) {
                        player.sendMessage(configManager.getMessage("already-in-claim"));
                        return;
                    }
                    if (profile.isTrusted(targetId)) {
                        player.sendMessage(configManager.getMessage("already-trusted"));
                        return;
                    }
                    int maxTrusted = configManager.getMaxTrustedPlayers(player);
                    if (profile.getTrustedPlayerFlags().size() >= maxTrusted
                            && !player.hasPermission("landclaim.admin")) {
                        player.sendMessage(MiniMessage.miniMessage().deserialize(
                                "<red>This profile has reached its Trusted-player limit (" + maxTrusted + ")."));
                        return;
                    }

                    if (!claimManager.addTrustedPlayer(player, targetId, profile)) {
                        return;
                    }
                    player.sendMessage(configManager.getMessage("trust-added", "<player>", targetName));
                    if (onlineTarget != null) {
                        onlineTarget.sendMessage(configManager.getMessage("you-are-trusted",
                                "<owner>", profile.getDisplayOwnerName()));
                    }
                }));

        manager.command(trustBuilder.literal("remove")
                .required("player", StringParser.stringParser(), OfflinePlayerSuggestions.all())
                .handler(context -> {
                    Player player = context.sender().source();
                    if (!org.ayosynk.landClaimPlugin.gui.GuiHelper.checkPermission(player, "landclaim.trust", plugin)) return;
                    ClaimProfile profile = claimManager.getActiveProfile(player);
                    if (profile == null) {
                        player.sendMessage(configManager.getMessage("no-profile"));
                        return;
                    }
                    if (!profile.canManage(player)) {
                        player.sendMessage(configManager.getMessage("not-owner"));
                        return;
                    }
                    String targetName = context.get("player");
                    Player target = Bukkit.getPlayer(targetName);
                    @SuppressWarnings("deprecation")
                    var offlineTarget = target == null ? Bukkit.getOfflinePlayer(targetName) : null;
                    UUID targetId = target != null ? target.getUniqueId() : offlineTarget.getUniqueId();
                    if (!profile.isTrusted(targetId)) {
                        player.sendMessage(configManager.getMessage("not-trusted"));
                        return;
                    }
                    profile.removeTrustedPlayer(targetId);
                    plugin.getCacheManager().getProfileCache().put(profile.getProfileId(), profile);
                    claimManager.saveAndSync(profile);
                    player.sendMessage(configManager.getMessage("trust-removed", "<player>", targetName));
                }));
    }
}
