# Database Documentation

## Supported Backends

| Backend | Class | Default |
|---------|-------|---------|
| SQLite | `SQLiteDatabase` | ✅ Yes |
| MySQL/MariaDB | `MySQLDatabase` | Optional |

Configuration: `config.yml` → `database.type` (`SQLITE` or `MYSQL`)

## Connection Pooling

MySQL connections use **HikariCP** with configurable pool settings:
- `maximumPoolSize` (default: 10)
- `minimumIdle` (default: 2)
- `connectionTimeout` (default: 30000ms)

## Schema

All tables use a configurable prefix (default: `lc_`).

### Role and spawnpoint migration

The role-category migration archives each pre-existing profile member assignment in `lc_profile_legacy_member_roles`, then rewrites the active `lc_profile_member_roles.role_name` as `Resident`. Previous custom-role names and per-role flags are not applied to the new categories; the new Resident category flags govern migrated members. The legacy `profile_roles` table is retained but ignored. Resident and Trusted permissions are stored in `lc_profile_category_flags`; `lc_profile_category_flag_sets` records initialized categories so an intentionally empty set remains empty after reload. One spawnpoint per profile is stored in `lc_profile_spawnpoints`.

Legacy per-player Trusted flags remain in `lc_profile_trusted_players` for compatibility but no longer grant permissions. Old warp/alliance rows are retained and are not rewritten during ordinary profile saves; they are only deleted when the associated profile itself is deleted. A single eligible legacy warp may be copied to the profile spawnpoint; multiple or out-of-claim warp locations are not selected automatically. Alliance rows no longer grant access.

### `lc_claim_profiles`

Primary table for claim profile metadata.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `owner_id` | VARCHAR(36) | PRIMARY KEY | Owner's UUID |
| `name` | VARCHAR(64) | NOT NULL | Display name |
| `claim_color` | VARCHAR(16) | NULLABLE | Hex color (e.g., `#00FF00`) |
| `vis_mode` | VARCHAR(32) | DEFAULT `'DISPLAY_ENTITY'` | Visualization mode |
| `title_enabled` | BOOLEAN | DEFAULT FALSE | Entry/exit title toggle |
| `enter_title` | VARCHAR(255) | NULLABLE | MiniMessage enter title |
| `leave_title` | VARCHAR(255) | NULLABLE | MiniMessage leave title |

### `lc_claimed_chunks`

Stores all claimed chunk positions.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `chunk_id` | VARCHAR(128) | PRIMARY KEY | Format: `world:x:z` |
| `owner_id` | VARCHAR(36) | NOT NULL, INDEX | FK → `claim_profiles.owner_id` |

### `lc_profile_roles`

Legacy custom-role definitions retained as stored data; current permissions use the role-category flag tables instead.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | VARCHAR(36) | PRIMARY KEY | Role UUID |
| `owner_id` | VARCHAR(36) | NOT NULL, INDEX | FK → `claim_profiles.owner_id` |
| `name` | VARCHAR(64) | NOT NULL | Role name (case-insensitive key) |
| `priority` | INT | NOT NULL | Lower = higher priority |
| `flags` | TEXT | NOT NULL | Comma-separated permission flags |

### `lc_profile_trusted_players`

Legacy per-player Trusted flag rows, retained but ignored by current permission resolution.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `owner_id` | VARCHAR(36) | NOT NULL | FK → `claim_profiles.owner_id` |
| `player_id` | VARCHAR(36) | NOT NULL | Trusted player's UUID |
| `flags` | TEXT | NOT NULL | Comma-separated permission flags |
| | | PRIMARY KEY (owner_id, player_id) | |

### `lc_profile_visitor_flags`

Visitor-category flags.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `owner_id` | VARCHAR(36) | NOT NULL | FK → `claim_profiles.owner_id` |
| `flag` | VARCHAR(64) | NOT NULL | Permission flag name |
| | | PRIMARY KEY (owner_id, flag) | |

### `lc_profile_member_roles`

Resident assignments within a profile (legacy role names are normalized during migration).

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `owner_id` | VARCHAR(36) | NOT NULL | FK → `claim_profiles.owner_id` |
| `player_id` | VARCHAR(36) | NOT NULL | Member's UUID |
| `role_name` | VARCHAR(64) | NOT NULL | Assigned role name |
| | | PRIMARY KEY (owner_id, player_id) | |

