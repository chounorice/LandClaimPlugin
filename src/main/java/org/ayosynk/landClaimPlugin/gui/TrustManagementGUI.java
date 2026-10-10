package org.ayosynk.landClaimPlugin.gui;

import org.ayosynk.landClaimPlugin.util.FoliaScheduler;
import com.destroystokyo.paper.profile.PlayerProfile;
import net.kyori.adventure.text.Component;
import org.ayosynk.landClaimPlugin.LandClaimPlugin;
import org.ayosynk.landClaimPlugin.config.menus.TrustManagementConfig;
import org.ayosynk.landClaimPlugin.gui.framework.ClickAction;
import org.ayosynk.landClaimPlugin.gui.framework.GuiItem;
import org.ayosynk.landClaimPlugin.gui.framework.PaginatedGui;
import org.ayosynk.landClaimPlugin.gui.framework.SlotDefinition;
import org.ayosynk.landClaimPlugin.models.ClaimProfile;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class TrustManagementGUI {

        public static void open(Player player, ClaimProfile profile, LandClaimPlugin plugin) {
                if (!GuiHelper.checkMenuPermission(player, "trusted", plugin)) {
                        return;
                }
                FoliaScheduler.runAsync(plugin, () -> {
                        TrustManagementConfig config = plugin.getConfigManager().getTrustManagementConfig();

                        List<GuiItem> contentItems = new ArrayList<>();

                        // Build player head items for each trusted player
                        for (UUID trustedId : profile.getTrustedPlayerFlags().keySet()) {
                                String trustedName = Bukkit.getOfflinePlayer(trustedId).getName();
                                if (trustedName == null)
                                        trustedName = trustedId.toString();
                                final String displayName = trustedName;

                                contentItems.add(new GuiItem() {
                                        @Override
                                        public ItemStack render(Player viewer) {
                                                ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
                                                SkullMeta meta = (SkullMeta) skull.getItemMeta();
                                                if (meta != null) {
                                                        meta.setOwningPlayer(Bukkit.getOfflinePlayer(trustedId));
                                                        meta.displayName(GuiHelper.MM
                                                                        .deserialize("<yellow>" + displayName));

                                                        List<Component> lore = new ArrayList<>();
                                                        lore.add(GuiHelper.MM.deserialize("<gray>Right-click: Remove"));
                                                        meta.lore(lore);
                                                        skull.setItemMeta(meta);
                                                }
                                                return skull;
                                        }

                                        @Override
                                        public ClickAction clickAction() {
                                                return (p, e) -> {
                                                        if (e.getClick() == ClickType.RIGHT) {
                                                                // Right-click → confirmation → remove
                                                                FoliaScheduler.runTask(plugin,
                                                                                () -> ConfirmationGUI.open(p,
                                                                                                "<red>Remove " + displayName
                                                                                                                + "?",
                                                                                                () -> {
                                                                                                        profile.removeTrustedPlayer(
                                                                                                                        trustedId);
                                                                                                        plugin.getCacheManager()
                                                                                                                        .getProfileCache()
                                                                                                                        .put(profile.getProfileId(),
                                                                                                                                        profile);
                                                                                                        plugin.getClaimManager()
                                                                                                                        .saveAndSync(profile);
                                                                                                        // Re-open trust
                                                                                                        // management
                                                                                                        TrustManagementGUI
                                                                                                                        .open(p, profile,
                                                                                                                                        plugin);
                                                                                                },
                                                                                                () -> TrustManagementGUI
                                                                                                                .open(p, profile,
                                                                                                                                plugin)));
                                                        }
                                                };
                                        }
                                });
                        }

                        String[] structure = GuiLayoutValidator.validate(config.rows, config.layout,
                                        Set.of("x", "B", "+", "<", "P", "N", "."), "P", "N",
                                        "Trusted", plugin);
                        if (structure == null) {
                                FoliaScheduler.runForPlayer(plugin, player, () -> player.sendMessage(
                                                GuiHelper.MM.deserialize("<red>Trusted menu layout is invalid; check the plugin log.")));
                                return;
                        }

                        Map<Character, SlotDefinition> ingredients = new HashMap<>();
                        ingredients.put('B', GuiHelper.buildSlot(config.bottomFill.material, config.bottomFill.name,
                                        config.bottomFill.lore, config.bottomFill.itemModel));
                        ingredients.put('+', GuiHelper.buildSlot(config.addPlayer.material, config.addPlayer.name,
                                        config.addPlayer.lore, config.addPlayer.itemModel, (p, e) -> {
                                                OnlinePlayerSelectorGUI.open(p, plugin, target -> {
                                                        // Callback: target selected
                                                        if (profile.isOwner(target.getUniqueId())) {
                                                                p.sendMessage(plugin.getConfigManager()
                                                                                .getMessage("cannot-trust-self"));
                                                                return;
                                                        }
                                                        if (profile.isMember(target.getUniqueId())) {
                                                                p.sendMessage(plugin.getConfigManager()
                                                                                .getMessage("already-in-claim"));
                                                                return;
                                                        }
                                                        if (profile.isTrusted(target.getUniqueId())) {
                                                                p.sendMessage(plugin.getConfigManager()
                                                                                .getMessage("already-trusted"));
                                                                return;
                                                        }

                                                        int maxTrusted = plugin.getConfigManager()
                                                                        .getMaxTrustedPlayers(p);
                                                        if (profile.getTrustedPlayerFlags().size() >= maxTrusted
                                                                        && !p.hasPermission("landclaim.admin")) {
                                                                p.sendMessage(GuiHelper.MM.deserialize(
                                                                                "<red>This profile has reached its Trusted-player limit ("
                                                                                                + maxTrusted + ")."));
                                                                return;
                                                        }
                                                        if (!plugin.getClaimManager().addTrustedPlayer(p,
                                                                        target.getUniqueId(), profile)) {
                                                                return;
                                                        }
                                                        target.sendMessage(plugin.getConfigManager().getMessage(
                                                                        "you-are-trusted",
                                                                        "<owner>", profile.getDisplayOwnerName()));
                                                        TrustManagementGUI.open(p, profile, plugin);
                                                }, () -> TrustManagementGUI.open(p, profile, plugin));
                                        }));
                        ingredients.put('<',
                                        GuiHelper.buildSlot(config.back.material, config.back.name, config.back.lore,
                                                        config.back.itemModel,
                                                        (p, e) -> {
                                                                ManageGUI.open(p, profile, plugin);
                                                        }));

                        Component title = GuiHelper.MM.deserialize(config.title);
                        PaginatedGui gui = new PaginatedGui(title, config.rows, structure, ingredients, 'x');

                        gui.setPrevButton(GuiLayoutValidator.findSlot(structure, "P"),
                                        GuiHelper.buildItemStack(config.previousPage.material, config.previousPage.name,
                                                        config.previousPage.lore, config.previousPage.itemModel),
                                        GuiHelper.buildItemStack(config.bottomFill.material, config.bottomFill.name,
                                                        config.bottomFill.lore, config.bottomFill.itemModel));
                        gui.setNextButton(GuiLayoutValidator.findSlot(structure, "N"),
                                        GuiHelper.buildItemStack(config.nextPage.material, config.nextPage.name,
                                                        config.nextPage.lore, config.nextPage.itemModel),
                                        GuiHelper.buildItemStack(config.bottomFill.material, config.bottomFill.name,
                                                        config.bottomFill.lore, config.bottomFill.itemModel));

                        gui.setContent(contentItems, player);
                        gui.open(player);
                });
        }
}
