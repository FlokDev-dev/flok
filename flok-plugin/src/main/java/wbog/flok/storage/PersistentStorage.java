package wbog.flok.storage;

import wbog.flok.api.FValue;
import wbog.flok.util.FLogger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persistent variable storage for flok.
 *
 * Backed by YAML on disk (humanreadable, bundled with Bukkit, easy admin editing).
 * ConcurrentHashMap for safe reads from any thread; writes assumed on main thread.
 *
 * Saves are deferred — only written when dirty, via the auto-save scheduler
 * in FlokPlugin (every 5 minutes) and on shutdown.
 */
public final class PersistentStorage {

    private final JavaPlugin plugin;
    private final File dataFile;
    private final ConcurrentHashMap<String, FValue> data = new ConcurrentHashMap<>();
    private volatile boolean dirty = false;

    public PersistentStorage(JavaPlugin plugin) {
        this.plugin   = plugin;
        String fname  = plugin.getConfig().getString("storage.data-file", "flok_data.yml");
        this.dataFile = new File(plugin.getDataFolder(), fname);
    }

    public FValue get(String key) {
        return data.getOrDefault(key, FValue.NULL);
    }

    public void set(String key, FValue value) {
        if (value == null || value.isNull()) {
            data.remove(key);
        } else {
            data.put(key, value);
        }
        dirty = true;
    }

    public void delete(String key) {
        data.remove(key);
        dirty = true;
    }

    public boolean has(String key) {
        FValue v = data.get(key);
        return v != null && !v.isNull();
    }

    public Set<String> keys() {
        return java.util.Collections.unmodifiableSet(data.keySet());
    }

    public Map<String, FValue> getAll() {
        return Collections.unmodifiableMap(data);
    }

    /**
     * Atomically increment a numeric key by {@code amount}.
     * Used for __key__ += expr without a read modify write race.
     */
    public FValue increment(String key, double amount) {
        FValue updated = data.merge(key, FValue.of(amount),
                (existing, delta) -> FValue.of(existing.asNumber() + delta.asNumber()));
        dirty = true;
        return updated;
    }

    public void load() {
        if (!dataFile.exists()) {
            FLogger.info("No persistent data file found starting fresh.");
            return;
        }
        try {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(dataFile);
            data.clear();
            for (String key : yaml.getKeys(false)) {
                data.put(key, FValue.fromSerializable(yaml.get(key)));
            }
            FLogger.info("Loaded " + data.size() + " persistent variable(s).");
        } catch (Exception e) {
            FLogger.error("Failed to load persistent data: " + e.getMessage());
        }
    }

    public void save() {
        if (!dirty) return;
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            YamlConfiguration yaml = new YamlConfiguration();
            for (Map.Entry<String, FValue> entry : data.entrySet()) {
                yaml.set(entry.getKey(), entry.getValue().toSerializable());
            }
            yaml.save(dataFile);
            dirty = false;
            FLogger.debug("Persistent data saved (" + data.size() + " entries).");
        } catch (IOException e) {
            FLogger.error("Failed to save persistent data: " + e.getMessage());
        }
    }

    public void forceSave() { dirty = true; save(); }

    public void reset() {
        data.clear();
        dirty = true;
        save();
    }
}