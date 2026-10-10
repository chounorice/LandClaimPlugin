package org.ayosynk.landClaimPlugin.managers;

import eu.okaeri.configs.yaml.bukkit.YamlBukkitConfigurer;
import eu.okaeri.configs.yaml.bukkit.serdes.SerdesBukkit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.ayosynk.landClaimPlugin.LandClaimPlugin;
import org.ayosynk.landClaimPlugin.config.MessagesConfig;
import org.ayosynk.landClaimPlugin.config.PluginConfig;
import org.ayosynk.landClaimPlugin.config.menus.MainMenuConfig;
import org.ayosynk.landClaimPlugin.config.menus.ManageMenuConfig;
import org.ayosynk.landClaimPlugin.config.menus.VisitorManagementConfig;
import org.ayosynk.landClaimPlugin.config.menus.ClaimSettingsConfig;
import org.ayosynk.landClaimPlugin.config.menus.VisitorSettingsConfig;
import org.ayosynk.landClaimPlugin.config.menus.TrustManagementConfig;
import org.ayosynk.landClaimPlugin.config.menus.MemberManagementConfig;
import org.ayosynk.landClaimPlugin.config.menus.PlayerControlPanelConfig;
import org.ayosynk.landClaimPlugin.config.menus.TitleSettingsConfig;
import org.ayosynk.landClaimPlugin.config.menus.OnlinePlayerSelectorConfig;
import org.ayosynk.landClaimPlugin.config.menus.ProfileSelectorConfig;
import org.ayosynk.landClaimPlugin.config.menus.RenameClaimConfig;
import org.ayosynk.landClaimPlugin.config.menus.ChangeClaimColorConfig;
import java.io.File;
import java.util.List;

public class ConfigManager {
    private final LandClaimPlugin plugin;

    private PluginConfig pluginConfig;
    private MessagesConfig messagesConfig;
    private MainMenuConfig mainMenuConfig;
    private ManageMenuConfig manageMenuConfig;
    private VisitorManagementConfig visitorManagementConfig;
    private ClaimSettingsConfig claimSettingsConfig;
    private VisitorSettingsConfig visitorSettingsConfig;
    private TrustManagementConfig trustManagementConfig;
    private MemberManagementConfig memberManagementConfig;
    private PlayerControlPanelConfig playerControlPanelConfig;
    private TitleSettingsConfig titleSettingsConfig;
    private RenameClaimConfig renameClaimConfig;
    private ChangeClaimColorConfig changeClaimColorConfig;
    private OnlinePlayerSelectorConfig onlinePlayerSelectorConfig;
    private ProfileSelectorConfig profileSelectorConfig;

    private List<String> blockedCommands = List.of();
    private List<String> blockedWorlds = List.of();
    private final java.util.Set<String> bannedClaimNames = new java.util.concurrent.ConcurrentHashMap<String, Boolean>().keySet(Boolean.TRUE);
    private final java.util.Set<String> defaultVisitorFlags = new java.util.concurrent.ConcurrentHashMap<String, Boolean>().keySet(Boolean.TRUE);

    public ConfigManager(LandClaimPlugin plugin) {
        this.plugin = plugin;
        loadConfigs();
    }

