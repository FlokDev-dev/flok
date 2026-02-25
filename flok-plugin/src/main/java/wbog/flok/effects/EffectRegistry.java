package wbog.flok.effects;

import wbog.flok.api.FlokEffect;
import wbog.flok.util.FLogger;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for all effect (action) handlers callable from .fk scripts.
 *
 * Built-in effects are registered here at startup.
 * Addon effects are registered via {@link wbog.flok.api.FlokAPI#registerEffect}.
 *
 * All lookups are O(1). Effect names are stored lowercase.
 */
public final class EffectRegistry {

    private final Map<String, FlokEffect> effects = new HashMap<>(64);

    public EffectRegistry() {
        BuiltinEffects.registerAll(this);
    }

    /** Register or replace an effect handler. Name is lowercased. */
    public void register(String name, FlokEffect handler) {
        effects.put(name.toLowerCase(), handler);
        FLogger.debug("Effect registered: " + name);
    }

    /** Remove an effect handler (called on addon disable). */
    public void unregister(String name) {
        effects.remove(name.toLowerCase());
    }

    /**
     * Look up an effect handler.
     * @return the handler, or null if not registered
     */
    public FlokEffect get(String name) {
        return effects.get(name.toLowerCase());
    }

    public boolean has(String name) {
        return effects.containsKey(name.toLowerCase());
    }

    public int size() { return effects.size(); }
}
