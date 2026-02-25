package wbog.flok.api;

import org.bukkit.entity.Player;

/**
 * Read/write access to the current script execution context.
 * Passed to {@link FlokEffect} implementations so they can interact
 * with script-local variables without touching engine internals.
 */
public interface FlokContext {

    /** Get the player running this script block. May be null. */
    Player getPlayer();

    /** Read a local script variable. Returns {@link FValue#NULL} if not set. */
    FValue getLocal(String name);

    /** Write a local script variable. */
    void setLocal(String name, FValue value);

    /** Check whether a local variable is set and non-null. */
    default boolean hasLocal(String name) {
        return !getLocal(name).isNull();
    }

    /**
     * Cancel the Bukkit event that triggered this script block.
     * No-op if the event is not cancellable or there is no event context.
     */
    void cancelEvent();

    /** Returns true if the event has been cancelled by this script. */
    boolean isEventCancelled();
}