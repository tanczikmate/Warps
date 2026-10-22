package me.tanczikmate.warps;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import me.tanczikmate.warps.commands.WarpsCommands;
import org.bukkit.plugin.java.JavaPlugin;

public final class Warps extends JavaPlugin {

    public static final String PERMISSION_USE = "warps.use";
    public static final String PERMISSION_SET = "warps.set";
    public static final String PERMISSION_DELETE = "warps.delete";
    public static final String PERMISSION_LIST = "warps.list";
    public static final String PERMISSION_RELOAD = "warps.reload";
    public static final String PERMISSION_TELEPORT_OTHERS = "warps.teleport.others";
    public static final String PERMISSION_BYPASS_WARMUP = "warps.bypass.warmup";
    public static final String PERMISSION_BYPASS_COOLDOWN = "warps.bypass.cooldown";
    public static final String PERMISSION_ADMIN = "warps.admin";

    private static Warps instance;
    private ConfigManager configManager;
    private WarpManager warpManager;
    private TeleportManager teleportManager;
    private ProtectionManager protectionManager;

    public static Warps getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public WarpManager getWarpManager() {
        return warpManager;
    }

    public TeleportManager getTeleportManager() {
        return teleportManager;
    }

    public ProtectionManager getProtectionManager() {
        return protectionManager;
    }

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        this.configManager = new ConfigManager(this);
        this.warpManager = new WarpManager(this);
        this.protectionManager = new ProtectionManager();
        this.teleportManager = new TeleportManager(this);

        // Register event listener (move cancel, damage cancel, quit cleanup, post-teleport protection)
        getServer().getPluginManager().registerEvents(new WarpsListener(this), this);

        // Register modern Paper Brigadier commands via LifecycleEvents
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            new WarpsCommands(this).register(event.registrar());
        });

        getLogger().info("Warps plugin enabled successfully!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Warps plugin disabled.");
    }
}
