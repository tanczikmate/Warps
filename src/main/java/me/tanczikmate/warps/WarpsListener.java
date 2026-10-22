package me.tanczikmate.warps;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class WarpsListener implements Listener {

    private final Warps plugin;

    public WarpsListener(Warps plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!plugin.getConfigManager().isCancelOnMove()) {
            return;
        }

        Player player = event.getPlayer();
        TeleportManager.PendingTeleport pending = plugin.getTeleportManager().getPending(player.getUniqueId());
        if (pending == null) {
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return;
        }

        // Only cancel if actual block position changes, ignoring head rotation
        if (from.getBlockX() != to.getBlockX()
                || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ()) {
            plugin.getTeleportManager().cancelWarmup(player, "warmup-cancelled");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        // 1. Check if player has post-teleport invulnerability
        if (plugin.getProtectionManager().isProtected(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        // 2. Cancel warmup if configured
        if (plugin.getConfigManager().isCancelOnDamage()) {
            TeleportManager.PendingTeleport pending = plugin.getTeleportManager().getPending(player.getUniqueId());
            if (pending != null) {
                plugin.getTeleportManager().cancelWarmup(player, "warmup-cancelled-damage");
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        // If a protected player attacks, their invulnerability ends immediately to prevent PvP abuse
        if (event.getDamager() instanceof Player attacker) {
            if (plugin.getProtectionManager().isProtected(attacker.getUniqueId())) {
                plugin.getProtectionManager().removeProtection(attacker.getUniqueId());
                plugin.getConfigManager().sendMessage(attacker, "protection-ended-attack");
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getTeleportManager().removePlayer(event.getPlayer().getUniqueId());
    }
}
