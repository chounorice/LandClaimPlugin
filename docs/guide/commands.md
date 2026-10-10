# Commands

All commands use `/claim` (alias: `/c`) as the base.

## General Commands

| Command | Description |
|---|---|
| `/claim` | Claim the chunk you're standing in |
| `/claim radius <1-5>` | Claim a square area of chunks centered on your current chunk (e.g. `1` = 3×3, `2` = 5×5) |
| `/claim profiles` | Open the active profile selector (Multi-Profile mode) |
| `/claim create <name>` | Create a new claim profile with the given name |
| `/claim auto` | Toggle auto-claim mode (claim chunks as you walk) |
| `/claim menu` | Open the main claim management GUI |
| `/claim info` | View info about the claim at your location |
| `/claim visible` | Toggle claim boundary visualization |
| `/claim toggle <mode>` | Switch visualization mode (`display_entities`, `particles`, `off`) |
| `/claim visualization <mode>` | Alias for `/claim toggle` |
| `/claim notify` | Toggle claim enter and leave chat notifications (alias: `/claim chatnotify`) |
| `/claim rename <name>` | Rename your active claim profile (3-32 alphanumeric characters) |
| `/claim color <color>` | Change claim color using a named color (e.g., `red`, `lime`) or hex code (e.g., `#FF5500`) |
| `/claim unstuck` | Safely teleport to the nearest wilderness block |
| `/claim abandon` | Delete your entire active claim profile and all its chunks |
| `/claim pvp <on/off> [time]` | Toggle PvP in the claim, with optional duration in seconds |
| `/claim spawnpoint [remove]` | Set or remove this profile's spawnpoint |
| `/claim tp <owner> <claim>` | Teleport to an authorized profile spawnpoint |
| `/claim buy claim [amount]` | Buy extra claim blocks |
| `/claim buy member` | Buy an additional member slot for your active profile |
| `/claim sell <profile> <price>` | List a claim profile on the server-wide marketplace |
| `/unclaim` | Unclaim the chunk you're standing in |
| `/unclaim auto` | Toggle auto-unclaim mode (unclaim your owned chunks as you walk) |
| `/unclaim radius <1-5>` | Unclaim all your owned chunks in a square radius (e.g. `1` = 3×3, `2` = 5×5) |
| `/unclaim all` | Unclaim all chunks belonging to your active profile (prompts for confirmation) |
| `/unclaim all confirm` | Confirm unclaiming all chunks |

## Menu Shortcuts

Jump directly to specific GUI panels without navigating through the main menu.
These commands respect the same permission checks as clicking the GUI buttons.

| Command | Permission Required | Description |
|---|---|---|
| `/claim menu settings` | `MANAGE_SETTINGS` | Open claim settings (color, PvP, visibility, titles) |
| `/claim menu manage` | `MANAGE_MEMBERS` | Open Resident, Trusted, and Visitor management |
| `/claim menu flags` | `MANAGE_SETTINGS` | Open profile flags (Visitor category by default) |

## Member Commands

| Command | Description |
|---|---|
| `/claim member invite <player>` | Invite a player to join your claim |
| `/claim member kick <player>` | Remove a member from your claim |
| `/claim member list` | List all members and their roles |
| `/claim accept <name>` | Accept a pending member invitation |
| `/claim deny <name>` | Deny a pending member invitation |
| `/claim leave <claim name>` | Leave a claim you are a member or trusted player of |

## Trust Commands

| Command | Description |
|---|---|
| `/claim trust add <player>` | Grant Trusted access directly |
| `/claim trust remove <player>` | Remove a trusted player |
| `/claim trust list` | List all trusted players and their flags |

## Ban Commands

| Command | Description |
|---|---|
| `/claim ban <player>` | Ban a player from your active claim. Banned players lose every flag, are blocked from entering any of the claim's chunks, and (if online) are teleported outside. Use `/claim unban` to reverse. |
| `/claim unban <player>` | Remove a player's ban from your active claim. |
| `/claim banlist` | List all players currently banned from your active claim. |

::: tip Ban vs. Kick
`/claim member kick` removes a player from the member list but still allows them to enter as a visitor. `/claim ban` is a hard denial — the player cannot enter the claim at all, even as a visitor. Use ban for players who have been harassing other members.
:::

::: note Bedrock players
Ban confirmations, abandon confirmations, unclaim-all confirmations, and AnvilInputGUI text prompts all send **native Bedrock forms** to Bedrock players when Geyser 2.x is installed, instead of the Java-only chat-prompt fallback.
:::

## Admin Commands

