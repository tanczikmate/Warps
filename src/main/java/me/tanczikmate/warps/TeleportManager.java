package me.tanczikmate.warps;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TeleportManager {

    public record PendingTeleport(
            String warpName,
            Location targetLocation,
            int startBlockX,
            int startBlockY,
            int startBlockZ,
            String startWorldName,
            ScheduledTask task
    ) {}

    private final Warps plugin;
    private final Map<UUID, PendingTeleport> pendingTeleports = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public TeleportManager(Warps plugin) {
        this.plugin = plugin;
    }

    public void teleport(Player player, Warp warp) {
        Location targetLocation = warp.toLocation();
        if (targetLocation == null) {
            plugin.getConfigManager().sendMessage(
                    player,
                    "world-not-loaded",
                    Placeholder.unparsed("warp", warp.name()),
                    Placeholder.unparsed("world", warp.worldName())
            );
            return;
        }

        UUID uuid = player.getUniqueId();
        ConfigManager config = plugin.getConfigManager();

        // 1. Check if already teleporting
        if (pendingTeleports.containsKey(uuid)) {
            config.sendMessage(player, "already-teleporting");
            return;
        }

        // 2. Cooldown check
        boolean bypassCooldown = player.hasPermission(Warps.PERMISSION_ADMIN)
                || player.hasPermission(Warps.PERMISSION_BYPASS_COOLDOWN);
        int cooldownSeconds = config.getTeleportCooldown();

        if (!bypassCooldown && cooldownSeconds > 0) {
            Long lastTeleportTime = cooldowns.get(uuid);
            if (lastTeleportTime != null) {
                long elapsed = System.currentTimeMillis() - lastTeleportTime;
                long requiredMillis = cooldownSeconds * 1000L;
                if (elapsed < requiredMillis) {
                    long remainingSeconds = (requiredMillis - elapsed + 999) / 1000;
                    config.sendMessage(
                            player,
                            "cooldown-active",
                            Placeholder.unparsed("seconds", String.valueOf(remainingSeconds))
                    );
                    return;
                }
            }
        }

        // 3. Warmup check
        boolean bypassWarmup = player.hasPermission(Warps.PERMISSION_ADMIN)
                || player.hasPermission(Warps.PERMISSION_BYPASS_WARMUP);
        int warmupSeconds = config.getTeleportWarmup();

        if (!bypassWarmup && warmupSeconds > 0) {
            Location currentLoc = player.getLocation();
            config.sendMessage(
                    player,
                    "warmup-started",
                    Placeholder.unparsed("seconds", String.valueOf(warmupSeconds)),
                    Placeholder.unparsed("warp", warp.name())
            );

            // Folia-safe player bound scheduler
            ScheduledTask task = player.getScheduler().runDelayed(plugin, scheduledTask -> {
                pendingTeleports.remove(uuid);
                if (player.isOnline()) {
                    executeTeleport(player, warp, targetLocation);
                }
            }, null, warmupSeconds * 20L);

            pendingTeleports.put(uuid, new PendingTeleport(
                    warp.name(),
                    targetLocation,
                    currentLoc.getBlockX(),
                    currentLoc.getBlockY(),
                    currentLoc.getBlockZ(),
                    currentLoc.getWorld() != null ? currentLoc.getWorld().getName() : "",
                    task
            ));
        } else {
            // Instant teleport
            executeTeleport(player, warp, targetLocation);
        }
    }

    /**
     * Teleports another player to a warp immediately (invoked by an admin or console).
     */
    public void teleportOther(CommandSender sender, Player target, Warp warp) {
        Location targetLocation = warp.toLocation();
        if (targetLocation == null) {
            plugin.getConfigManager().sendMessage(
                    sender,
                    "world-not-loaded",
                    Placeholder.unparsed("warp", warp.name()),
                    Placeholder.unparsed("world", warp.worldName())
            );
            return;
        }

        // Cancel any existing warmup for the target player
        cancelWarmup(target, null);

        // Execute teleport directly using native Folia/Paper async teleportation
        target.teleportAsync(targetLocation).thenAccept(success -> {
            if (Boolean.TRUE.equals(success)) {
                int protectionSeconds = plugin.getConfigManager().getPostTeleportProtection();
                if (protectionSeconds > 0) {
                    plugin.getProtectionManager().protect(target.getUniqueId(), protectionSeconds);
                }

                // Notify sender
                plugin.getConfigManager().sendMessage(
                        sender,
                        "teleported-other",
                        Placeholder.unparsed("target", target.getName()),
                        Placeholder.unparsed("warp", warp.name())
                );

                // Notify target
                String senderName = sender instanceof Player p ? p.getName() : "Console";
                plugin.getConfigManager().sendMessage(
                        target,
                        "teleported-by-other",
                        Placeholder.unparsed("sender", senderName),
                        Placeholder.unparsed("warp", warp.name())
                );
            }
        });
    }

    private void executeTeleport(Player player, Warp warp, Location targetLocation) {
        // Folia-safe asynchronous teleportation across any world or region
        player.teleportAsync(targetLocation).thenAccept(success -> {
            if (Boolean.TRUE.equals(success)) {
                cooldowns.put(player.getUniqueId(), System.currentTimeMillis());

                int protectionSeconds = plugin.getConfigManager().getPostTeleportProtection();
                if (protectionSeconds > 0) {
                    plugin.getProtectionManager().protect(player.getUniqueId(), protectionSeconds);
                }

                plugin.getConfigManager().sendMessage(
                        player,
                        "warp-success",
                        Placeholder.unparsed("warp", warp.name())
                );
            }
        });
    }

    public PendingTeleport getPending(UUID uuid) {
        return pendingTeleports.get(uuid);
    }

    public void cancelWarmup(Player player, String reasonMessageKey) {
        PendingTeleport pending = pendingTeleports.remove(player.getUniqueId());
        if (pending != null) {
            if (pending.task() != null) {
                pending.task().cancel();
            }
            if (reasonMessageKey != null) {
                plugin.getConfigManager().sendMessage(player, reasonMessageKey);
            }
        }
    }

    public void removePlayer(UUID uuid) {
        PendingTeleport pending = pendingTeleports.remove(uuid);
        if (pending != null && pending.task() != null) {
            pending.task().cancel();
        }
        cooldowns.remove(uuid);
        plugin.getProtectionManager().removeProtection(uuid);
    }
}
