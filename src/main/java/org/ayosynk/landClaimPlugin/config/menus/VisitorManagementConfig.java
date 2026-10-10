package org.ayosynk.landClaimPlugin.config.menus;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.CustomKey;

import java.util.List;

public class VisitorManagementConfig extends OkaeriConfig {

    public String title = "Visitors Online";
    public int rows = 4;
    public List<String> layout = List.of(
            "x x x x x x x x x",
            "x x x x x x x x x",
            "x x x x x x x x x",
            "P B B B < B B B N");

    public ItemConfig filler = new ItemConfig("GRAY_STAINED_GLASS_PANE", " ", List.of());
    public ItemConfig back = new ItemConfig("ARROW", "<yellow>Back", List.of());
    public ItemConfig previousPage = new ItemConfig("ARROW", "<yellow>Previous", List.of());
    public ItemConfig nextPage = new ItemConfig("ARROW", "<yellow>Next", List.of());

    public static class ItemConfig extends OkaeriConfig {
        public String material;
        public String name;
        public List<String> lore;
        @CustomKey("item_model")
        public String itemModel;

        public ItemConfig() {
        }

        public ItemConfig(String material, String name, List<String> lore) {
            this.material = material;
            this.name = name;
            this.lore = lore;
        }
    }
}
