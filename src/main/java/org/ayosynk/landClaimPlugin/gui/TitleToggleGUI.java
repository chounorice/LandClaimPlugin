package org.ayosynk.landClaimPlugin.gui;

import org.ayosynk.landClaimPlugin.util.FoliaScheduler;
import net.kyori.adventure.text.Component;
import org.ayosynk.landClaimPlugin.LandClaimPlugin;
import org.ayosynk.landClaimPlugin.config.menus.TitleSettingsConfig;
import org.ayosynk.landClaimPlugin.gui.framework.CustomGui;
import org.ayosynk.landClaimPlugin.gui.framework.SlotDefinition;
import org.ayosynk.landClaimPlugin.models.ClaimProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TitleToggleGUI {

    public static void open(Player player, ClaimProfile profile, LandClaimPlugin plugin) {
        FoliaScheduler.runAsync(plugin, () -> {
            TitleSettingsConfig config = plugin.getConfigManager().getTitleSettingsConfig();

            String[] structure = GuiLayoutValidator.validate(config.rows, config.layout,
                    Set.of("F", "T", "E", "O", "S", "<", "."), null, null,
                    "Title Settings", plugin);
            if (structure == null) {
                FoliaScheduler.runForPlayer(plugin, player, () -> player.sendMessage(
                        GuiHelper.MM.deserialize("<red>Title settings layout is invalid; check the plugin log.")));
                return;
            }

            Map<Character, SlotDefinition> ingredients = new HashMap<>();
            ingredients.put('F', GuiHelper.buildSlot(config.frame.material, config.frame.name,
                    config.frame.lore, config.frame.itemModel));
            ingredients.put('S', GuiHelper.buildSlot(config.navSpacer.material, config.navSpacer.name,
                    config.navSpacer.lore, config.navSpacer.itemModel));

            // --- Toggle button ---
            List<String> toggleLore = new ArrayList<>(config.titleToggle.lore);
            toggleLore.add("");
            toggleLore.add("<gray>Status: "
                    + (profile.isEnterTitleEnabled() ? "<green>Enabled" : "<red>Disabled"));
            ingredients.put('T', GuiHelper.buildSlot(config.titleToggle.material, config.titleToggle.name,
                    toggleLore, config.titleToggle.itemModel, (p, e) -> {
                        profile.setEnterTitleEnabled(!profile.isEnterTitleEnabled());
                        plugin.getDatabaseManager().getProfileDao().saveProfile(profile);
                        open(p, profile, plugin);
                    }));

            // --- Entry title button ---
            boolean enterIsSub = "SUBTITLE".equalsIgnoreCase(profile.getEnterTitleMode());
            List<String> entryLore = new ArrayList<>(config.onEntry.lore);
            entryLore.add("");
            entryLore.add("<gray>Text: <reset>" + profile.getEnterTitle());
            entryLore.add("<gray>Mode: " + (enterIsSub ? "<aqua>Subtitle" : "<gold>Title"));
            entryLore.add("");
            entryLore.add("<dark_gray><i>Left-click to set text");
            entryLore.add("<dark_gray><i>Right-click to toggle Title/Subtitle");

            ingredients.put('E', GuiHelper.buildSlot(config.onEntry.material, config.onEntry.name,
                    entryLore, config.onEntry.itemModel, (p, e) -> {
                        if (e.getClick() == ClickType.RIGHT) {
                            // Toggle mode
                            profile.setEnterTitleMode(enterIsSub ? "TITLE" : "SUBTITLE");
                            plugin.getDatabaseManager().getProfileDao().saveProfile(profile);
                            open(p, profile, plugin);
                        } else {
                            // Open AnvilGUI to set text
                            AnvilInputGUI.open(plugin, p, "Enter Title", profile.getEnterTitle(),
                                    input -> {
                                        if (input == null) {
                                            p.sendMessage(GuiHelper.MM.deserialize(
                                                    plugin.getConfigManager()
                                                            .getMessagesConfig().titleCancelled));
                                        } else {
                                            profile.setEnterTitle(input);
                                            plugin.getDatabaseManager()
                                                    .getProfileDao()
                                                    .saveProfile(profile);
                                            p.sendMessage(GuiHelper.MM.deserialize(
                                                    plugin.getConfigManager()
                                                            .getMessagesConfig().titleUpdated));
                                        }
                                        FoliaScheduler.runTask(plugin,
                                                () -> open(p, profile, plugin));
                                    });
                        }
                    }));

            // --- Leave title button ---
            boolean leaveIsSub = "SUBTITLE".equalsIgnoreCase(profile.getLeaveTitleMode());
            List<String> leaveLore = new ArrayList<>(config.onLeaveTitle.lore);
            leaveLore.add("");
            leaveLore.add("<gray>Text: <reset>" + profile.getLeaveTitle());
            leaveLore.add("<gray>Mode: " + (leaveIsSub ? "<aqua>Subtitle" : "<gold>Title"));
            leaveLore.add("");
            leaveLore.add("<dark_gray><i>Left-click to set text");
            leaveLore.add("<dark_gray><i>Right-click to toggle Title/Subtitle");

            ingredients.put('O', GuiHelper.buildSlot(config.onLeaveTitle.material, config.onLeaveTitle.name,
                    leaveLore, config.onLeaveTitle.itemModel, (p, e) -> {
                        if (e.getClick() == ClickType.RIGHT) {
                            // Toggle mode
                            profile.setLeaveTitleMode(leaveIsSub ? "TITLE" : "SUBTITLE");
                            plugin.getDatabaseManager().getProfileDao().saveProfile(profile);
                            open(p, profile, plugin);
                        } else {
                            // Open AnvilGUI to set text
                            AnvilInputGUI.open(plugin, p, "Leave Title", profile.getLeaveTitle(),
                                    input -> {
                                        if (input == null) {
                                            p.sendMessage(GuiHelper.MM.deserialize(
                                                    plugin.getConfigManager()
                                                            .getMessagesConfig().titleCancelled));
                                        } else {
                                            profile.setLeaveTitle(input);
                                            plugin.getDatabaseManager()
                                                    .getProfileDao()
                                                    .saveProfile(profile);
                                            p.sendMessage(GuiHelper.MM.deserialize(
                                                    plugin.getConfigManager()
                                                            .getMessagesConfig().titleUpdated));
                                        }
                                        FoliaScheduler.runTask(plugin,
                                                () -> open(p, profile, plugin));
                                    });
                        }
                    }));

            ingredients.put('<',
                    GuiHelper.buildSlot(config.back.material, config.back.name, config.back.lore,
                            config.back.itemModel,
                            (p, e) -> {
                                ClaimSettingsGUI.open(p, profile, plugin);
                            }));

            Component title = GuiHelper.MM.deserialize(config.title);
            CustomGui gui = new CustomGui(title, config.rows);
            gui.fillFromStructure(structure, ingredients);
            gui.open(player);
        });
    }
}