    @SuppressWarnings("deprecation")
    private void loadConfigs() {
        this.pluginConfig = eu.okaeri.configs.ConfigManager.create(PluginConfig.class, (it) -> {
            it.withConfigurer(new YamlBukkitConfigurer(), new SerdesBukkit());
            it.withBindFile(new File(plugin.getDataFolder(), "config.yml"));
            it.saveDefaults();
            it.load();
        });
        
        // Validate configuration
        java.util.List<String> configErrors = pluginConfig.validateConfig();
        if (!configErrors.isEmpty()) {
            plugin.getLogger().warning("Configuration validation found " + configErrors.size() + " issue(s):");
            for (String error : configErrors) {
                plugin.getLogger().warning("  - " + error);
            }
            plugin.getLogger().warning("Please fix the configuration errors in config.yml.");
        }

        this.messagesConfig = eu.okaeri.configs.ConfigManager.create(MessagesConfig.class, (it) -> {
            it.withConfigurer(new YamlBukkitConfigurer(), new SerdesBukkit());
            it.withBindFile(new File(plugin.getDataFolder(), "locales/messages_" + pluginConfig.language + ".yml"));
            it.saveDefaults();
            it.load();
        });

        this.mainMenuConfig = eu.okaeri.configs.ConfigManager.create(MainMenuConfig.class, (it) -> {
            it.withConfigurer(new YamlBukkitConfigurer(), new SerdesBukkit());
            it.withBindFile(new File(plugin.getDataFolder(), "menus/mainmenu.yml"));
            it.saveDefaults();
            it.load();
        });
        normalizeLegacyMainMenuLabels();

        this.manageMenuConfig = eu.okaeri.configs.ConfigManager.create(ManageMenuConfig.class, (it) -> {
            it.withConfigurer(new YamlBukkitConfigurer(), new SerdesBukkit());
            it.withBindFile(new File(plugin.getDataFolder(), "menus/Manage.yml"));
            it.saveDefaults();
            it.load();
        });

        this.visitorManagementConfig = eu.okaeri.configs.ConfigManager.create(VisitorManagementConfig.class, (it) -> {
            it.withConfigurer(new YamlBukkitConfigurer(), new SerdesBukkit());
            it.withBindFile(new File(plugin.getDataFolder(), "menus/VisitorManagement.yml"));
            it.saveDefaults();
            it.load();
        });

        this.profileSelectorConfig = eu.okaeri.configs.ConfigManager.create(ProfileSelectorConfig.class, (it) -> {
            it.withConfigurer(new YamlBukkitConfigurer(), new SerdesBukkit());
            it.withBindFile(new File(plugin.getDataFolder(), "menus/profile-selector.yml"));
            it.saveDefaults();
            it.load();
        });

        this.claimSettingsConfig = eu.okaeri.configs.ConfigManager.create(ClaimSettingsConfig.class, (it) -> {
            it.withConfigurer(new YamlBukkitConfigurer(), new SerdesBukkit());
            it.withBindFile(new File(plugin.getDataFolder(), "menus/ClaimSettings.yml"));
            it.saveDefaults();
            it.load();
        });

        this.visitorSettingsConfig = eu.okaeri.configs.ConfigManager.create(VisitorSettingsConfig.class, (it) -> {
            it.withConfigurer(new YamlBukkitConfigurer(), new SerdesBukkit());
            it.withBindFile(new File(plugin.getDataFolder(), "menus/VisitorSettings.yml"));
            it.saveDefaults();
            it.load();
        });

        // Ensure new flags exist to fix backward compatibility for servers updating from older versions
        if (!visitorSettingsConfig.flags.containsKey("CLAIM_LAND")) {
            visitorSettingsConfig.flags.put("CLAIM_LAND", new VisitorSettingsConfig.ItemConfig("GOLDEN_SHOVEL", "<yellow>Claim Land", java.util.List.of("<gray>Allow claiming land on behalf of owner.")));
        }
        if (!visitorSettingsConfig.flags.containsKey("ADMIN_MENU")) {
            visitorSettingsConfig.flags.put("ADMIN_MENU", new VisitorSettingsConfig.ItemConfig("COMMAND_BLOCK", "<gold>Admin Menu Access", java.util.List.of("<gray>Allow members to open the /claim menu.", "<gray>Does not allow abandoning the claim.")));
        }
        if (!visitorSettingsConfig.flags.containsKey("MANAGE_SETTINGS")) {
            visitorSettingsConfig.flags.put("MANAGE_SETTINGS", new VisitorSettingsConfig.ItemConfig("COMPARATOR", "<yellow>Manage Settings", java.util.List.of("<gray>Allow changing claim settings (PvP, Color, Toggles).")));
        }
        if (!visitorSettingsConfig.flags.containsKey("MANAGE_MEMBERS")) {
            visitorSettingsConfig.flags.put("MANAGE_MEMBERS", new VisitorSettingsConfig.ItemConfig("PLAYER_HEAD", "<yellow>Manage Members", java.util.List.of("<gray>Allow adding/removing members and trusted players.")));
        }
        // Keep the new defaults in memory. Saving here rewrites Okaeri-managed
        // @Comment metadata, which reintroduces comments administrators removed.

        this.trustManagementConfig = eu.okaeri.configs.ConfigManager.create(TrustManagementConfig.class, (it) -> {
            it.withConfigurer(new YamlBukkitConfigurer(), new SerdesBukkit());
            it.withBindFile(new File(plugin.getDataFolder(), "menus/TrustManagement.yml"));
            it.saveDefaults();
            it.load();
        });

        this.memberManagementConfig = eu.okaeri.configs.ConfigManager.create(MemberManagementConfig.class, (it) -> {
            it.withConfigurer(new YamlBukkitConfigurer(), new SerdesBukkit());
            it.withBindFile(new File(plugin.getDataFolder(), "menus/MemberManagement.yml"));
            it.saveDefaults();
            it.load();
        });

        this.playerControlPanelConfig = eu.okaeri.configs.ConfigManager.create(PlayerControlPanelConfig.class, (it) -> {
            it.withConfigurer(new YamlBukkitConfigurer(), new SerdesBukkit());
            it.withBindFile(new File(plugin.getDataFolder(), "menus/PlayerControlPanel.yml"));
            it.saveDefaults();
            it.load();
        });

        this.titleSettingsConfig = eu.okaeri.configs.ConfigManager.create(TitleSettingsConfig.class, (it) -> {
            it.withConfigurer(new YamlBukkitConfigurer(), new SerdesBukkit());
            it.withBindFile(new File(plugin.getDataFolder(), "menus/TitleSettings.yml"));
            it.saveDefaults();
            it.load();
        });

        this.renameClaimConfig = eu.okaeri.configs.ConfigManager.create(RenameClaimConfig.class, (it) -> {
            it.withConfigurer(new YamlBukkitConfigurer(), new SerdesBukkit());
            it.withBindFile(new File(plugin.getDataFolder(), "menus/RenameClaim.yml"));
            it.saveDefaults();
            it.load();
        });

        this.changeClaimColorConfig = eu.okaeri.configs.ConfigManager.create(ChangeClaimColorConfig.class, (it) -> {
            it.withConfigurer(new YamlBukkitConfigurer(), new SerdesBukkit());
            it.withBindFile(new File(plugin.getDataFolder(), "menus/ChangeClaimColor.yml"));
            it.saveDefaults();
            it.load();
        });

        this.onlinePlayerSelectorConfig = eu.okaeri.configs.ConfigManager.create(OnlinePlayerSelectorConfig.class, (it) -> {
            it.withConfigurer(new YamlBukkitConfigurer(), new SerdesBukkit());
            it.withBindFile(new File(plugin.getDataFolder(), "menus/OnlinePlayerSelector.yml"));
            it.saveDefaults();
            it.load();
        });

        blockedCommands = pluginConfig.blockCmd.stream().map(String::toLowerCase).toList();
        blockedWorlds = pluginConfig.blockWorld.stream().map(String::toLowerCase).toList();
        loadBannedWords();
        loadDefaultVisitorFlags();
    }

