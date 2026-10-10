package org.ayosynk.landClaimPlugin.gui;

import org.ayosynk.landClaimPlugin.util.FoliaScheduler;
import net.kyori.adventure.text.Component;
import org.ayosynk.landClaimPlugin.LandClaimPlugin;
import org.ayosynk.landClaimPlugin.config.menus.MemberManagementConfig;
import org.ayosynk.landClaimPlugin.gui.framework.ClickAction;
import org.ayosynk.landClaimPlugin.gui.framework.GuiItem;
import org.ayosynk.landClaimPlugin.gui.framework.PaginatedGui;
import org.ayosynk.landClaimPlugin.gui.framework.SlotDefinition;
import org.ayosynk.landClaimPlugin.models.ClaimProfile;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class MemberManagementGUI {

        public static void open(Player player, ClaimProfile profile, LandClaimPlugin plugin) {
                if (!GuiHelper.checkMenuPermission(player, "members", plugin)) {
                        return;
                }
                FoliaScheduler.runAsync(plugin, () -> {
                        MemberManagementConfig config = plugin.getConfigManager().getMemberManagementConfig();

                        List<GuiItem> contentItems = new ArrayList<>();

                        // Build player head items for each member
                        for (Map.Entry<UUID, String> entry : profile.getMemberRoles().entrySet()) {
                                UUID memberId = entry.getKey();
                                String role = entry.getValue();

                                String memberName = Bukkit.getOfflinePlayer(memberId).getName();
                                if (memberName == null)
                                        memberName = memberId.toString();
                                final String displayName = memberName;

                                contentItems.add(new GuiItem() {
                                        @Override
                                        public ItemStack render(Player viewer) {
                                                ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
                                                SkullMeta meta = (SkullMeta) skull.getItemMeta();
                                                if (meta != null) {
                                                        meta.setOwningPlayer(Bukkit.getOfflinePlayer(memberId));
                                                        meta.displayName(GuiHelper.MM
                                                                        .deserialize("<yellow>" + displayName));

                                                        List<Component> lore = new ArrayList<>();
                                                        lore.add(GuiHelper.MM
                                                                        .deserialize("<gray>Role: <white>" + role));
                                                        lore.add(Component.empty());
                                                        lore.add(GuiHelper.MM.deserialize("<yellow>Click to manage"));
                                                        meta.lore(lore);
                                                        skull.setItemMeta(meta);
                                                }
                                                return skull;
                                        }

                                        @Override
                                        public ClickAction clickAction() {
                                                return (p, e) -> {
                                                        PlayerControlPanelGUI.open(p, profile, plugin, memberId,
                                                                        displayName);
                                                };
                                        }
                                });
                        }

                        String[] structure = validateLayout(config, plugin);
                        if (structure == null) {
                                FoliaScheduler.runForPlayer(plugin, player, () -> player.sendMessage(
                                                GuiHelper.MM.deserialize("<red>Resident menu layout is invalid; check the plugin log.")));
                                return;
                        }

                        Map<Character, SlotDefinition> ingredients = new HashMap<>();
                        ingredients.put('B', GuiHelper.buildSlot(config.bottomFill.material, config.bottomFill.name,
                                        config.bottomFill.lore, config.bottomFill.itemModel));
                        ingredients.put('+', GuiHelper.buildSlot(config.inviteMember.material, config.inviteMember.name,
                                        config.inviteMember.lore, config.inviteMember.itemModel, (p, e) -> {
                                                OnlinePlayerSelectorGUI.open(p, plugin, target -> {
                                                        // Callback: target selected
                                                        if (profile.isOwner(target.getUniqueId())) {
                                                                p.sendMessage(plugin.getConfigManager()
                                                                                .getMessage("cannot-invite-self"));
                                                                return;
                                                        }
                                                        if (profile.isMember(target.getUniqueId())) {
                                                                p.sendMessage(plugin.getConfigManager()
                                                                                .getMessage("already-member"));
                                                                return;
                                                        }

                                                        int maxResidents = plugin.getConfigManager()
                                                                        .getPluginConfig().maxClaimMembers
                                                                        + profile.getBonusMemberSlots();
                                                        if (profile.getMemberRoles().size() >= maxResidents
                                                                        && !p.hasPermission("landclaim.admin")) {
                                                                p.sendMessage(GuiHelper.MM.deserialize(
                                                                                "<red>This profile has reached its Resident limit ("
                                                                                                + maxResidents + ")."));
                                                                return;
                                                        }

                                                        // Logic from /claim member invite
                                                        plugin.getClaimManager().sendMemberInvite(p, target, profile);
                                                        MemberManagementGUI.open(p, profile, plugin);
                                                }, () -> MemberManagementGUI.open(p, profile, plugin));
                                        }));
                        ingredients.put('<',
                                        GuiHelper.buildSlot(config.back.material, config.back.name, config.back.lore,
                                                        config.back.itemModel,
                                                        (p, e) -> ManageGUI.open(p, profile, plugin)));

                        Component title = GuiHelper.MM.deserialize(config.title);
                        PaginatedGui gui = new PaginatedGui(title, config.rows, structure, ingredients, 'x');

                        gui.setPrevButton(findSlot(structure, "P"),
                                        GuiHelper.buildItemStack(config.previousPage.material, config.previousPage.name,
                                                        config.previousPage.lore, config.previousPage.itemModel),
                                        GuiHelper.buildItemStack(config.bottomFill.material, config.bottomFill.name,
                                                        config.bottomFill.lore, config.bottomFill.itemModel));
                        gui.setNextButton(findSlot(structure, "N"),
                                        GuiHelper.buildItemStack(config.nextPage.material, config.nextPage.name,
                                                        config.nextPage.lore, config.nextPage.itemModel),
                                        GuiHelper.buildItemStack(config.bottomFill.material, config.bottomFill.name,
                                                        config.bottomFill.lore, config.bottomFill.itemModel));

                        gui.setContent(contentItems, player);
                        gui.open(player);
                });
        }

        private static String[] validateLayout(MemberManagementConfig config, LandClaimPlugin plugin) {
                return GuiLayoutValidator.validate(config.rows, config.layout,
                                Set.of("x", "B", "+", "<", "P", "N", "."), "P", "N",
                                "Resident", plugin);
        }

        private static int findSlot(String[] layout, String target) {
                return GuiLayoutValidator.findSlot(layout, target);
        }
}
