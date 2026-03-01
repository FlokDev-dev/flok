package wbog.flok.engine.runtime;

import wbog.flok.api.FValue;
import wbog.flok.api.FlokContext;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

import java.util.HashMap;
import java.util.Map;

public final class ExecutionContext implements FlokContext {

    // Local variable frame — reset on reuse
    private final Map<String, FValue> locals = new HashMap<>(16);

    private Player     player;
    private long       opsRemaining;
    private Cancellable cancellableEvent;
    private boolean returnFlag;
    private boolean breakFlag;
    private boolean continueFlag;
    private FValue  returnValue;

    private int callDepth;
    private static final int MAX_CALL_DEPTH = 64;

    private boolean playerVarsInjected;

    public ExecutionContext(Player player, long maxOps) {
        this.player       = player;
        this.opsRemaining = maxOps;
    }

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


    @Override public Player getPlayer()                    { return player; }
    @Override public FValue getLocal(String name)          { return locals.getOrDefault(name, FValue.NULL); }
    @Override public void   setLocal(String name, FValue v){ locals.put(name, v); }

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

    public boolean tickOp() {
        if (--opsRemaining <= 0) throw new OpLimitExceededException();
        return true;
    }

    public long    opsRemaining()                { return opsRemaining; }
    public void     syncOpsFromChild(long childRemaining) {
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

    public Map<String, FValue> locals()          { return locals; }

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

    public static final class OpLimitExceededException extends RuntimeException {
        public OpLimitExceededException() { super("Script exceeded maximum operation limit", null, true, false); }
    }

    public static final class CallDepthExceededException extends RuntimeException {
        public CallDepthExceededException() { super("Script exceeded maximum call depth (" + MAX_CALL_DEPTH + ")", null, true, false); }
    }
}