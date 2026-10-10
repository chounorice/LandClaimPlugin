# Installation

## Requirements

- **Paper 26.2+** (or any Paper fork like Purpur)
- **Java 25+**

## Quick Start

1. Download the latest release from [GitHub Releases](https://github.com/synkfr/LandClaimPlugin/releases)
2. Drop `LandClaimPlugin.jar` into your server's `plugins/` folder
3. Restart your server
4. Configure `plugins/LandClaimPlugin/config.yml` to your liking

::: tip
The plugin generates all configuration files on first startup. Edit them and use `/claim admin reload` to apply changes without restarting.
:::

::: warning DEVELOPMENT BUILDS
Development builds are automatically generated from the latest commits and may include unfinished features or breaking changes. Use them only for testing.
:::

---

## Features

### Core Claiming
- **Claim Profiles** — Manage your land using claim profiles. Depending on server configuration, players can own a single profile or utilize the **Multi-Profile System** to manage multiple independent bases.
- **Chunk-Based Claims** — 16×16 block protection zones, simple and intuitive.
- **Auto-Claim** — Automatically claim chunks as you walk.
- **Connected Claims** — Optionally require claims to be adjacent (with diagonal support).
- **Profile Spawnpoint** — Set one spawnpoint per profile and allow authorized members to teleport to it.
- **Unstuck Command** — A safe `/claim unstuck` feature that teleports trapped players to the nearest safe wilderness block.

### Permission System
- **Role Categories** — Owner › Resident › Trusted › Visitor — the highest matching category decides
- **Configurable Flags** — Set separate Resident, Trusted, and Visitor permissions per profile
- **Trusted Players** — Grant Trusted access directly with `/claim trust add`
- **25+ Permission Flags** — Doors, trapdoors, containers, workstations, animals, vehicles, redstone, and more

### Social Systems
- **Resident System** — Invite players to join your claim as Residents and manage access

### Protection
- **Block Protection** — Prevent unauthorized breaking and placing
- **Entity Protection** — Protect animals, armor stands, and item frames
- **Explosion Protection** — Block TNT, creeper, and other explosion damage
- **Interaction Protection** — Control door, container, and workstation access
- **Item Protection** — Prevent unauthorized pickup and drop
- **Piston Protection** — Block pistons from pushing/pulling across claim borders
- **PvP Protection** — Disable PvP inside claims
- **Vehicle Protection** — Prevent unauthorized vehicle use and destruction
- **Command Blocking** — Block specific commands inside other players' claims

### Customization
- **Owner Alias** — Set a custom display name to replace your Minecraft username
- **Claim Colors** — Pick a custom hex color for your claim used on maps and borders
- **Visualization Modes** — Toggle between Display Entity or Particle-based boundary rendering
- **Entry/Exit Titles** — Custom MiniMessage-formatted titles shown when players enter or leave
- **Rename Claims** — Change your claim's display name at any time
- **Full Message Customization** — All plugin messages are configurable via YAML

---

## Supported Integrations

### Web Map Plugins
Visualize claims directly on your server's web map as colored polygons with owner info.
- **BlueMap** · **Dynmap** · **Squaremap** · **Pl3xMap**

### Combat Tagger Plugins
Prevent players from abusing claim commands while in combat.
- **DeluxeCombat** · **PvPManager** · **EternalCombat**

### WorldGuard
- **Volumetric Intersection Check** — 100% accurate 3D boundary checking
- **Gap Enforcement** — Configurable required distance between claims and WorldGuard regions
- **Custom Flag Support** — Apply the `allow-land-claims` flag to regions to explicitly allow claiming

---

## 📋 Disclosures & Privacy

### AI Assistance Disclosure
This project uses AI assistance (large language model tools) for code drafting, refactoring, and documentation under the direct architectural design, review, and validation of the maintainers.

### External Network Interactions
- **Modrinth API**: Asynchronously checks for new releases to notify operators. Can be disabled via `updateChecker.enabled: false`.
- **bStats Analytics**: Collects anonymous server statistics. Can be disabled in `plugins/bStats/config.yml`.
- **Redis / MySQL**: Connects only if configured by the server administrator for cross-server sync or external storage.
