package wbog.flok.engine;

import wbog.flok.api.FValue;
import wbog.flok.effects.EffectRegistry;
import wbog.flok.engine.ast.ASTNode;
import wbog.flok.engine.runtime.ContextPool;
import wbog.flok.engine.runtime.ExecutionContext;
import wbog.flok.engine.runtime.Interpreter;
import wbog.flok.engine.runtime.ScriptException;
import wbog.flok.engine.runtime.WaitSignal;
import wbog.flok.parser.Lexer;
import wbog.flok.parser.ParseException;
import wbog.flok.parser.Parser;
import wbog.flok.storage.PersistentStorage;
import wbog.flok.util.FLogger;
import wbog.flok.util.ScriptLoadResult;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * Central script engine for flok.
 *
 * PERFORMANCE DESIGN:
 * - Event dispatch is O(1) via pre-built index (no per event script scan).
 * - ExecutionContext is pooled via ContextPool zero HashMap allocation per dispatch.
 * - Player variables are injected lazily by the Interpreter, not eagerly on every call.
 * - High-frequency events (player move, player tick) are throttled at this layer
 *   to a minimum of 1 second per player enforced unconditionally, not configurable
 *   per script, because a script author should never be able to cause per tick execution :D.
 */
public final class ScriptEngine {

    private static final Set<String> THROTTLED_EVENTS = Set.of(
            "player-move", "player-tick", "player-step"
    );
    private static final long THROTTLE_MS = 1000L; // 1 second

    private final HashMap<String, Long> throttleTable = new HashMap<>();

    private final Map<String, CompiledScript>         scripts      = new HashMap<>();
    private final Map<String, List<EventEntry>>       eventIndex   = new HashMap<>();
    private final Map<String, CommandEntry>           commandIndex = new HashMap<>();

    public record EventEntry  (CompiledScript script, ASTNode.EventBlock   block) {}
    public record CommandEntry(CompiledScript script, ASTNode.CommandBlock block) {}

    private final PersistentStorage storage;
    private final EffectRegistry    effectRegistry;
    private final long              maxOps;
    private final JavaPlugin        plugin;

    public ScriptEngine(JavaPlugin plugin, PersistentStorage storage, EffectRegistry effectRegistry) {
        this.storage        = storage;
        this.effectRegistry = effectRegistry;
        this.maxOps         = plugin.getConfig().getLong("safety.max-ops", 50_000L);
        this.plugin         = plugin;
    }

    public List<ScriptLoadResult> loadAll(File folder) {
        List<ScriptLoadResult> results = new ArrayList<>();
        if (!folder.exists() || !folder.isDirectory()) {
            FLogger.warn("Scripts folder not found: " + folder.getPath());
            return results;
        }

        scripts.clear();
        eventIndex.clear();
        commandIndex.clear();
        throttleTable.clear();

        File[] files = folder.listFiles(f -> f.getName().endsWith(".fk"));
        if (files == null) return results;

        Arrays.sort(files, Comparator.comparing(File::getName));

        for (File file : files) {
            ScriptLoadResult r = loadScriptInternal(file);
            results.add(r);
            FLogger.info((r.isOk() ? "  ✓ " : "  ✗ ") + file.getName()
                    + (r.isOk() ? "" : ": " + r.getErrorMessage()));
        }

        rebuildIndices();
        long ok = results.stream().filter(ScriptLoadResult::isOk).count();
        FLogger.info("Loaded " + ok + "/" + results.size() + " script(s).");
        return results;
    }

    public ScriptLoadResult loadSingle(File scriptsFolder, String fileName) {
        String name = fileName.endsWith(".fk") ? fileName : fileName + ".fk";
        File file   = new File(scriptsFolder, name);
        if (!file.exists()) return ScriptLoadResult.ioError(name, "File not found");
        ScriptLoadResult r = loadScriptInternal(file);
        rebuildIndices();
        return r;
    }

    private ScriptLoadResult loadScriptInternal(File file) {
        String name = file.getName();
        try {
            String source    = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            CompiledScript cs = compile(source, name);
            scripts.put(name, cs);
            return ScriptLoadResult.ok(name);
        } catch (IOException e) {
            return ScriptLoadResult.ioError(name, e.getMessage());
        } catch (ParseException e) {
            return ScriptLoadResult.parseError(name, e.getRawMessage(), e.getLine());
        } catch (Exception e) {
            return ScriptLoadResult.runtimeError(name, e.getMessage());
        }
    }

    public CompiledScript compile(String source, String name) throws ParseException {
        Lexer lexer        = new Lexer(source, name);
        List<Lexer.Token> tokens = lexer.tokenize();
        Parser parser      = new Parser(tokens, name);
        ASTNode.Program ast = parser.parse();
        return new CompiledScript(name, ast);
    }

