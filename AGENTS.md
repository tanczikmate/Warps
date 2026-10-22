# AGENTS.md

Welcome to the **Warps** repository. This document provides contextual guidelines, architectural details, and operational instructions for AI agents working within this codebase.

---

## 1. Project Overview

**Warps** is a modern, lightweight, high-performance warp plugin built for the **Paper** and **Folia** ecosystem (supporting Minecraft **1.21** through **26.x**).

It enables players and administrators to create, manage, list, and teleport to warps with optional warmup/cooldowns, post-teleport damage protection, beautiful MiniMessage feedback, full Unicode/accented Hungarian character support, and native Folia region-threading compliance.

---

## 2. Technology Stack

* **Language:** Java 21 (Toolchain configured for Java 21, runtime compatible up to Java 25+)
* **Target Platforms:** Paper, Purpur, Folia (Minecraft 1.21+ up to 26.x)
* **Build System:** Gradle with Kotlin DSL (`build.gradle.kts`)
* **Libraries:**
  * `io.papermc.paper:paper-api` (Paper plugin API)
  * `net.kyori.adventure` (Adventure text and component library, MiniMessage)
  * `com.mojang:brigadier` (Brigadier command engine via Paper's lifecycle events)

---

## 3. Architecture & File Structure

```
Warps/
├── .github/
│   └── workflows/
│       └── build.yml                  # CI build and verification workflow
├── gradle/wrapper/                    # Gradle wrapper binaries & configuration
├── src/
│   └── main/
│       ├── java/me/tanczikmate/warps/
│       │   ├── Warps.java             # Main plugin class (onEnable, Brigadier setup)
│       │   ├── ConfigManager.java     # Loads & manages config.yml, MiniMessage formatting & permissions
│       │   ├── WarpManager.java       # Thread-safe storage, caching, and UTF-8 async persistence
│       │   ├── Warp.java              # Warp record model (name, location, serialization)
│       │   ├── WarpUtils.java         # Diacritics/accent stripping and Unicode key normalization
│       │   ├── ProtectionManager.java # Thread-safe post-teleport damage invulnerability tracker
│       │   ├── TeleportManager.java   # Folia-safe async teleportation, cooldowns, and warmup tasks
│       │   ├── WarpsListener.java     # PlayerMoveEvent and EntityDamageEvent warmup & protection listener
│       │   └── commands/
│       │       └── WarpsCommands.java # Brigadier command trees (/warp, /setwarp, /delwarp, /warps)
│       └── resources/
│           ├── config.yml             # Default configuration and localized messages
│           └── paper-plugin.yml       # Modern Paper plugin metadata & permission definitions
├── build.gradle.kts                   # Gradle build script (dependencies, versioning, copy task)
├── settings.gradle.kts                # Project root settings
├── gradle.properties                  # Project group, version & Gradle optimizations
├── local.properties                   # Local server plugin directory (git-ignored)
├── AGENTS.md                          # AI Agent guide (this file)
└── README.md                          # User-facing plugin documentation
```

---

## 4. Key Rules & Best Practices for Agents

1. **Do Not Commit Automatically:** Do not run `git commit` unless explicitly instructed by the user.
2. **Conventional Commits:** When committing, always follow Conventional Commits: `<type>(<scope>): <description>` (e.g. `feat:`, `fix:`, `docs:`, `refactor:`, `perf:`, `ci:`, `chore:`).
3. **Folia Compliance (Critical):**
   * **Never use BukkitScheduler:** Do NOT use `Bukkit.getScheduler()`, as Folia will throw an `UnsupportedOperationException`. Instead, use `player.getScheduler()` for player-bound tasks, `Bukkit.getRegionScheduler()` for block/chunk-bound tasks, or `Bukkit.getAsyncScheduler()` for non-world async operations.
   * **Asynchronous Teleportation:** Always use `player.teleportAsync(location)` rather than the synchronous `player.teleport(location)`.
   * **Thread Locality:** Do not access or mutate cross-region state synchronously. Never inspect blocks across worlds or regions synchronously.
4. **Unicode & Diacritics Support:** Support accented characters (e.g. Hungarian á, é, í, ó, ö, ő, ú, ü, ű) in warp names seamlessly using `StringArgumentType.greedyString()` and UTF-8 persistence.
5. **Adventure / MiniMessage:** Use MiniMessage exclusively for messages and formatting with `Placeholder.unparsed` for user-supplied input.
6. **Modern Command API:** Use Paper's `LifecycleEvents.COMMANDS` and Brigadier rather than legacy `plugin.yml` commands.
7. **Preserve Compatibility:** Avoid introducing APIs exclusive to minor subversions. Maintain compatibility across 1.21 - 26.x.
8. **Documentation Integrity:** Keep existing comments, JavaDocs, and metadata intact when updating code.

---

## 5. Build & Verification Commands

* **Compile Java:**
  ```powershell
  ./gradlew compileJava
  ```
* **Assemble & Build Plugin JAR (and deploy to local server if configured):**
  ```powershell
  ./gradlew build
  ```
