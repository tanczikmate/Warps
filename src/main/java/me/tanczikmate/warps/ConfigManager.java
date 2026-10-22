package me.tanczikmate.warps;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class ConfigManager {

    private final Warps plugin;
    private boolean requirePermission;
    private boolean perWarpPermissions;
    private int teleportWarmup;
    private boolean cancelOnMove;
    private boolean cancelOnDamage;
    private int teleportCooldown;
    private int postTeleportProtection;

    public ConfigManager(Warps plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        requirePermission = config.getBoolean("settings.require-permission", false);
        perWarpPermissions = config.getBoolean("settings.per-warp-permissions", false);
        teleportWarmup = Math.max(0, config.getInt("settings.teleport-warmup", 0));
        cancelOnMove = config.getBoolean("settings.cancel-on-move", true);
        cancelOnDamage = config.getBoolean("settings.cancel-on-damage", false);
        teleportCooldown = Math.max(0, config.getInt("settings.teleport-cooldown", 0));
        postTeleportProtection = Math.max(0, config.getInt("settings.post-teleport-protection", 3));
    }

    public void sendMessage(CommandSender sender, String key, TagResolver... tags) {
        FileConfiguration config = plugin.getConfig();
        String prefix = config.getString("messages.prefix", "<gold>[Warps]</gold> ");
        String rawMessage = config.getString("messages." + key, "");

        if (rawMessage.isEmpty()) {
            return;
        }

        Component component = MiniMessage.miniMessage().deserialize(prefix + rawMessage, tags);
        sender.sendMessage(component);
    }

    public Component getComponent(String key, TagResolver... tags) {
        FileConfiguration config = plugin.getConfig();
        String rawMessage = config.getString("messages." + key, "");
        if (rawMessage.isEmpty()) {
            return Component.empty();
        }
        return MiniMessage.miniMessage().deserialize(rawMessage, tags);
    }

    public Component formatWarpListItem(Warp warp) {
        FileConfiguration config = plugin.getConfig();
        String raw = config.getString("messages.warp-list-item",
                "<click:run_command:'/warp <warp>'><hover:show_text:'<gray>Click to teleport to <white><warp></white></gray>'><yellow><warp></yellow></hover></click>");

        String safeWarpName = warp.name().replace("'", "\\'");
        String replaced = raw.replace("<warp>", safeWarpName);

        Component item = MiniMessage.miniMessage().deserialize(replaced);

        if (item.clickEvent() == null) {
            item = item.clickEvent(ClickEvent.runCommand("/warp " + warp.name()));
        }

        return item;
    }

    public boolean canAccessWarp(Player player, String warpName) {
        if (player.hasPermission(Warps.PERMISSION_ADMIN)) {
            return true;
        }

        if (requirePermission && !player.hasPermission(Warps.PERMISSION_USE)) {
            return false;
        }

        if (perWarpPermissions) {
            String lower = WarpUtils.normalizeKey(warpName);
            String stripped = WarpUtils.stripAccents(lower);

            boolean hasAccented = player.hasPermission("warps.warp." + lower);
            boolean hasStripped = !stripped.equals(lower) && player.hasPermission("warps.warp." + stripped);
            boolean hasWildcard = player.hasPermission("warps.warp.*");

            return hasAccented || hasStripped || hasWildcard;
        }

        return true;
    }

    public boolean canUseWarps(Player player) {
        if (player.hasPermission(Warps.PERMISSION_ADMIN)) {
            return true;
        }
        if (requirePermission) {
            return player.hasPermission(Warps.PERMISSION_USE);
        }
        return true;
    }

    public boolean canListWarps(Player player) {
        if (player.hasPermission(Warps.PERMISSION_ADMIN)) {
            return true;
        }
        if (requirePermission) {
            return player.hasPermission(Warps.PERMISSION_LIST) || player.hasPermission(Warps.PERMISSION_USE);
        }
        return true;
    }

    public boolean isRequirePermission() {
        return requirePermission;
    }

    public boolean isPerWarpPermissions() {
        return perWarpPermissions;
    }

    public int getTeleportWarmup() {
        return teleportWarmup;
    }

    public boolean isCancelOnMove() {
        return cancelOnMove;
    }

    public boolean isCancelOnDamage() {
        return cancelOnDamage;
    }

    public int getTeleportCooldown() {
        return teleportCooldown;
    }

    public int getPostTeleportProtection() {
        return postTeleportProtection;
    }
}
