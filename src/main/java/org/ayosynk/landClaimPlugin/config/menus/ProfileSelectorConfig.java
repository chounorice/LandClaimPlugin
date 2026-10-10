package org.ayosynk.landClaimPlugin.config.menus;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.*;

import java.util.List;

@Header("ProfileSelector GUI Configuration")
@Header("Materials must be valid Bukkit Material enum names.")
@Header("Names and lore support MiniMessage formatting.")
public class ProfileSelectorConfig extends OkaeriConfig {

    public String title = "<dark_gray>Select Claim Profile";
    public int rows = 5;
    public List<String> layout = List.of(
            "F F F F F F F F F",
            "F x x x x x x x F",
            "F x x x x x x x F",
            "F x x x x x x x F",
            "F F F P C N F F F");

    @Comment("The material used for a profile that is NOT active.")
    public String inactiveProfileMaterial = "BOOK";
    @CustomKey("inactive_profile_item_model")
    public String inactiveProfileItemModel;

    @Comment("The material used for the active profile.")
    public String activeProfileMaterial = "ENCHANTED_BOOK";
    @CustomKey("active_profile_item_model")
    public String activeProfileItemModel;

    @Comment("Format for the profile item name. Variables: <name>")
    public String profileNameFormat = "<!italic><gold><bold><name>";

    @Comment("Lore added to the profile item if you are the Owner.")
    public String roleOwnerLore = "<!italic><gray>Role: <yellow>Owner";

    @Comment("Lore added to the profile item if you are a Member. Variables: <role>")
    public String roleMemberLore = "<!italic><gray>Role: <yellow><role>";

    @Comment("Lore showing the chunk count. Variables: <count>")
    public String chunksClaimedLore = "<!italic><gray>Chunks Claimed: <yellow><count>";

    @Comment("Lore added if the profile is currently active.")
    public String activeProfileLore = "<!italic><green>Currently Active Profile";

    @Comment("Lore added if the profile is NOT active.")
    public String clickToActivateLore = "<!italic><yellow>Click to set as Active Profile";

    @Comment("Filler item used in the background.")
    public ItemConfig filler = new ItemConfig("GRAY_STAINED_GLASS_PANE", "<gray> ", List.of());

    @Comment("Close button.")
    public ItemConfig close = new ItemConfig("BARRIER", "<red>Close", List.of());

    @Comment("Previous Page button.")
    public ItemConfig previousPage = new ItemConfig("ARROW", "<green>Previous Page", List.of());

    @Comment("Next Page button.")
    public ItemConfig nextPage = new ItemConfig("ARROW", "<green>Next Page", List.of());

    public static class ItemConfig extends OkaeriConfig {
        public String material;
        public String name;
        public List<String> lore;
        @CustomKey("item_model")
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
