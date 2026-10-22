# Warps

A lightweight, modern, and high-performance warp plugin built for **Paper** and **Folia** servers (Minecraft 1.21+).

---

### ✨ Features

* **Folia & Paper Ready:** Native region-threading support with asynchronous teleportation. Zero lag, completely thread-safe.
* **Interactive Chat List:** Running `/warps` displays a clean, clickable list in chat with hover previews. Click any warp name to teleport instantly.
* **Warmup & Cooldown:** Optional teleport delay with movement cancellation, plus cooldown timers between warps.
* **Post-Teleport Protection:** Brief damage invulnerability upon arrival so players don't die while chunks render.
* **Teleport Others:** Easily teleport other players to warps with `/warp <name> [player]`.
* **Smart Tab-Completion:** Fast auto-completion for all warp commands and online players.
* **MiniMessage Formatting:** Beautiful and fully customizable messages and colors via `config.yml`.

---

### 📋 Commands & Permissions

| Command | Description | Permission | Default |
|---|---|---|---|
| `/warp <name>` | Teleport to a warp | `warps.use` | `op` |
| `/warp <name> [player]` | Teleport another player to a warp | `warps.teleport.others` | `op` |
| `/setwarp <name>` | Create or update a warp at your location | `warps.set` | `op` |
| `/delwarp <name>` | Delete a warp | `warps.delete` | `op` |
| `/warps` | List all accessible warps | `warps.list` | `op` |
| `/warps reload` | Reload configuration and warps | `warps.reload` | `op` |

* `warps.admin`: Grants all permissions and bypasses.
* `warps.bypass.warmup`: Skips the teleport warmup delay.
* `warps.bypass.cooldown`: Skips the teleport cooldown timer.

---

### ⚙️ Configuration

All messages, warmup delays, cooldowns, and permissions can be adjusted in `config.yml`.
