package wbog.flok.engine.runtime;

import wbog.flok.engine.ast.ASTNode;

import java.util.List;

/**
 * Thrown by the Interpreter when a {@code wait} statement is encountered.
 *
 * This is NOT an error — it is a control-flow signal. When caught by
 * ScriptEngine, it schedules a Bukkit task to resume execution after
 * the requested delay, passing the remaining statements as a continuation.
 *
 * Stack trace generation is disabled for performance (same trick as ScriptException).
 */
public final class WaitSignal extends RuntimeException {

    private final long              delayTicks;
    private final List<ASTNode>     remaining;   // statements left in the current block
    private final ExecutionContext  ctx;

    public WaitSignal(long delayTicks, List<ASTNode> remaining, ExecutionContext ctx) {
        super("wait:" + delayTicks, null, true, false); // no stack trace
        this.delayTicks = delayTicks;
        this.remaining  = remaining;
        this.ctx        = ctx;
    }

    public long             delayTicks() { return delayTicks; }
    public List<ASTNode>    remaining()  { return remaining; }
    public ExecutionContext  ctx()        { return ctx; }
}