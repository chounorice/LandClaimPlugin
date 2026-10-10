package org.ayosynk.landClaimPlugin.gui;

import net.kyori.adventure.text.Component;
import org.ayosynk.landClaimPlugin.LandClaimPlugin;
import org.ayosynk.landClaimPlugin.config.menus.VisitorManagementConfig;
import org.ayosynk.landClaimPlugin.gui.framework.ClickAction;
import org.ayosynk.landClaimPlugin.gui.framework.GuiItem;
import org.ayosynk.landClaimPlugin.gui.framework.PaginatedGui;
import org.ayosynk.landClaimPlugin.gui.framework.SlotDefinition;
import org.ayosynk.landClaimPlugin.managers.PermissionResolver;
import org.ayosynk.landClaimPlugin.models.ClaimProfile;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Lists online players who currently have the implicit Visitor role in this profile. */
public final class VisitorManagementGUI {

    private VisitorManagementGUI() {
    }

    public static void open(Player player, ClaimProfile profile, LandClaimPlugin plugin) {
        VisitorManagementConfig config = plugin.getConfigManager().getVisitorManagementConfig();
        String[] structure = GuiLayoutValidator.validate(config.rows, config.layout,
                java.util.Set.of("x", "B", "<", "P", "N", "."), "P", "N",
                "Visitor", plugin);
        if (structure == null) {
            player.sendMessage(GuiHelper.MM.deserialize("<red>Visitor menu layout is invalid; check the plugin log."));
            return;
        }

        List<GuiItem> visitors = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (PermissionResolver.getPlayerStatus(profile, online.getUniqueId()).equals("visitor")) {
                visitors.add(createVisitorItem(online));
            }
        }

        Map<Character, SlotDefinition> ingredients = new HashMap<>();
        ingredients.put('B', GuiHelper.buildSlot(config.filler.material, config.filler.name,
                config.filler.lore, config.filler.itemModel));
        ingredients.put('<', GuiHelper.buildSlot(config.back.material, config.back.name, config.back.lore,
                config.back.itemModel,
                (p, e) -> ManageGUI.open(p, profile, plugin)));

        PaginatedGui gui = new PaginatedGui(
                GuiHelper.MM.deserialize(config.title), config.rows, structure, ingredients, 'x');
        ItemStack filler = GuiHelper.buildItemStack(config.filler.material, config.filler.name,
                config.filler.lore, config.filler.itemModel);
        gui.setPrevButton(GuiLayoutValidator.findSlot(structure, "P"),
                GuiHelper.buildItemStack(config.previousPage.material, config.previousPage.name,
                        config.previousPage.lore, config.previousPage.itemModel), filler);
        gui.setNextButton(GuiLayoutValidator.findSlot(structure, "N"),
                GuiHelper.buildItemStack(config.nextPage.material, config.nextPage.name,
                        config.nextPage.lore, config.nextPage.itemModel), filler);
        gui.setContent(visitors, player);
        gui.open(player);
    }

    private static GuiItem createVisitorItem(Player visitor) {
        return new GuiItem() {
            @Override
            public ItemStack render(Player viewer) {
                ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) head.getItemMeta();
                if (meta != null) {
                    meta.setOwningPlayer(visitor);
                    meta.displayName(GuiHelper.MM.deserialize("<yellow>" + visitor.getName()));
                    meta.lore(List.of(Component.empty(),
                            GuiHelper.MM.deserialize("<gray>Role: <white>Visitor")));
                    head.setItemMeta(meta);
                }
                return head;
            }

            @Override
            public ClickAction clickAction() {
                return (p, e) -> { };
            }
        };
    }
}