    private void rebuildIndices() {
        eventIndex.clear();
        commandIndex.clear();
        for (CompiledScript cs : scripts.values()) {
            cs.getEventIndex().forEach((k, v) ->
                    eventIndex.computeIfAbsent(k, x -> new ArrayList<>())
                            .add(new EventEntry(cs, v)));
            cs.getCommandIndex().forEach((k, v) ->
                    commandIndex.put(k, new CommandEntry(cs, v)));
        }
    }

    public void dispatchEvent(String eventName, Player player, Map<String, FValue> params) {
        dispatchEvent(eventName, player, params, null);
    }

    public void dispatchEvent(String eventName, Player player, Map<String, FValue> params, Cancellable cancellable) {
        String normalized = CompiledScript.normalizeEventName(eventName);

        if (player != null && THROTTLED_EVENTS.contains(normalized)) {
            String key   = normalized + ":" + player.getUniqueId();
            long   now   = System.currentTimeMillis();
            Long   last  = throttleTable.get(key);
            if (last != null && now - last < THROTTLE_MS) return;
            throttleTable.put(key, now);
        }

        List<EventEntry> handlers = eventIndex.get(normalized);
        if (handlers == null || handlers.isEmpty()) return;

        for (EventEntry entry : handlers) {
            run(entry.script(), entry.block().body(), player, params, cancellable);
        }
    }

    public boolean dispatchCommand(String commandName, Player player, String[] args) {
        CommandEntry entry = commandIndex.get(commandName.toLowerCase());
        if (entry == null) return false;

        Map<String, FValue> params = new HashMap<>(8);
        params.put("args",       buildArgsList(args));
        params.put("args-count", FValue.of(args.length));

        List<String> paramNames = entry.block().paramNames();
        for (int i = 0; i < paramNames.size(); i++) {
            params.put(paramNames.get(i), i < args.length ? FValue.of(args[i]) : FValue.EMPTY_STRING);
        }

        run(entry.script(), entry.block().body(), player, params, null);
        return true;
    }

    private void run(CompiledScript script, ASTNode.Block block,
                     Player player, Map<String, FValue> params, Cancellable cancellable) {
        ExecutionContext ctx = ContextPool.acquire(player, maxOps);
        if (cancellable != null) ctx.setCancellableEvent(cancellable);
        try {
            params.forEach(ctx::setLocal);
            Interpreter interp = new Interpreter(script, storage, effectRegistry);
            interp.executeBlock(block, ctx);

        } catch (WaitSignal w) {
            WaitSignal detached = new WaitSignal(w.delayTicks(), w.remaining(),
                    ContextPool.detach(w.ctx()));
            ContextPool.release(ctx);
            scheduleResume(script, detached);
            return;
        } catch (ScriptException e) {
            FLogger.scriptError(script.getName(), e.getLine(), e.getMessage());
        } catch (ExecutionContext.OpLimitExceededException e) {
            FLogger.warn("[" + script.getName() + "] Script exceeded op limit and was halted.");
        } catch (ExecutionContext.CallDepthExceededException e) {
            FLogger.warn("[" + script.getName() + "] Script exceeded max call depth (infinite recursion?).");
        } catch (Exception e) {
            FLogger.error("[" + script.getName() + "] Unexpected error: " + e.getMessage());
            if (FLogger.isDebugMode()) e.printStackTrace();
        } finally {
            ContextPool.release(ctx);
        }
    }

    /**
     * Schedule a wait continuation via Bukkit's scheduler.
     * The context is NOT pooled here — it must remain live until the task fires.
     * Nested waits re-schedule recursively.
     */
    private void scheduleResume(CompiledScript script, WaitSignal w) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            ExecutionContext ctx = w.ctx();
            Interpreter interp  = new Interpreter(script, storage, effectRegistry);
            try {
                interp.resumeBlock(w.remaining(), ctx);
            } catch (WaitSignal w2) {
                scheduleResume(script, w2);
            } catch (ScriptException e) {
                FLogger.scriptError(script.getName(), e.getLine(), e.getMessage());
            } catch (ExecutionContext.OpLimitExceededException e) {
                FLogger.warn("[" + script.getName() + "] Script exceeded op limit in wait continuation.");
            } catch (Exception e) {
                FLogger.error("[" + script.getName() + "] Error in wait continuation: " + e.getMessage());
                if (FLogger.isDebugMode()) e.printStackTrace();
            }
        }, w.delayTicks());
    }


    public EffectRegistry                        getEffectRegistry()  { return effectRegistry; }
    public PersistentStorage                     getStorage()         { return storage; }
    public Collection<CompiledScript>            getScripts()         { return scripts.values(); }
    public int                                   getLoadedScriptCount(){ return scripts.size(); }
    public Map<String, List<EventEntry>>         getEventIndex()      { return eventIndex; }
    public Map<String, CommandEntry>             getCommandIndex()    { return commandIndex; }

    public void shutdown() {
        scripts.clear();
        eventIndex.clear();
        commandIndex.clear();
        throttleTable.clear();
    }

    private FValue buildArgsList(String[] args) {
        List<FValue> list = new ArrayList<>(args.length);
        for (String a : args) list.add(FValue.of(a));
        return FValue.ofList(list);
    }
}