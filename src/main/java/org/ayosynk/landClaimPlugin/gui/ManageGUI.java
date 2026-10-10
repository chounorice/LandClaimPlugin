package org.ayosynk.landClaimPlugin.gui;

import org.ayosynk.landClaimPlugin.LandClaimPlugin;
import org.ayosynk.landClaimPlugin.config.menus.ManageMenuConfig;
import org.ayosynk.landClaimPlugin.gui.framework.CustomGui;
import org.ayosynk.landClaimPlugin.gui.framework.SlotDefinition;
import org.ayosynk.landClaimPlugin.models.ClaimProfile;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Entry point for managing the three profile role groups. */
public final class ManageGUI {

    private ManageGUI() {
    }

    public static void open(Player player, ClaimProfile profile, LandClaimPlugin plugin) {
        ManageMenuConfig config = plugin.getConfigManager().getManageMenuConfig();
        String[] structure = validateLayout(config, plugin);
        if (structure == null) {
            return;
        }
        Map<Character, SlotDefinition> items = new HashMap<>();
        items.put('F', GuiHelper.buildSlot(config.filler.material, config.filler.name,
                config.filler.lore, config.filler.itemModel));
        items.put('R', GuiHelper.buildSlot(config.residents.material, config.residents.name,
                config.residents.lore, config.residents.itemModel, (p, e) ->
                        MemberManagementGUI.open(p, profile, plugin)));
        items.put('T', GuiHelper.buildSlot(config.trusted.material, config.trusted.name,
                config.trusted.lore, config.trusted.itemModel, (p, e) ->
                        TrustManagementGUI.open(p, profile, plugin)));
        items.put('V', GuiHelper.buildSlot(config.visitors.material, config.visitors.name,
                config.visitors.lore, config.visitors.itemModel, (p, e) ->
                        VisitorManagementGUI.open(p, profile, plugin)));
        items.put('B', GuiHelper.buildSlot(config.back.material, config.back.name,
                config.back.lore, config.back.itemModel, (p, e) ->
                        MainMenuGUI.open(p, profile, plugin)));

        CustomGui gui = new CustomGui(GuiHelper.MM.deserialize(config.title), config.rows);
        gui.fillFromStructure(structure, items);
        gui.open(player);
    }

    private static String[] validateLayout(ManageMenuConfig config, LandClaimPlugin plugin) {
        Set<String> allowedSlots = Set.of("F", "R", "T", "V", "B");
        if (config.rows < 1 || config.rows > 6 || config.layout == null
                || config.layout.size() != config.rows) {
            plugin.getLogger().severe("Invalid Manage menu layout: rows must be 1-6 and match the layout list.");
            return null;
        }
        for (int row = 0; row < config.layout.size(); row++) {
            String line = config.layout.get(row);
            if (line == null || line.trim().isEmpty() || line.trim().split("\\s+").length != 9) {
                plugin.getLogger().severe("Invalid Manage menu layout row " + (row + 1)
                        + ": each row must contain exactly nine space-separated slots.");
                return null;
            }
            for (String slot : line.trim().split("\\s+")) {
                if (!allowedSlots.contains(slot)) {
                    plugin.getLogger().severe("Invalid Manage menu layout slot '" + slot
                            + "' in row " + (row + 1) + ".");
                    return null;
                }
            }
        }
        return config.layout.toArray(new String[0]);
    }
}
