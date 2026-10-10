package org.ayosynk.landClaimPlugin.gui;

import org.ayosynk.landClaimPlugin.LandClaimPlugin;

import java.util.List;
import java.util.Set;

final class GuiLayoutValidator {

    private GuiLayoutValidator() {
    }

    static String[] validate(int rows, List<String> layout, Set<String> allowedSlots,
            String previousSlot, String nextSlot, String menuName, LandClaimPlugin plugin) {
        if (rows < 2 || rows > 6 || layout == null || layout.size() != rows) {
            plugin.getLogger().severe("Invalid " + menuName + " menu layout: rows must be 2-6 and match the layout list.");
            return null;
        }

        int previousCount = 0;
        int nextCount = 0;
        int contentCount = 0;
        for (int row = 0; row < layout.size(); row++) {
            String line = layout.get(row);
            String[] slots = line == null || line.isBlank() ? new String[0] : line.trim().split("\\s+");
            if (slots.length != 9) {
                plugin.getLogger().severe("Invalid " + menuName + " menu layout row " + (row + 1)
                        + ": each row must contain exactly nine space-separated slots.");
                return null;
            }
            for (String slot : slots) {
                if (!allowedSlots.contains(slot)) {
                    plugin.getLogger().severe("Invalid " + menuName + " menu layout slot '" + slot
                            + "' in row " + (row + 1) + ".");
                    return null;
                }
                if (previousSlot != null && slot.equals(previousSlot)) previousCount++;
                if (nextSlot != null && slot.equals(nextSlot)) nextCount++;
                if (slot.equals("x")) contentCount++;
            }
        }
        boolean paginated = previousSlot != null || nextSlot != null;
        if (paginated && (previousCount != 1 || nextCount != 1 || contentCount == 0)) {
            plugin.getLogger().severe("Invalid " + menuName + " menu layout: include one or more x content slots "
                    + "and exactly one " + previousSlot + " and one " + nextSlot + " navigation slot.");
            return null;
        }
        return layout.toArray(new String[0]);
    }

    static int findSlot(String[] layout, String target) {
        for (int row = 0; row < layout.length; row++) {
            String[] slots = layout[row].trim().split("\\s+");
            for (int column = 0; column < slots.length; column++) {
                if (slots[column].equals(target)) return row * 9 + column;
            }
        }
        throw new IllegalStateException("Validated GUI layout is missing slot " + target);
    }
}
