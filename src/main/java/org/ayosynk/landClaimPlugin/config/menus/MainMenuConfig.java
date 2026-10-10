package org.ayosynk.landClaimPlugin.config.menus;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.*;

import java.util.List;

@Header("MainMenu GUI Configuration")
@Header("Materials must be valid Bukkit Material enum names.")
@Header("Names and lore support MiniMessage formatting (e.g., <red>Test</red>, <gradient:blue:aqua>Gradient</gradient>)")
public class MainMenuConfig extends OkaeriConfig {

        public String title = "Claim: {claim_name}";

        @Comment("Inventory row count (1-6) and nine-slot-wide layout, top row first.")
        public int rows = 3;

        @Comment("Layout characters map to the item keys below; '.' leaves a slot empty.")
        public List<String> layout = List.of(
                        "1 1 2 2 2 2 2 1 1",
                        "1 . . S T V . . 1",
                        "2 2 2 2 2 2 2 2 2");

        @Comment("Filler 1")
        public ItemConfig filler1 = new ItemConfig("WHITE_STAINED_GLASS_PANE", " ", List.of());

        @Comment("Filler 2")
        public ItemConfig filler2 = new ItemConfig("GRAY_STAINED_GLASS_PANE", " ", List.of());

        @Comment("Claim Settings Button")
        public ItemConfig settings = new ItemConfig("EMERALD", "Settings", List.of());

        @Comment("Manage Residents, Trusted players, and Visitors")
        public ItemConfig trusted = new ItemConfig("COPPER_CHESTPLATE", "<gold>Manage", List.of());

        @Comment("Open profile flags (Visitor category first)")
        public ItemConfig visitors = new ItemConfig("SKULL_BANNER_PATTERN", "<yellow>Flags", List.of());

        public static class ItemConfig extends OkaeriConfig {
                public String material;
                public String name;
                public List<String> lore;
                @eu.okaeri.configs.annotation.CustomKey("item_model")
                public String itemModel;

                public ItemConfig() {
                } // For Okaeri to instantiate

                public ItemConfig(String material, String name, List<String> lore) {
                        this.material = material;
                        this.name = name;
                        this.lore = lore;
                }
        }
}