    public void reloadMainConfig() {
        pluginConfig.load();
        messagesConfig.load();
        mainMenuConfig.load();
        normalizeLegacyMainMenuLabels();
        manageMenuConfig.load();
        visitorManagementConfig.load();
        claimSettingsConfig.load();
        visitorSettingsConfig.load();
        trustManagementConfig.load();
        memberManagementConfig.load();
        playerControlPanelConfig.load();
        titleSettingsConfig.load();
        renameClaimConfig.load();
        changeClaimColorConfig.load();
        onlinePlayerSelectorConfig.load();

        blockedCommands = pluginConfig.blockCmd.stream().map(String::toLowerCase).toList();
        blockedWorlds = pluginConfig.blockWorld.stream().map(String::toLowerCase).toList();
        loadBannedWords();
        loadDefaultVisitorFlags();
    }

    private void normalizeLegacyMainMenuLabels() {
        if (mainMenuConfig.trusted != null
                && "Trusted Members Management".equalsIgnoreCase(mainMenuConfig.trusted.name)) {
            mainMenuConfig.trusted.name = "<gold>Manage";
        }
        if (mainMenuConfig.visitors != null
                && "Visitor Settings".equalsIgnoreCase(mainMenuConfig.visitors.name)) {
            mainMenuConfig.visitors.name = "<yellow>Flags";
        }
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    public MessagesConfig getMessagesConfig() {
        return messagesConfig;
    }

    public MainMenuConfig getMainMenuConfig() {
        return mainMenuConfig;
    }

    public ManageMenuConfig getManageMenuConfig() {
        return manageMenuConfig;
    }

    public VisitorManagementConfig getVisitorManagementConfig() {
        return visitorManagementConfig;
    }

    public ClaimSettingsConfig getClaimSettingsConfig() {
        return claimSettingsConfig;
    }

    public VisitorSettingsConfig getVisitorSettingsConfig() {
        return visitorSettingsConfig;
    }

    public TrustManagementConfig getTrustManagementConfig() {
        return trustManagementConfig;
    }

    public MemberManagementConfig getMemberManagementConfig() {
        return memberManagementConfig;
    }

    public PlayerControlPanelConfig getPlayerControlPanelConfig() {
        return playerControlPanelConfig;
    }

    public TitleSettingsConfig getTitleSettingsConfig() {
        return titleSettingsConfig;
    }

    public RenameClaimConfig getRenameClaimConfig() {
        return renameClaimConfig;
    }

    public int getMaxMemberships() {
        return pluginConfig.maxMemberships;
    }

    public int getMaxTrustedPlayers(org.bukkit.entity.Player player) {
        int limit = Math.max(0, pluginConfig.maxTrustedPlayers);
        for (org.bukkit.permissions.PermissionAttachmentInfo permission : player.getEffectivePermissions()) {
            String node = permission.getPermission();
            String prefix = "landclaim.trust.limit.";
            if (!node.startsWith(prefix)) continue;
            try {
                limit = Math.max(limit, Integer.parseInt(node.substring(prefix.length())));
            } catch (NumberFormatException ignored) {
                // Ignore malformed permission nodes; valid numeric limits remain effective.
            }
        }
        return limit;
    }

    public ChangeClaimColorConfig getChangeClaimColorConfig() {
        return changeClaimColorConfig;
    }

    public ProfileSelectorConfig getProfileSelectorConfig() {
        return profileSelectorConfig;
    }

    public OnlinePlayerSelectorConfig getOnlinePlayerSelectorConfig() {
        return onlinePlayerSelectorConfig;
    }

    // --- Legacy wrapper methods to bridge until other classes are transformed ---

    public boolean requireConnectedClaims() {
        return pluginConfig.requireConnectedClaims;
    }

    public boolean allowDiagonalConnections() {
        return pluginConfig.allowDiagonalConnections;
    }

    public boolean isWorldBlocked(String worldName) {
        return blockedWorlds.contains(worldName.toLowerCase());
    }

    public List<String> getBlockedCommands() {
        return blockedCommands;
    }

    public List<String> getBlockedWorlds() {
        return blockedWorlds;
    }

    public boolean isMultiProfilesEnabled() {
        return pluginConfig.multiProfilesEnabled;
    }

    public int getMaxProfilesPerPlayer() {
        return pluginConfig.maxProfilesPerPlayer;
    }

    public int getUnstuckCooldown() {
        return pluginConfig.cooldownUnstuck;
    }

    public int getWorldGuardGap() {
        return pluginConfig.worldguardGap;
    }

    public int getMinClaimGap() {
        return pluginConfig.minClaimGap;
    }

    public boolean useSeparatePremission() {
        return pluginConfig.useSeparatePremission;
    }

    public String getDefaultVisualizationMode() {
        return "OFF";
    }

    public boolean isActionBarEnabled() {
        return pluginConfig.actionbarEnabled;
    }

    public int getActionBarUpdateInterval() {
        return pluginConfig.actionbarUpdateInterval;
    }

    public boolean isClaimChatNotificationsEnabled() {
        return pluginConfig.claimChatNotifications;
    }

    public boolean isVisitorSettingsLocked() {
        return pluginConfig.visitorSettings != null && pluginConfig.visitorSettings.locked;
    }

    public boolean hasDefaultVisitorFlag(String flag) {
        if (flag == null) return false;
        return defaultVisitorFlags.contains(flag.toUpperCase());
    }

    public java.util.Set<String> getDefaultVisitorFlags() {
        return java.util.Collections.unmodifiableSet(defaultVisitorFlags);
    }

    private void loadDefaultVisitorFlags() {
        defaultVisitorFlags.clear();
        if (pluginConfig.visitorSettings != null && pluginConfig.visitorSettings.defaultFlags != null) {
            for (String flag : pluginConfig.visitorSettings.defaultFlags) {
                if (flag != null && !flag.isBlank()) {
                    defaultVisitorFlags.add(flag.trim().toUpperCase());
                }
            }
        }
    }

    // --- MiniMessage formatting ---

    public String getMessage(String key, String... replacements) {
        String template = getRawMessageString(key);
        for (int i = 0; i < replacements.length; i += 2) {
            String placeholder = replacements[i];
            String value = replacements[i + 1];
            template = template.replace(placeholder, value);
            
            // Support alternate bracket style if placeholder starts with < or {
            if (placeholder.startsWith("<") && placeholder.endsWith(">")) {
                String alt = "{" + placeholder.substring(1, placeholder.length() - 1) + "}";
                template = template.replace(alt, value);
            } else if (placeholder.startsWith("{") && placeholder.endsWith("}")) {
                String alt = "<" + placeholder.substring(1, placeholder.length() - 1) + ">";
                template = template.replace(alt, value);
            }
        }
        Component comp = MiniMessage.miniMessage().deserialize(pluginConfig.prefix + template);
        return LegacyComponentSerializer.legacySection().serialize(comp);
    }

    public String getRawMessage(String key, String... replacements) {
        String template = getRawMessageString(key);
        for (int i = 0; i < replacements.length; i += 2) {
            String placeholder = replacements[i];
            String value = replacements[i + 1];
            template = template.replace(placeholder, value);

            // Support alternate bracket style
            if (placeholder.startsWith("<") && placeholder.endsWith(">")) {
                String alt = "{" + placeholder.substring(1, placeholder.length() - 1) + "}";
                template = template.replace(alt, value);
            } else if (placeholder.startsWith("{") && placeholder.endsWith("}")) {
                String alt = "<" + placeholder.substring(1, placeholder.length() - 1) + ">";
                template = template.replace(alt, value);
            }
        }
        Component comp = MiniMessage.miniMessage().deserialize(template);
        return LegacyComponentSerializer.legacySection().serialize(comp);
    }

    public String getActionBarMessage(String key) {
        return getRawMessageString(key);
    }

    private String getRawMessageString(String key) {
        try {
            // Convert kebab-case to camelCase if needed
            String camelCaseKey = key;
            if (key.contains("-")) {
                StringBuilder sb = new StringBuilder();
                boolean nextUpper = false;
                for (char c : key.toCharArray()) {
                    if (c == '-') {
                        nextUpper = true;
                    } else if (nextUpper) {
                        sb.append(Character.toUpperCase(c));
                        nextUpper = false;
                    } else {
                        sb.append(c);
                    }
                }
                camelCaseKey = sb.toString();
            }

            var field = messagesConfig.getClass().getField(camelCaseKey);
            return (String) field.get(messagesConfig);
        } catch (Exception e) {
            return "<red>Message not found: " + key;
        }
    }

    public void loadBannedWords() {
        bannedClaimNames.clear();
        String fileName = pluginConfig.bannedClaimNamesFile;
        if (fileName == null || fileName.isEmpty()) {
            fileName = "banned-claim-name.txt";
        }
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            try {
                if (!file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }
                java.nio.file.Files.write(file.toPath(), java.util.List.of(
                    "# LandClaimPlugin - Banned Claim Names",
                    "# Add words or phrases (one per line) that cannot be used in claim profile names.",
                    "# Any claim name containing these words (case-insensitive) will be rejected.",
                    "# Empty lines and lines starting with '#' are ignored.",
                    "",
                    "slur1",
                    "slur2",
                    "offensiveword1",
                    "offensiveword2",
                    "badword"
                ));
            } catch (java.io.IOException e) {
                plugin.getLogger().severe("Failed to create default " + fileName + ": " + e.getMessage());
            }
        }

        if (file.exists()) {
            try {
                java.util.List<String> lines = java.nio.file.Files.readAllLines(file.toPath());
                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    bannedClaimNames.add(line.toLowerCase());
                }
                plugin.getLogger().info("Loaded " + bannedClaimNames.size() + " banned claim name words from " + fileName);
            } catch (java.io.IOException e) {
                plugin.getLogger().severe("Failed to read " + fileName + ": " + e.getMessage());
            }
        }
    }

    public boolean isBannedClaimName(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase();
        for (String banned : bannedClaimNames) {
            if (lower.contains(banned)) {
                return true;
            }
        }
        return false;
    }
}