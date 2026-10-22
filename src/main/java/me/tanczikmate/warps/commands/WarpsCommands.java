package me.tanczikmate.warps.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import me.tanczikmate.warps.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class WarpsCommands {

    private final Warps plugin;
    private final ConfigManager configManager;
    private final WarpManager warpManager;
    private final TeleportManager teleportManager;

    public WarpsCommands(Warps plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.warpManager = plugin.getWarpManager();
        this.teleportManager = plugin.getTeleportManager();
    }

    public void register(Commands commands) {
        registerWarpCommand(commands);
        registerSetWarpCommand(commands);
        registerDelWarpCommand(commands);
        registerWarpsCommand(commands);
    }

    private void registerWarpCommand(Commands commands) {
        var node = Commands.literal("warp")
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    if (!(sender instanceof Player player)) {
                        configManager.sendMessage(sender, "usage-warp-console");
                        return 1;
                    }
                    if (!configManager.canUseWarps(player)) {
                        configManager.sendMessage(player, "no-permission");
                        return 1;
                    }
                    configManager.sendMessage(player, "usage-warp");
                    return 1;
                })
                .then(Commands.argument("warp", StringArgumentType.greedyString())
                        .suggests((ctx, builder) -> suggestAccessibleWarps(ctx, builder))
                        .executes(ctx -> {
                            CommandSender sender = ctx.getSource().getSender();
                            String input = ctx.getArgument("warp", String.class).trim();
                            handleWarpCommand(sender, input);
                            return 1;
                        })
                );

        commands.register(node.build(), "Teleport to a warp or teleport other players");
    }

    private void registerSetWarpCommand(Commands commands) {
        var node = Commands.literal("setwarp")
                .requires(source -> source.getSender().hasPermission(Warps.PERMISSION_SET)
                        || source.getSender().hasPermission(Warps.PERMISSION_ADMIN))
                .executes(ctx -> {
                    configManager.sendMessage(ctx.getSource().getSender(), "usage-setwarp");
                    return 1;
                })
                .then(Commands.argument("warp", StringArgumentType.greedyString())
                        .suggests((ctx, builder) -> suggestAllWarps(builder))
                        .executes(ctx -> {
                            CommandSender sender = ctx.getSource().getSender();
                            if (!(sender instanceof Player player)) {
                                configManager.sendMessage(sender, "only-players");
                                return 1;
                            }
                            String warpName = ctx.getArgument("warp", String.class).trim();
                            handleSetWarp(player, warpName);
                            return 1;
                        })
                );

        commands.register(node.build(), "Set a warp location");
    }

    private void registerDelWarpCommand(Commands commands) {
        var node = Commands.literal("delwarp")
                .requires(source -> source.getSender().hasPermission(Warps.PERMISSION_DELETE)
                        || source.getSender().hasPermission(Warps.PERMISSION_ADMIN))
                .executes(ctx -> {
                    configManager.sendMessage(ctx.getSource().getSender(), "usage-delwarp");
                    return 1;
                })
                .then(Commands.argument("warp", StringArgumentType.greedyString())
                        .suggests((ctx, builder) -> suggestAllWarps(builder))
                        .executes(ctx -> {
                            CommandSender sender = ctx.getSource().getSender();
                            String warpName = ctx.getArgument("warp", String.class).trim();
                            handleDelWarp(sender, warpName);
                            return 1;
                        })
                );

        commands.register(node.build(), "Delete a warp");
    }

    private void registerWarpsCommand(Commands commands) {
        var node = Commands.literal("warps")
                .executes(ctx -> {
                    handleListWarps(ctx.getSource().getSender());
                    return 1;
                })
                .then(Commands.literal("reload")
                        .requires(source -> source.getSender().hasPermission(Warps.PERMISSION_RELOAD)
                                || source.getSender().hasPermission(Warps.PERMISSION_ADMIN))
                        .executes(ctx -> {
                            configManager.loadConfig();
                            warpManager.loadWarps();
                            configManager.sendMessage(ctx.getSource().getSender(), "reload-success");
                            return 1;
                        })
                );

        commands.register(node.build(), "List or manage warps");
    }

    private void handleWarpCommand(CommandSender sender, String input) {
        if (input.isEmpty()) {
            if (sender instanceof Player player) {
                configManager.sendMessage(player, "usage-warp");
            } else {
                configManager.sendMessage(sender, "usage-warp-console");
            }
            return;
        }

        // Check if there is a target player argument: "/warp <warp> <player>"
        int lastSpace = input.lastIndexOf(' ');
        if (lastSpace != -1) {
            String potentialWarpName = input.substring(0, lastSpace).trim();
            String potentialTargetName = input.substring(lastSpace + 1).trim();

            Warp warp = warpManager.getWarp(potentialWarpName);
            if (warp != null) {
                boolean canTeleportOthers = sender.hasPermission(Warps.PERMISSION_TELEPORT_OTHERS)
                        || sender.hasPermission(Warps.PERMISSION_ADMIN);

                if (!canTeleportOthers) {
                    configManager.sendMessage(sender, "no-permission");
                    return;
                }

                Player target = plugin.getServer().getPlayerExact(potentialTargetName);
                if (target == null) {
                    configManager.sendMessage(sender, "player-not-found",
                            Placeholder.unparsed("target", potentialTargetName));
                    return;
                }

                teleportManager.teleportOther(sender, target, warp);
                return;
            }
        }

        // Self teleport
        if (!(sender instanceof Player player)) {
            configManager.sendMessage(sender, "usage-warp-console");
            return;
        }

        if (!configManager.canUseWarps(player)) {
            configManager.sendMessage(player, "no-permission");
            return;
        }

        Warp warp = warpManager.getWarp(input);
        if (warp == null) {
            configManager.sendMessage(player, "warp-not-found", Placeholder.unparsed("warp", input));
            return;
        }

        if (!configManager.canAccessWarp(player, warp.name())) {
            configManager.sendMessage(player, "no-permission-warp", Placeholder.unparsed("warp", warp.name()));
            return;
        }

        teleportManager.teleport(player, warp);
    }

    private void handleSetWarp(Player player, String warpName) {
        if (warpName.isEmpty()) {
            configManager.sendMessage(player, "usage-setwarp");
            return;
        }

        // Validate illegal characters that could break parsing or YAML
        if (warpName.contains("<") || warpName.contains(">") || warpName.contains("\"")
                || warpName.contains("'") || warpName.contains(":") || warpName.contains(".")) {
            configManager.sendMessage(player, "invalid-warp-name");
            return;
        }

        Warp warp = Warp.fromLocation(warpName, player.getLocation());
        warpManager.setWarp(warp);
        configManager.sendMessage(player, "warp-set", Placeholder.unparsed("warp", warp.name()));
    }

    private void handleDelWarp(CommandSender sender, String warpName) {
        if (warpName.isEmpty()) {
            configManager.sendMessage(sender, "usage-delwarp");
            return;
        }

        Warp warp = warpManager.getWarp(warpName);
        if (warp == null) {
            configManager.sendMessage(sender, "warp-not-found", Placeholder.unparsed("warp", warpName));
            return;
        }

        warpManager.deleteWarp(warp.name());
        configManager.sendMessage(sender, "warp-deleted", Placeholder.unparsed("warp", warp.name()));
    }

    private void handleListWarps(CommandSender sender) {
        if (sender instanceof Player player && !configManager.canListWarps(player)) {
            configManager.sendMessage(player, "no-permission");
            return;
        }

        List<Warp> available;
        if (sender instanceof Player player) {
            available = warpManager.getAccessibleWarps(player, configManager);
        } else {
            available = new ArrayList<>(warpManager.getAllWarps());
            available.sort(Comparator.comparing(w -> WarpUtils.normalizeKey(w.name())));
        }

        if (available.isEmpty()) {
            configManager.sendMessage(sender, "warp-list-empty");
            return;
        }

        configManager.sendMessage(sender, "warp-list-header",
                Placeholder.unparsed("count", String.valueOf(available.size())));

        Component separator = configManager.getComponent("warp-list-separator");
        Component listComponent = Component.empty();

        for (int i = 0; i < available.size(); i++) {
            Warp warp = available.get(i);
            Component item = configManager.formatWarpListItem(warp);

            if (i > 0) {
                listComponent = listComponent.append(separator);
            }
            listComponent = listComponent.append(item);
        }

        sender.sendMessage(listComponent);
    }

    private CompletableFuture<Suggestions> suggestAccessibleWarps(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        CommandSender sender = ctx.getSource().getSender();

        if (sender instanceof Player player && !configManager.canUseWarps(player)
                && !player.hasPermission(Warps.PERMISSION_TELEPORT_OTHERS)
                && !player.hasPermission(Warps.PERMISSION_ADMIN)) {
            return builder.buildFuture();
        }

        String input = builder.getRemaining();

        int lastSpace = input.lastIndexOf(' ');
        if (lastSpace != -1) {
            boolean canTeleportOthers = sender.hasPermission(Warps.PERMISSION_TELEPORT_OTHERS)
                    || sender.hasPermission(Warps.PERMISSION_ADMIN);

            if (canTeleportOthers) {
                String warpPart = input.substring(0, lastSpace).trim();
                Warp warp = warpManager.getWarp(warpPart);
                if (warp != null) {
                    String playerPart = input.substring(lastSpace + 1).toLowerCase(Locale.ROOT);
                    for (Player online : plugin.getServer().getOnlinePlayers()) {
                        if (online.getName().toLowerCase(Locale.ROOT).startsWith(playerPart)) {
                            builder.suggest(warp.name() + " " + online.getName());
                        }
                    }
                    return builder.buildFuture();
                }
            }
        }

        String remainingLower = WarpUtils.normalizeKey(input);
        String remainingStripped = WarpUtils.stripAccents(remainingLower);

        List<Warp> available;
        if (sender instanceof Player player) {
            available = warpManager.getAccessibleWarps(player, configManager);
        } else {
            available = new ArrayList<>(warpManager.getAllWarps());
        }

        for (Warp warp : available) {
            String warpLower = WarpUtils.normalizeKey(warp.name());
            String warpStripped = WarpUtils.stripAccents(warpLower);

            if (warpLower.startsWith(remainingLower) || warpStripped.startsWith(remainingStripped)) {
                builder.suggest(warp.name());
            }
        }
        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestAllWarps(SuggestionsBuilder builder) {
        String remaining = builder.getRemaining();
        String remainingLower = WarpUtils.normalizeKey(remaining);
        String remainingStripped = WarpUtils.stripAccents(remainingLower);

        for (Warp warp : warpManager.getAllWarps()) {
            String warpLower = WarpUtils.normalizeKey(warp.name());
            String warpStripped = WarpUtils.stripAccents(warpLower);

            if (warpLower.startsWith(remainingLower) || warpStripped.startsWith(remainingStripped)) {
                builder.suggest(warp.name());
            }
        }
        return builder.buildFuture();
    }
}
