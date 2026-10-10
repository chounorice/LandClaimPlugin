package org.ayosynk.landClaimPlugin.config.menus;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.*;

import java.util.List;

@Header("PlayerControlPanel GUI Configuration")
@Header("Materials must be valid Bukkit Material enum names.")
@Header("Names and lore support MiniMessage formatting (e.g., <red>Test</red>, <gradient:blue:aqua>Gradient</gradient>)")
public class PlayerControlPanelConfig extends OkaeriConfig {

    public String title = "Player Control Panel";
    public int rows = 4;
    public List<String> layout = List.of(
            "F F F F F F F F F",
            "F F C F T F K F F",
            "F F F F F F F F F",
            ". . . X B B B . .");

    @Comment("Main UI framing")
    public ItemConfig frame = new ItemConfig("GRAY_STAINED_GLASS_PANE", " ", List.of());

    @Comment("Bottom separator")
    public ItemConfig accent = new ItemConfig("WHITE_STAINED_GLASS_PANE", " ", List.of());

    @Comment("Resident membership indicator")
    public ItemConfig changeRole = new ItemConfig(
            "PLAYER_HEAD",
            "<green>Resident",
            List.of("<gray>This player is a Resident of the profile."));

    @Comment("Transfer Ownership Button")
    public ItemConfig transferOwnership = new ItemConfig(
            "SKULL_BANNER_PATTERN",
            "<red><bold>Transfer Ownership",
            List.of("<gray>Transfer your claim ownership", "<gray>to this player."));

    @Comment("Kick Player Button")
    public ItemConfig kickPlayer = new ItemConfig(
            "BED",
            "<red>Kick <Player>",
            List.of("<gray>Remove this player from the claim."));

    @Comment("Ban Player Button — blocks entry to the claim entirely.")
    public ItemConfig banPlayer = new ItemConfig(
            "BARRIER",
            "<dark_red><bold>Ban <Player>",
            List.of("<gray>Block this player from entering", "<gray>your claim. Use /claim unban to reverse."));

    @Comment("Return to previous menu")
    public ItemConfig back = new ItemConfig(
            "SPECTRAL_ARROW",
            "<yellow>Back",
            List.of("<gray>Return to Members Management"));

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
