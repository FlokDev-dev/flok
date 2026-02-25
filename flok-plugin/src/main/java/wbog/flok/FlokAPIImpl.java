package wbog.flok;

import wbog.flok.api.FValue;
import wbog.flok.api.FlokAPI;
import wbog.flok.api.FlokEffect;
import wbog.flok.engine.CompiledScript;
import wbog.flok.engine.ScriptEngine;
import wbog.flok.storage.PersistentStorage;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * Internal implementation of {@link FlokAPI}.
 * Registered with Bukkit's ServiceManager at startup.
 * Addons only ever see the FlokAPI interface — never this class.
 */
final class FlokAPIImpl implements FlokAPI {

    private final ScriptEngine      engine;
    private final PersistentStorage storage;

    FlokAPIImpl(ScriptEngine engine, PersistentStorage storage) {
        this.engine  = engine;
        this.storage = storage;
    }

    @Override
    public void registerEffect(String name, FlokEffect effect) {
        engine.getEffectRegistry().register(name, effect);
    }

    @Override
    public void unregisterEffect(String name) {
        engine.getEffectRegistry().unregister(name);
    }

    @Override
    public void fireEvent(String eventName, Player player, Map<String, FValue> params) {
        engine.dispatchEvent(eventName, player, params);
    }

    @Override
    public boolean hasEventHandler(String eventName) {
        return engine.getEventIndex().containsKey(CompiledScript.normalizeEventName(eventName));
    }

    @Override
    public boolean hasCommand(String commandName) {
        return engine.getCommandIndex().containsKey(commandName.toLowerCase());
    }

    @Override
    public int getLoadedScriptCount() {
        return engine.getLoadedScriptCount();
    }

    @Override
    public FValue getStorage(String key) {
        return storage.get(key);
    }

    @Override
    public void setStorage(String key, FValue value) {
        storage.set(key, value);
    }
}