### `lc_profile_ally_flags`

Legacy alliance rows retained for data compatibility; they do not grant permissions.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `owner_id` | VARCHAR(36) | NOT NULL | FK → `claim_profiles.owner_id` |
| `ally_id` | VARCHAR(36) | NOT NULL | Allied profile owner's UUID |
| `flags` | TEXT | NOT NULL | Comma-separated permission flags |
| | | PRIMARY KEY (owner_id, ally_id) | |

### `lc_players`

Player preference data.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `uuid` | VARCHAR(36) | PRIMARY KEY | Player's UUID |
| `auto_claim` | BOOLEAN | DEFAULT FALSE | Auto-claim toggle |
| `auto_unclaim` | BOOLEAN | DEFAULT FALSE | Auto-unclaim toggle |
| `vis_mode` | VARCHAR(32) | DEFAULT `'DEFAULT'` | Preferred visualization |
| `bonus_claim_blocks` | INT | DEFAULT 0 | Bonus claim limit |
| `decay_exempt` | BOOLEAN | DEFAULT FALSE | Inactivity decay exemption toggle |

### `lc_warps`

Legacy warp points, read-only for migration to the profile spawnpoint.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `owner_id` | VARCHAR(36) | NOT NULL | Profile owner UUID |
| `name` | VARCHAR(64) | NOT NULL | Warp name (lowercase) |
| `world` | VARCHAR(64) | NOT NULL | World name |
| `x` | DOUBLE | NOT NULL | X coordinate |
| `y` | DOUBLE | NOT NULL | Y coordinate |
| `z` | DOUBLE | NOT NULL | Z coordinate |
| `yaw` | FLOAT | NOT NULL | Yaw rotation |
| `pitch` | FLOAT | NOT NULL | Pitch rotation |
| `icon` | VARCHAR(64) | NOT NULL | Material name for icon |
| | | PRIMARY KEY (owner_id, name) | |

### Legacy Tables (V1 Migration)

These tables exist for backward compatibility with the V1 → V2 migration:

| Table | Status |
|-------|--------|
| `lc_claims` | Deprecated — migrated to profiles |
| `lc_claim_chunks` | Deprecated — use `claimed_chunks` |
| `lc_claim_members` | Deprecated — use `profile_member_roles` |
| `lc_claim_trusted` | Deprecated — use `profile_trusted_players` |

## DAO Architecture

```
ProfileDao (interface)
    └── SQLProfileDao (implementation)

ClaimDao (interface, legacy)
    └── SQLClaimDao (implementation)

PlayerDao (interface)
    └── SQLPlayerDao (implementation)

WarpDao (interface)
    └── SQLWarpDao (read-only legacy migration)
```

All DAO operations return `CompletableFuture<T>` for async execution. The `DatabaseManager` routes to the correct implementation based on the configured database type.

## Save Strategy

`SQLProfileDao.saveProfile()` uses a **clear-and-reinsert** strategy within a transaction:

```
BEGIN TRANSACTION
  1. UPSERT claim_profiles row
  2. DELETE FROM claimed_chunks WHERE owner_id = ?
  3. INSERT claimed_chunks (batch)
  4. DELETE FROM profile_visitor_flags WHERE owner_id = ?
  5. INSERT profile_visitor_flags (batch)
  6. DELETE FROM profile_trusted_players WHERE owner_id = ?
  7. INSERT profile_trusted_players (batch)
  8. DELETE FROM profile_roles WHERE owner_id = ?
  9. INSERT profile_roles (batch)
  10. DELETE FROM profile_member_roles WHERE owner_id = ?
  11. INSERT profile_member_roles (batch)
COMMIT
```

Legacy alliance and warp rows are deliberately not changed by profile saves. This approach is simple and ensures consistency, at the cost of more writes. For profiles with many chunks/members, consider batching optimizations in future versions.

## Cross-Server Sync (Redis)

When Redis is enabled, cache invalidation messages are published after DB writes:

```
Publish → landclaim:sync → "INVALIDATE_PROFILE:<owner_uuid>"
```

All servers subscribe and invalidate their local Caffeine cache on receipt.
