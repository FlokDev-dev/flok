package wbog.flok.engine.runtime;

import org.bukkit.entity.Player;

public final class ContextPool {
    private static final ThreadLocal<ExecutionContext> POOL =
            ThreadLocal.withInitial(() -> new ExecutionContext(null, 0));

    private ContextPool() {}

    public static ExecutionContext acquire(Player player, long maxOps) {
        ExecutionContext ctx = POOL.get();
        ctx.reset(player, maxOps);
        return ctx;
    }
    public static void release(ExecutionContext ctx) {
    }
    public static ExecutionContext detach(ExecutionContext ctx) {
        return ctx.snapshot();
    }
}