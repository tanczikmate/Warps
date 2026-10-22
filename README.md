# Warps

**Warps** is a modern, lightweight, high-performance warp plugin built from the ground up for the **Paper** and **Folia** ecosystem (Minecraft 1.21 through 26.x).

---

### ✨ Features

* **Full Folia & Paper Compatibility:** Native region-threaded asynchronous teleportation (`player.teleportAsync`) and player-bound schedulers. Zero lag, zero cross-region locking.
* **Post-Teleport Damage Protection:** Configurable invulnerability period (e.g. 3 seconds) upon landing to prevent unfair deaths while chunks render. Breaks automatically if the player attacks an entity.
* **Teleport Other Players:** Administrative command support (`/warp <warp> [player]`) allowing admins and console to teleport other players.
* **Accented Characters Support:** Seamless support for Hungarian and international accented characters (e.g. `á`, `é`, `í`, `ó`, `ö`, `ő`, `ú`, `ü`, `ű`). Type `/warp főtér` or even `/warp foter` without accents!
* **Smart Tab-Completion:** Interactive auto-completion for `/warp`, `/setwarp`, and `/delwarp`. Suggests player names dynamically for the second argument if permitted.
* **Interactive MiniMessage Chat List:** `/warps` presents a stylish, clickable list with hover tooltips. Click any warp name in chat to teleport immediately!
* **Configurable Warmup & Cooldown:** Optional teleport delay and cooldown timer with movement cancellation (head rotation is ignored).
* **Flexible Permission System:** Configurable global permissions (`warps.use`) and per-warp permissions (`warps.warp.<name>`).
* **Asynchronous UTF-8 Storage:** Warps are persisted safely in the background to `warps.yml` without stalling server ticks.

---

### 📋 Commands & Permissions

| Command | Description | Permission | Default |
|---|---|---|---|
| `/warp <name>` | Teleports to the specified warp | `warps.use` (if required) / `warps.warp.<name>` | `op` |
| `/warp <name> [player]` | Teleports another player to the specified warp | `warps.teleport.others` | `op` |
| `/setwarp <name>` | Creates or updates a warp at your current location | `warps.set` | `op` |
| `/delwarp <name>` | Deletes an existing warp | `warps.delete` | `op` |
| `/warps` | Lists all accessible warps in chat | `warps.list` | `op` |
| `/warps reload` | Reloads configuration and warps | `warps.reload` | `op` |

* `warps.teleport.others`: Allows teleporting other players to a warp.
* `warps.bypass.warmup`: Bypasses the teleport delay.
* `warps.bypass.cooldown`: Bypasses the teleport cooldown timer.
* `warps.admin`: Grants full administrative control over all Warps features and bypasses.

---

### ⚙️ Configuration (`config.yml`)

```yaml
settings:
  # Whether a general permission ('warps.use') is required to use /warp and /warps.
  # If false, any player can use /warp and /warps without permissions.
  require-permission: false

  # Whether each warp requires an individual permission ('warps.warp.<warpname>').
  # If true, players can only teleport to and tab-complete warps they have permission for.
  per-warp-permissions: false

  # Teleport delay (warmup) in seconds. 0 to disable.
  teleport-warmup: 0

  # Cancel teleportation if the player moves during warmup
  cancel-on-move: true

  # Cancel teleportation if the player takes damage during warmup
  cancel-on-damage: false

  # Teleport cooldown in seconds. 0 to disable.
  teleport-cooldown: 0

  # Temporary damage protection in seconds after teleporting (e.g. 3). 0 to disable.
  post-teleport-protection: 3
```
