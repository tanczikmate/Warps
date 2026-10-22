package me.tanczikmate.warps;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class WarpManager {

    private final Warps plugin;
    private final File warpsFile;
    private final Map<String, Warp> warps = new ConcurrentHashMap<>();

    public WarpManager(Warps plugin) {
        this.plugin = plugin;
        this.warpsFile = new File(plugin.getDataFolder(), "warps.yml");
        loadWarps();
    }

    public synchronized void loadWarps() {
        warps.clear();

        if (!warpsFile.exists()) {
            return;
        }

        try (Reader reader = new InputStreamReader(new FileInputStream(warpsFile), StandardCharsets.UTF_8)) {
            YamlConfiguration config = new YamlConfiguration();
            config.load(reader);

            ConfigurationSection section = config.getConfigurationSection("warps");
            if (section == null) {
                return;
            }

            for (String key : section.getKeys(false)) {
                ConfigurationSection warpSec = section.getConfigurationSection(key);
                if (warpSec != null) {
                    Warp warp = Warp.deserialize(key, warpSec);
                    warps.put(WarpUtils.normalizeKey(warp.name()), warp);
                }
            }
            plugin.getLogger().info("Loaded " + warps.size() + " warp(s).");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Could not load warps.yml", e);
        }
    }

    public void setWarp(Warp warp) {
        warps.put(WarpUtils.normalizeKey(warp.name()), warp);
        saveWarpsAsync();
    }

    public boolean deleteWarp(String name) {
        Warp exact = getWarp(name);
        if (exact != null) {
            warps.remove(WarpUtils.normalizeKey(exact.name()));
            saveWarpsAsync();
            return true;
        }
        return false;
    }

    public Warp getWarp(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String normalizedKey = WarpUtils.normalizeKey(name);
        Warp exact = warps.get(normalizedKey);
        if (exact != null) {
            return exact;
        }

        // Accent-insensitive fallback (e.g. typing "vasar" finds "vásár")
        String strippedQuery = WarpUtils.stripAccents(normalizedKey);
        for (Warp warp : warps.values()) {
            if (WarpUtils.stripAccents(WarpUtils.normalizeKey(warp.name())).equals(strippedQuery)) {
                return warp;
            }
        }

        return null;
    }

    public boolean warpExists(String name) {
        return getWarp(name) != null;
    }

    public Collection<Warp> getAllWarps() {
        return Collections.unmodifiableCollection(warps.values());
    }

    public List<Warp> getAccessibleWarps(Player player, ConfigManager configManager) {
        List<Warp> accessible = new ArrayList<>();
        for (Warp warp : warps.values()) {
            if (configManager.canAccessWarp(player, warp.name())) {
                accessible.add(warp);
            }
        }
        accessible.sort(Comparator.comparing(w -> WarpUtils.normalizeKey(w.name())));
        return accessible;
    }

    public void saveWarpsAsync() {
        Map<String, Map<String, Object>> snapshot = new LinkedHashMap<>();
        for (Warp warp : warps.values()) {
            snapshot.put(warp.name(), warp.serialize());
        }

        plugin.getServer().getAsyncScheduler().runNow(plugin, task -> {
            YamlConfiguration config = new YamlConfiguration();
            ConfigurationSection section = config.createSection("warps");
            for (Map.Entry<String, Map<String, Object>> entry : snapshot.entrySet()) {
                section.createSection(entry.getKey(), entry.getValue());
            }

            try {
                if (!plugin.getDataFolder().exists()) {
                    plugin.getDataFolder().mkdirs();
                }
                try (Writer writer = new OutputStreamWriter(new FileOutputStream(warpsFile), StandardCharsets.UTF_8)) {
                    writer.write(config.saveToString());
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save warps.yml", e);
            }
        });
    }
}
