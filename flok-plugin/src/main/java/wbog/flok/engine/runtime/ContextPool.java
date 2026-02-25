package wbog.flok.engine.runtime;

import org.bukkit.entity.Player;

/**
 * Pool of reusable {@link ExecutionContext} instances.
 *
 * Script execution always happens on the main server thread, so a single
 * pooled instance is sufficient. This eliminates the HashMap allocation
 * that previously occurred on every event dispatch and command execution.
 *
 * Usage:
 * <pre>
 *   ExecutionContext ctx = ContextPool.acquire(player, maxOps);
 *   try {
 *       interpreter.executeBlock(block, ctx);
 *   } finally {
 *       ContextPool.release(ctx);
 *   }
 * </pre>
 */
public final class ContextPool {

    // One pooled context per thread — safe because Bukkit runs scripts on main thread.
    // Using ThreadLocal also makes this safe if async dispatch is ever added.
    private static final ThreadLocal<ExecutionContext> POOL =
            ThreadLocal.withInitial(() -> new ExecutionContext(null, 0));

    private ContextPool() {}

    /** Acquire a context, resetting its state for fresh use. */
    public static ExecutionContext acquire(Player player, long maxOps) {
        ExecutionContext ctx = POOL.get();
        ctx.reset(player, maxOps);
        return ctx;
    }

    /**
     * Release the context back to the pool.
     * Currently a no-op (ThreadLocal handles lifecycle), but exists so call
     * sites are structured correctly for future changes.
     */
    public static void release(ExecutionContext ctx) {
        // no-op: ThreadLocal keeps the instance alive for reuse automatically
    }

    /**
     * Create a fresh, independent copy of {@code ctx} for use in a wait continuation.
     * The copy is NOT pooled — it lives until the scheduled task completes.
     * The pool's ThreadLocal instance remains available for the next synchronous dispatch.
     */
    public static ExecutionContext detach(ExecutionContext ctx) {
        return ctx.snapshot();
    }
}