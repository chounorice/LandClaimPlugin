package org.ayosynk.landClaimPlugin.config.menus;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.CustomKey;

import java.util.List;

public class ManageMenuConfig extends OkaeriConfig {

    public String title = "Manage Claim Roles";
    public int rows = 3;
    public List<String> layout = List.of(
            "F F F F F F F F F",
            "F F R F T F V F F",
            "F F F F B F F F F");

    public ItemConfig filler = new ItemConfig("GRAY_STAINED_GLASS_PANE", " ", List.of());
    public ItemConfig residents = new ItemConfig("PLAYER_HEAD", "<green>Residents",
            List.of("<gray>Manage Resident membership"));
    public ItemConfig trusted = new ItemConfig("NAME_TAG", "<gold>Trusted",
            List.of("<gray>Manage Trusted membership"));
    public ItemConfig visitors = new ItemConfig("OAK_DOOR", "<yellow>Visitors",
            List.of("<gray>View online players with the Visitor role"));
    public ItemConfig back = new ItemConfig("SPECTRAL_ARROW", "<yellow>Back",
            List.of("<gray>Return to claim menu"));

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
