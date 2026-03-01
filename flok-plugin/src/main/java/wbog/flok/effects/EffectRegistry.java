package wbog.flok.effects;

import wbog.flok.api.FlokEffect;
import wbog.flok.util.FLogger;

import java.util.HashMap;
import java.util.Map;

public final class EffectRegistry {

    private final Map<String, FlokEffect> effects = new HashMap<>(64);

    public EffectRegistry() {
        BuiltinEffects.registerAll(this);
    }

    public void register(String name, FlokEffect handler) {
        effects.put(name.toLowerCase(), handler);
        FLogger.debug("Effect registered: " + name);
    }

    public void unregister(String name) {
        effects.remove(name.toLowerCase());
    }

    public FlokEffect get(String name) {
        return effects.get(name.toLowerCase());
    }

    public boolean has(String name) {
        return effects.containsKey(name.toLowerCase());
    }

    public int size() { return effects.size(); }
}
