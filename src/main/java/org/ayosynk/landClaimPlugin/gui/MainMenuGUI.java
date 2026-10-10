package org.ayosynk.landClaimPlugin.gui;

import org.ayosynk.landClaimPlugin.util.FoliaScheduler;
import net.kyori.adventure.text.Component;
import org.ayosynk.landClaimPlugin.LandClaimPlugin;
import org.ayosynk.landClaimPlugin.config.menus.MainMenuConfig;
import org.ayosynk.landClaimPlugin.gui.framework.CustomGui;
import org.ayosynk.landClaimPlugin.gui.framework.SlotDefinition;
import org.ayosynk.landClaimPlugin.models.ClaimProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MainMenuGUI {

        public static void open(Player player, ClaimProfile profile, LandClaimPlugin plugin) {
                if (!GuiHelper.checkMenuPermission(player, "main", plugin)) {
                        return;
                }
                FoliaScheduler.runAsync(plugin, () -> {
                        String ownerName = profile.getProfileId() != null
                                        ? profile.getColoredOwnerName()
                                        : "Unknown";
                        if (ownerName == null)
                                ownerName = "Unknown";
                        String claimName = profile.getColoredName() != null ? profile.getColoredName() : "Unnamed Claim";

                        MainMenuConfig config = plugin.getConfigManager().getMainMenuConfig();

                        String[] structure = validateLayout(config, plugin);
                        if (structure == null) {
                                FoliaScheduler.runForPlayer(plugin, player, () -> player.sendMessage(
                                                GuiHelper.MM.deserialize("<red>Claim menu layout is invalid; check the plugin log.")));
                                return;
                        }

                        boolean canManage = profile.canManage(player);
                        boolean canManageSettings = canManage || org.ayosynk.landClaimPlugin.managers.PermissionResolver.hasPermission(profile, player.getUniqueId(), "MANAGE_SETTINGS");
                        boolean canManageMembers = canManage || org.ayosynk.landClaimPlugin.managers.PermissionResolver.hasPermission(profile, player.getUniqueId(), "MANAGE_MEMBERS");

                        Map<Character, SlotDefinition> ingredients = new HashMap<>();
                        ingredients.put('1', GuiHelper.buildSlot(config.filler1.material, config.filler1.name,
                                        config.filler1.lore, profile, player, ownerName, claimName,
                                        config.filler1.itemModel));
                        ingredients.put('2', GuiHelper.buildSlot(config.filler2.material, config.filler2.name,
                                        config.filler2.lore, profile, player, ownerName, claimName,
                                        config.filler2.itemModel));
                        ingredients.put('S', GuiHelper.buildSlot(config.settings.material, config.settings.name,
                                        config.settings.lore, profile, player, ownerName, claimName,
                                        config.settings.itemModel, (p, e) -> {
                                                // Settings menu itself handles granular permissions, but need MANAGE_SETTINGS or ADMIN_MENU to enter
                                                // Actually ADMIN_MENU is what got them here, so we allow entry.
                                                ClaimSettingsGUI.open(p, profile, plugin);
                                        }));
                        ingredients.put('T', GuiHelper.buildSlot(config.trusted.material, config.trusted.name,
                                        config.trusted.lore, profile, player, ownerName, claimName,
                                        config.trusted.itemModel, (p, e) -> {
                                                if (!canManageMembers) {
                                                        p.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                                                        return;
                                                }
                                                ManageGUI.open(p, profile, plugin);
                                        }));
                        ingredients.put('V', GuiHelper.buildSlot(config.visitors.material, config.visitors.name,
                                        config.visitors.lore, profile, player, ownerName, claimName,
                                        config.visitors.itemModel, (p, e) -> {
                                                if (!canManageSettings) {
                                                        p.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                                                        return;
                                                }
                                                VisitorSettingsGUI.open(p, profile, plugin, "visitor");
                                        }));

                        String windowTitle = config.title.replace("{claim_name}", claimName);
                        Component title = GuiHelper.MM.deserialize(windowTitle);

                        CustomGui gui = new CustomGui(title, config.rows);
                        gui.fillFromStructure(structure, ingredients);
                        gui.open(player);
                });
        }

        private static String[] validateLayout(MainMenuConfig config, LandClaimPlugin plugin) {
                Set<String> allowedSlots = Set.of("1", "2", "S", "T", "V", ".");
                if (config.rows < 1 || config.rows > 6 || config.layout == null
                                || config.layout.size() != config.rows) {
                        plugin.getLogger().severe("Invalid main menu layout: rows must be 1-6 and match the layout list.");
                        return null;
                }
                for (int row = 0; row < config.layout.size(); row++) {
                        String line = config.layout.get(row);
                        if (line == null || line.trim().isEmpty() || line.trim().split("\\s+").length != 9) {
                                plugin.getLogger().severe("Invalid main menu layout row " + (row + 1)
                                                + ": each row must contain exactly nine space-separated slots.");
                                return null;
                        }
                        for (String slot : line.trim().split("\\s+")) {
                                if (!allowedSlots.contains(slot)) {
                                        plugin.getLogger().severe("Invalid main menu layout slot '" + slot
                                                        + "' in row " + (row + 1) + ".");
                                        return null;
                                }
                        }
                }
                return config.layout.toArray(new String[0]);
        }
}
