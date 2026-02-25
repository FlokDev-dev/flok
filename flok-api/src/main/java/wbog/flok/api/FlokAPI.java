package wbog.flok.api;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Map;

/**
 * Public API for Flok. Addons should only depend on this module.
 *
 * ── Obtaining the API ────────────────────────────────────────────────────────
 *
 * <pre>
 * RegisteredServiceProvider<FlokAPI> rsp =
 *     Bukkit.getServicesManager().getRegistration(FlokAPI.class);
 * if (rsp != null) {
 *     FlokAPI flok = rsp.getProvider();
 *     flok.registerEffect("my-effect", (player, args, ctx) -> { ... });
 * }
 * </pre>
 *
 * ── Depending on Flok in your addon ─────────────────────────────────────────
 *
 * Add flok-api.jar as a compileOnly dependency. Declare in plugin.yml:
 * <pre>
 *   softdepend: [Flok]
 * </pre>
 */
public interface FlokAPI {

    // ── Effect registration ───────────────────────────────────────────────────

    /**
     * Register a custom effect callable from .fk scripts.
     * Names are case-insensitive. Replaces any existing registration with the same name.
     *
     * @param name   The effect keyword (e.g. "give-item", "send-discord")
     * @param effect Handler implementation
     */
    void registerEffect(String name, FlokEffect effect);

    /**
     * Unregister a custom effect. Call this in your addon's onDisable().
     */
    void unregisterEffect(String name);

    // ── Custom event dispatch ─────────────────────────────────────────────────

    /**
     * Fire a custom event, triggering any matching {@code on <eventName>:} blocks.
     *
     * @param eventName Event name (case-insensitive, spaces treated as hyphens)
     * @param player    Player context — may be null
     * @param params    String key → FValue parameters exposed as script variables
     */
    void fireEvent(String eventName, Player player, Map<String, FValue> params);

    /**
     * Convenience overload — fire an event with no extra parameters.
     */
    default void fireEvent(String eventName, Player player) {
        fireEvent(eventName, player, Map.of());
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    /** Returns true if any loaded script listens for this event. */
    boolean hasEventHandler(String eventName);

    /** Returns true if any loaded script defines this command. */
    boolean hasCommand(String commandName);

    /** Number of currently loaded .fk scripts. */
    int getLoadedScriptCount();

    // ── Storage access ────────────────────────────────────────────────────────

    /**
     * Read a persistent variable (the {@code __key__} storage).
     * Returns {@link FValue#NULL} if not set.
     */
    FValue getStorage(String key);

    /**
     * Write a persistent variable.
     * Passing {@link FValue#NULL} deletes the key.
     */
    void setStorage(String key, FValue value);

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Convenience: look up the registered FlokAPI from the Bukkit service manager. */
    static FlokAPI get() {
        RegisteredServiceProvider<FlokAPI> rsp =
            Bukkit.getServicesManager().getRegistration(FlokAPI.class);
        if (rsp == null) throw new IllegalStateException("Flok is not loaded or did not register its API.");
        return rsp.getProvider();
    }
}
