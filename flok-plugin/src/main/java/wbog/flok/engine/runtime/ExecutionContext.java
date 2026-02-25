package wbog.flok.engine.runtime;

import wbog.flok.api.FValue;
import wbog.flok.api.FlokContext;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

import java.util.HashMap;
import java.util.Map;

/**
 * Runtime execution context for a single script block invocation.
 *
 * PERFORMANCE NOTES:
 * - Implements FlokContext (the public API interface) so effects can interact with it
 *   without importing this internal class.
 * - Designed to be pooled via {@link ContextPool} — call reset() before reuse.
 * - Uses a plain HashMap (not ConcurrentHashMap) because script execution is
 *   always on the main server thread.
 * - Player variables are injected lazily by the interpreter on first read,
 *   not eagerly on context creation. This avoids ~12 FValue allocations
 *   for blocks that never touch player state.
 */
public final class ExecutionContext implements FlokContext {

    // Local variable frame — reset on reuse
    private final Map<String, FValue> locals = new HashMap<>(16);

    private Player     player;
    private long       opsRemaining;
    private Cancellable cancellableEvent; // null if event is not cancellable
    private boolean returnFlag;
    private boolean breakFlag;
    private boolean continueFlag;
    private FValue  returnValue;

    // Call stack depth guard — prevents infinite recursion from blowing the JVM stack
    private int callDepth;
    private static final int MAX_CALL_DEPTH = 64;

    // Lazily-resolved player variables flag
    // When false, player vars have not been injected yet.
    // The interpreter checks this and injects on first %player-*% read.
    private boolean playerVarsInjected;

    public ExecutionContext(Player player, long maxOps) {
        this.player       = player;
        this.opsRemaining = maxOps;
    }

    /** Reset all state for pooled reuse. */
    public void reset(Player player, long maxOps) {
        this.player              = player;
        this.opsRemaining        = maxOps;
        this.returnFlag          = false;
        this.breakFlag           = false;
        this.continueFlag        = false;
        this.returnValue         = null;
        this.callDepth           = 0;
        this.playerVarsInjected  = false;
        this.cancellableEvent    = null;
        this.locals.clear();
    }

    // ── FlokContext (public API) ──────────────────────────────────────────────

    @Override public Player getPlayer()                    { return player; }
    @Override public FValue getLocal(String name)          { return locals.getOrDefault(name, FValue.NULL); }
    @Override public void   setLocal(String name, FValue v){ locals.put(name, v); }

    // ── Internal engine access ────────────────────────────────────────────────

    public void   setPlayer(Player p)                  { this.player = p; }
    public boolean isReturnSet()                       { return returnFlag; }
    public boolean isBreakSet()                        { return breakFlag; }
    public boolean isContinueSet()                     { return continueFlag; }
    public FValue  getReturnValue()                    { return returnValue != null ? returnValue : FValue.NULL; }

    public void signalReturn(FValue value) {
        this.returnValue = value;
        this.returnFlag  = true;
    }

    public void clearReturn()   { returnFlag = false; returnValue = null; }
    public void signalBreak()   { breakFlag    = true; }
    public void clearBreak()    { breakFlag    = false; }
    public void signalContinue(){ continueFlag = true; }
    public void clearContinue() { continueFlag = false; }

    /** Returns false and throws if the op limit is exceeded. */
    public boolean tickOp() {
        if (--opsRemaining <= 0) throw new OpLimitExceededException();
        return true;
    }

    public long    opsRemaining()                { return opsRemaining; }
    /** Called after a child function frame returns — deducts ops the child consumed. */
    public void     syncOpsFromChild(long childRemaining) {
        // child started with our opsRemaining; we take back whatever it didn't use
        this.opsRemaining = Math.min(this.opsRemaining, childRemaining);
    }

    public boolean pushCall() {
        if (callDepth >= MAX_CALL_DEPTH) throw new CallDepthExceededException();
        callDepth++;
        return true;
    }

    public void popCall() { callDepth--; }

    public boolean isPlayerVarsInjected()       { return playerVarsInjected; }
    public void    markPlayerVarsInjected()      { playerVarsInjected = true; }

    /** Direct access to the locals map — used by Interpreter for child frame merging. */
    public Map<String, FValue> locals()          { return locals; }

    /**
     * Create a deep copy of this context for use in a wait continuation.
     * The snapshot is independent of the pool and retains all local variables,
     * player reference, remaining op budget, and control-flow flags.
     */
    public ExecutionContext snapshot() {
        ExecutionContext snap = new ExecutionContext(player, opsRemaining);
        snap.locals.putAll(this.locals);
        snap.returnFlag         = this.returnFlag;
        snap.breakFlag          = this.breakFlag;
        snap.continueFlag       = this.continueFlag;
        snap.returnValue        = this.returnValue;
        snap.callDepth          = this.callDepth;
        snap.playerVarsInjected = this.playerVarsInjected;
        return snap;
    }

    // ── Event cancellation ───────────────────────────────────────────────────────

    /** Attach a cancellable Bukkit event to this context. Called by ScriptEngine before dispatch. */
    public void setCancellableEvent(Cancellable event) { this.cancellableEvent = event; }

    @Override
    public void cancelEvent() {
        if (cancellableEvent != null) cancellableEvent.setCancelled(true);
    }

    public void uncancelEvent() {
        if (cancellableEvent != null) cancellableEvent.setCancelled(false);
    }

    @Override
    public boolean isEventCancelled() {
        return cancellableEvent != null && cancellableEvent.isCancelled();
    }

    // ── Exceptions ────────────────────────────────────────────────────────────

    public static final class OpLimitExceededException extends RuntimeException {
        public OpLimitExceededException() { super("Script exceeded maximum operation limit", null, true, false); }
    }

    public static final class CallDepthExceededException extends RuntimeException {
        public CallDepthExceededException() { super("Script exceeded maximum call depth (" + MAX_CALL_DEPTH + ")", null, true, false); }
    }
}