| Command | Description |
|---|---|
| `/claim admin claim` | Claim the current chunk for the global Admin Profile (Server Land) |
| `/claim admin menu` | Open the management menu for the global Admin Profile |
| `/claim admin edit <player>` | Open any player's claim management GUI with full override (supports `@p`, `@s`, `@r`) |
| `/claim admin check` | View detailed claim info (owner UUID, profile name) |
| `/claim admin unclaim` | Force-unclaim the chunk you're standing in |
| `/claim admin add chunk <player> <amount>` | Add bonus claim chunks to a player's limit *(Console & In-Game)*. Supports selectors (`@p`, `@s`, `@r`, `@a`), reverse ordering (`<amount> <player>`), and self-granting (`<amount>`). See [Integrations Guide](/guide/integrations) |
| `/claim admin set chunk <player> <amount>` | Set a player's bonus claim chunks directly *(Console & In-Game, supports selectors, reverse ordering, and self-targeting)* |
| `/claim admin remove chunk <player> <amount>` | Deduct bonus claim chunks from a player *(Console & In-Game, supports selectors, reverse ordering, and self-targeting)* |
| `/claim admin reset chunk [player]` | Reset bonus claim chunks back to 0 *(Console & In-Game, supports selectors, defaults to self for in-game players)* |
| `/claim admin setalias <claim> <alias>` | Set or reset an owner's custom alias *(Console & In-Game)* |
| `/claim admin trust list <owner>` | List players trusted by this owner *(Console & In-Game, supports selectors)* |
| `/claim admin trust who <player>` | List claims where this player is trusted *(Console & In-Game, supports selectors)* |
| `/claim admin decay run` | Trigger a manual scan to decay inactive claims *(Console & In-Game)* |
| `/claim admin decay exempt <player> [true\|false]` | Toggle or set a player's exemption from claim decay *(Console & In-Game, supports selectors)* |
| `/claim admin decay list` | List all players manually exempt from claim decay *(Console & In-Game)* |
| `/claim admin reload` | Reload the plugin configuration and messages *(Console & In-Game)* |
| `/claim reload` | Reload the plugin configuration and messages (Root Shortcut, Console & In-Game) |

::: tip Bonus Chunks & Shop Integrations
The `/claim admin add chunk` command is fully compatible with **DeluxeMenus** and **Citizens 2** NPCs for creating chunk-purchasing shops. It accepts `@p` and `<p>` interchangeably. For complete configuration examples, see the [Integrations Guide](/guide/integrations).
:::

## Permissions

| Permission | Description | Default |
|---|---|---|
| `landclaim.*` | All LandClaim permissions | `false` |
| `landclaim.claim` | Basic claiming ability | `true` |
| `landclaim.auto` | Use auto-claim mode | `true` |
| `landclaim.admin` | Admin commands and bypass all protection | `op` |
| `landclaim.decay.exempt` | Exempts player from automatic claim decay | `op` |
| `landclaim.update.notify` | Receive update notifications on join | `op` |
| `landclaim.unstuck` | Teleport to a safe wilderness location | `true` |
| `landclaim.unclaim` | Unclaim the current chunk | `true` |
| `landclaim.member` | Access to member subcommands | `true` |
| `landclaim.trust` | Access to trust subcommands | `true` |
| `landclaim.ban` | Ban / unban players from your claim | `true` |
| `landclaim.abandon` | Abandon active claim profile | `true` |
| `landclaim.create` | Create new claim profiles | `true` |
| `landclaim.visible` | Toggle boundary visibility | `true` |
| `landclaim.toggle` | Switch visualization mode | `true` |
| `landclaim.info` | View claim information | `true` |
| `landclaim.pvp` | Toggle PvP state | `true` |
| `landclaim.rename` | Rename claims | `true` |
| `landclaim.color` | Change claim colors | `true` |
| `landclaim.unclaimall` | Unclaim all land | `true` |
| `landclaim.leave` | Leave a claim | `true` |
| `landclaim.menu.*` | Access to all GUI menus | `true` |
| `landclaim.menu.<menu>` | Access to a specific GUI menu (e.g. `main`, `manage`, `flags`, `members`, `trusted`) | `true` |
| `landclaim.limit.<n>` | Override the chunk claim limit | `false` |
| `landclaim.list` | List claims | `true` |
| `landclaim.trust.limit.<n>` | Set a Trusted-player cap for a profile (default cap: 5) | `false` |
| `landclaim.trust.limit.<n>` | Set a Trusted-player cap for a profile (default cap: 5) | `false` |
