package yaluv.flok;

import yaluv.flok.api.FlokAPI;
import yaluv.flok.commands.DynamicCommandRegistrar;
import yaluv.flok.commands.FlokCommand;
import yaluv.flok.effects.EffectRegistry;
import yaluv.flok.engine.ScriptEngine;
import yaluv.flok.events.EventAdapter;
import yaluv.flok.storage.PersistentStorage;
import yaluv.flok.util.FLogger;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/**
 * Flok Lightweight Minecraft scripting plugin.
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
        FLogger.init(this);

        saveDefaultConfig();

        storage = new PersistentStorage(this);
        storage.load();

        effectRegistry = new EffectRegistry();

        engine = new ScriptEngine(this, storage, effectRegistry);

        File scriptsFolder = new File(getDataFolder(),
                getConfig().getString("scripts.folder", "scripts"));
        if (!scriptsFolder.exists()) {
            scriptsFolder.mkdirs();
            FLogger.info("Created scripts folder: " + scriptsFolder.getPath());
        }
        engine.loadAll(scriptsFolder);

        commandRegistrar = new DynamicCommandRegistrar(this, engine);
        commandRegistrar.registerAll();

        var flokCmd = getCommand("flok");
        if (flokCmd != null) {
            var handler = new FlokCommand(this);
            flokCmd.setExecutor(handler);
            flokCmd.setTabCompleter(handler);
        }

        getServer().getPluginManager().registerEvents(new EventAdapter(engine, this), this);

        getServer().getServicesManager().register(
                FlokAPI.class,
                new FlokAPIImpl(engine, storage),
                this,
                ServicePriority.Normal
        );

        getServer().getScheduler().runTaskTimerAsynchronously(this, storage::save, 6000L, 6000L);

        FLogger.info("Flok enabled. " + engine.getLoadedScriptCount() + " script(s) loaded. !WARNING PLUGIN IN BETA EXPECT ERRORS!");
    }

    @Override
    public void onDisable() {
        storage.forceSave();
        engine.shutdown();
        FLogger.info("Flok disabled.");
    }


    public ScriptEngine             getEngine()            { return engine; }
    public PersistentStorage        getStorage()           { return storage; }
    public DynamicCommandRegistrar  getCommandRegistrar()  { return commandRegistrar; }
}