package wbog.flok;

import wbog.flok.api.FlokAPI;
import wbog.flok.commands.DynamicCommandRegistrar;
import wbog.flok.commands.FlokCommand;
import wbog.flok.effects.EffectRegistry;
import wbog.flok.engine.ScriptEngine;
import wbog.flok.events.EventAdapter;
import wbog.flok.storage.PersistentStorage;
import wbog.flok.util.FLogger;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/**
 * Flok — Lightweight Minecraft scripting plugin.
 *
 * Startup order:
 *  1. Logger
 *  2. Config
 *  3. PersistentStorage (load data)
 *  4. EffectRegistry
 *  5. ScriptEngine
 *  6. Load scripts
 *  7. Register commands
 *  8. Register event listener
 *  9. Register FlokAPI with Bukkit ServiceManager
 * 10. Schedule auto-save (every 5 min)
 */
public final class FlokPlugin extends JavaPlugin {

    private PersistentStorage    storage;
    private EffectRegistry       effectRegistry;
    private ScriptEngine         engine;
    private DynamicCommandRegistrar commandRegistrar;

    @Override
    public void onEnable() {
        // 1. Logger
        FLogger.init(this);

        // 2. Config
        saveDefaultConfig();

        // 3. Storage
        storage = new PersistentStorage(this);
        storage.load();

        // 4. Effects
        effectRegistry = new EffectRegistry();

        // 5. Engine
        engine = new ScriptEngine(this, storage, effectRegistry);

        // 6. Load scripts
        File scriptsFolder = new File(getDataFolder(),
                getConfig().getString("scripts.folder", "scripts"));
        if (!scriptsFolder.exists()) {
            scriptsFolder.mkdirs();
            FLogger.info("Created scripts folder: " + scriptsFolder.getPath());
        }
        engine.loadAll(scriptsFolder);

        // 7. Commands (script-defined + admin)
        commandRegistrar = new DynamicCommandRegistrar(this, engine);
        commandRegistrar.registerAll();

        var flokCmd = getCommand("flok");
        if (flokCmd != null) {
            var handler = new FlokCommand(this);
            flokCmd.setExecutor(handler);
            flokCmd.setTabCompleter(handler);
        }

        // 8. Events
        getServer().getPluginManager().registerEvents(new EventAdapter(engine, this), this);

        // 9. Register API
        getServer().getServicesManager().register(
                FlokAPI.class,
                new FlokAPIImpl(engine, storage),
                this,
                ServicePriority.Normal
        );

        // 10. Auto-save every 5 minutes (6000 ticks)
        getServer().getScheduler().runTaskTimerAsynchronously(this, storage::save, 6000L, 6000L);

        FLogger.info("Flok enabled. " + engine.getLoadedScriptCount() + " script(s) loaded. !WARNING PLUGIN IN BETA EXPECT ERRORS!");
    }

    @Override
    public void onDisable() {
        storage.forceSave();
        engine.shutdown();
        FLogger.info("Flok disabled.");
    }

    // ── Accessors for command handlers ────────────────────────────────────────

    public ScriptEngine             getEngine()            { return engine; }
    public PersistentStorage        getStorage()           { return storage; }
    public DynamicCommandRegistrar  getCommandRegistrar()  { return commandRegistrar; }
}