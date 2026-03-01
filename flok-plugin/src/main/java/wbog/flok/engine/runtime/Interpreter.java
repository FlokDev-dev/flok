package wbog.flok.engine.runtime;

import wbog.flok.api.FValue;
import wbog.flok.effects.EffectRegistry;
import wbog.flok.engine.CompiledScript;
import wbog.flok.engine.ast.ASTNode;
import wbog.flok.storage.PersistentStorage;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class Interpreter {

    private final CompiledScript    script;
    private final PersistentStorage storage;
    private final EffectRegistry    effects;

    public Interpreter(CompiledScript script, PersistentStorage storage, EffectRegistry effects) {
        this.script  = script;
        this.storage = storage;
        this.effects = effects;
    }

    public void executeBlock(ASTNode.Block block, ExecutionContext ctx) {
        List<ASTNode> stmts = block.statements();
        for (int i = 0; i < stmts.size(); i++) {
            if (ctx.isReturnSet() || ctx.isBreakSet() || ctx.isContinueSet()) break;
            try {
                executeStmt(stmts.get(i), ctx);
            } catch (WaitSignal w) {
                List<ASTNode> rest = stmts.subList(i + 1, stmts.size());
                throw new WaitSignal(w.delayTicks(),
                        new java.util.ArrayList<>(rest), w.ctx());
            }
            ctx.tickOp();
        }
    }

    private void executeStmt(ASTNode node, ExecutionContext ctx) {
        switch (node) {

            case ASTNode.VarAssign va ->
                    ctx.setLocal(va.name(), eval(va.value(), ctx));

            case ASTNode.AugAssign aa -> {
                FValue current = resolveVar(aa.name(), ctx);
                FValue delta   = eval(aa.value(), ctx);
                ctx.setLocal(aa.name(), applyAug(aa.op(), current, delta));
            }

            case ASTNode.PersistAssign pa -> {
                String key   = evalKey(pa.keyExpr(), ctx);
                FValue value = eval(pa.value(), ctx);
                storage.set(key, value);
            }

            case ASTNode.PersistAugAssign paa -> {
                String key   = evalKey(paa.keyExpr(), ctx);
                FValue delta = eval(paa.value(), ctx);
                applyPersistAug(paa.op(), key, delta);
            }

            case ASTNode.EffectStmt es -> {
                var handler = effects.get(es.effectName());
                if (handler == null) {
                    throw new ScriptException("Unknown effect: " + es.effectName(), es.line());
                }
                List<FValue> args = new ArrayList<>(es.args().size());
                for (ASTNode a : es.args()) args.add(eval(a, ctx));
                handler.execute(ctx.getPlayer(), args, ctx);
            }

            case ASTNode.IfStmt is -> executeIf(is, ctx);
            case ASTNode.WhileStmt ws -> executeWhile(ws, ctx);
            case ASTNode.ForEachStmt fe -> executeForEach(fe, ctx);
            case ASTNode.RepeatStmt rs -> executeRepeat(rs, ctx);

            case ASTNode.ReturnStmt rs ->
                    ctx.signalReturn(rs.value() != null ? eval(rs.value(), ctx) : FValue.NULL);

            case ASTNode.BreakStmt ignored    -> ctx.signalBreak();
            case ASTNode.ContinueStmt ignored -> ctx.signalContinue();

            case ASTNode.ExprStmt es -> eval(es.expr(), ctx);

            case ASTNode.Block b -> executeBlock(b, ctx);

            case ASTNode.WaitStmt ws -> {
                long ticks = Math.max(1L, eval(ws.ticks(), ctx).asLong());
                throw new WaitSignal(ticks, java.util.Collections.emptyList(), ctx);
            }

            default -> throw new ScriptException("Unhandled statement: " + node.getClass().getSimpleName(), node.line());
        }
    }

    private void executeIf(ASTNode.IfStmt is, ExecutionContext ctx) {
        if (eval(is.condition(), ctx).asBoolean()) {
            executeBlock(is.thenBlock(), ctx);
            return;
        }
        for (ASTNode.IfStmt.ElseBranch branch : is.elseBranches()) {
            if (eval(branch.condition(), ctx).asBoolean()) {
                executeBlock(branch.body(), ctx);
                return;
            }
        }
        if (is.elseBlock() != null) executeBlock(is.elseBlock(), ctx);
    }

    private void executeWhile(ASTNode.WhileStmt ws, ExecutionContext ctx) {
        while (eval(ws.condition(), ctx).asBoolean()) {
            ctx.tickOp();
            executeBlock(ws.body(), ctx);
            if (ctx.isReturnSet()) break;
            if (ctx.isBreakSet())  { ctx.clearBreak(); break; }
            if (ctx.isContinueSet()) ctx.clearContinue();
        }
    }

    private void executeForEach(ASTNode.ForEachStmt fe, ExecutionContext ctx) {
        FValue iterable = eval(fe.iterable(), ctx);
        List<FValue> items;
        if (iterable.isList()) {
            items = iterable.asList();
        } else if (iterable.isMap()) {
            items = new ArrayList<>();
            iterable.asMap().forEach((k, v) -> items.add(FValue.of(k)));
        } else {
            items = new ArrayList<>();
            for (char c : iterable.asString().toCharArray()) items.add(FValue.of(String.valueOf(c)));
        }
        for (FValue item : items) {
            ctx.tickOp();
            ctx.setLocal(fe.varName(), item);
            executeBlock(fe.body(), ctx);
            if (ctx.isReturnSet()) break;
            if (ctx.isBreakSet())  { ctx.clearBreak(); break; }
            if (ctx.isContinueSet()) ctx.clearContinue();
        }
    }

    private void executeRepeat(ASTNode.RepeatStmt rs, ExecutionContext ctx) {
        long count = eval(rs.count(), ctx).asLong();
        for (long i = 0; i < count; i++) {
            ctx.tickOp();
            executeBlock(rs.body(), ctx);
            if (ctx.isReturnSet()) break;
            if (ctx.isBreakSet())  { ctx.clearBreak(); break; }
            if (ctx.isContinueSet()) ctx.clearContinue();
        }
    }

    public FValue eval(ASTNode node, ExecutionContext ctx) {
        ctx.tickOp();
        return switch (node) {

            case ASTNode.Literal lit -> lit.value();

            case ASTNode.VarRef vr   -> resolveVar(vr.name(), ctx);

            case ASTNode.PersistRef pr -> storage.get(evalKey(pr.keyExpr(), ctx));

            case ASTNode.BinaryOp bo  -> evalBinaryOp(bo, ctx);

            case ASTNode.UnaryOp uo   -> {
                FValue v = eval(uo.operand(), ctx);
                yield switch (uo.op()) {
                    case "-"   -> v.negate();
                    case "not" -> FValue.of(!v.asBoolean());
                    default    -> v;
                };
            }

            case ASTNode.FunctionCall fc -> callFunction(fc, ctx);

            case ASTNode.IndexAccess ia -> {
                FValue target = eval(ia.target(), ctx);
                FValue index  = eval(ia.index(), ctx);
                if (target.isList()) {
                    int i = index.asInt();
                    List<FValue> list = target.asList();
                    yield (i >= 0 && i < list.size()) ? list.get(i) : FValue.NULL;
                } else if (target.isMap()) {
                    yield target.asMap().getOrDefault(index.asString(), FValue.NULL);
                }
                yield FValue.NULL;
            }

            case ASTNode.ListLiteral ll -> {
                List<FValue> items = new ArrayList<>(ll.elements().size());
                for (ASTNode e : ll.elements()) items.add(eval(e, ctx));
                yield FValue.ofList(items);
            }

            case ASTNode.MapLiteral ml -> {
                var map = new java.util.LinkedHashMap<String, FValue>();
                for (var entry : ml.entries()) {
                    map.put(eval(entry.getKey(), ctx).asString(), eval(entry.getValue(), ctx));
                }
                yield FValue.ofMap(map);
            }

            case ASTNode.StringTemplate st -> {
                StringBuilder sb = new StringBuilder();
                for (Object part : st.parts()) {
                    if (part instanceof String s) sb.append(s);
                    else sb.append(eval((ASTNode) part, ctx).asString());
                }
                yield FValue.of(sb.toString());
            }

            case ASTNode.Conditional c ->
                    eval(c.condition(), ctx).asBoolean()
                            ? eval(c.ifTrue(), ctx)
                            : eval(c.ifFalse(), ctx);

            case ASTNode.PropertyAccess pa -> {
                FValue target = eval(pa.target(), ctx);
                if (target.isMap()) yield target.asMap().getOrDefault(pa.property(), FValue.NULL);
                yield FValue.NULL;
            }

            default -> throw new ScriptException("Unhandled expression: " + node.getClass().getSimpleName(), node.line());
        };
    }

    private FValue evalBinaryOp(ASTNode.BinaryOp bo, ExecutionContext ctx) {
        if ("and".equals(bo.op())) {
            FValue l = eval(bo.left(), ctx);
            return l.asBoolean() ? eval(bo.right(), ctx) : l;
        }
        if ("or".equals(bo.op())) {
            FValue l = eval(bo.left(), ctx);
            return l.asBoolean() ? l : eval(bo.right(), ctx);
        }

        FValue left  = eval(bo.left(), ctx);
        FValue right = eval(bo.right(), ctx);

        return switch (bo.op()) {
            case "+"   -> left.add(right);
            case "-"   -> left.subtract(right);
            case "*"   -> left.multiply(right);
            case "/"   -> left.divide(right);
            case "%"   -> left.modulo(right);
            case "^"   -> left.power(right);
            case "=="  -> FValue.of(left.equalsValue(right));
            case "!="  -> FValue.of(!left.equalsValue(right));
            case "<"   -> FValue.of(left.compareTo(right) < 0);
            case "<="  -> FValue.of(left.compareTo(right) <= 0);
            case ">"   -> FValue.of(left.compareTo(right) > 0);
            case ">="  -> FValue.of(left.compareTo(right) >= 0);
            case "contains" -> {
                if (left.isList()) yield FValue.of(left.asList().contains(right));
                yield FValue.of(left.asString().contains(right.asString()));
            }
            default    -> throw new ScriptException("Unknown operator: " + bo.op(), bo.line());
        };
    }
    
    private FValue callFunction(ASTNode.FunctionCall fc, ExecutionContext ctx) {
        ASTNode.FunctionDef def = script.getFunction(fc.name());
        if (def != null) return callScriptFunction(def, fc.args(), ctx);

        return BuiltinFunctions.call(fc.name(), fc.args(), ctx, this);
    }

    private FValue callScriptFunction(ASTNode.FunctionDef def, List<ASTNode> argExprs, ExecutionContext ctx) {
        ctx.pushCall();
        ExecutionContext child = new ExecutionContext(ctx.getPlayer(), ctx.opsRemaining());
        for (int i = 0; i < def.params().size(); i++) {
            child.setLocal(def.params().get(i),
                    i < argExprs.size() ? eval(argExprs.get(i), ctx) : FValue.NULL);
        }
        Interpreter childInterp = new Interpreter(script, storage, effects);
        childInterp.executeBlock(def.body(), child);
        ctx.popCall();
        ctx.syncOpsFromChild(child.opsRemaining());
        return child.isReturnSet() ? child.getReturnValue() : FValue.NULL;
    }


    /**
     * Resolve a variable name. On first access to any player variable,
     * inject all player variables lazily into the context.
     */
    private FValue resolveVar(String name, ExecutionContext ctx) {
        FValue v = ctx.getLocal(name);
        if (!v.isNull()) return v;

        if (name.startsWith("player") && !ctx.isPlayerVarsInjected()) {
            injectPlayerVars(ctx);
            v = ctx.getLocal(name);
        }
        return v;
    }

    /**
     * Inject all player-related variables into ctx.
     * Called at most once per execution block (guarded by playerVarsInjected flag).
     */
    private void injectPlayerVars(ExecutionContext ctx) {
        ctx.markPlayerVarsInjected();
        Player p = ctx.getPlayer();
        if (p == null) return;

        ctx.setLocal("player-name",         FValue.of(p.getName()));
        ctx.setLocal("player-display-name", FValue.of(p.getDisplayName()));
        ctx.setLocal("player-uuid",         FValue.of(p.getUniqueId().toString()));
        ctx.setLocal("player-world",        FValue.of(p.getWorld().getName()));
        ctx.setLocal("player-health",       FValue.of(p.getHealth()));
        ctx.setLocal("player-max-health",   FValue.of(p.getMaxHealth()));
        ctx.setLocal("player-food",         FValue.of((double) p.getFoodLevel()));
        ctx.setLocal("player-level",        FValue.of((double) p.getLevel()));
        ctx.setLocal("player-gamemode",     FValue.of(p.getGameMode().name().toLowerCase()));
        ctx.setLocal("player-x",            FValue.of(p.getLocation().getX()));
        ctx.setLocal("player-y",            FValue.of(p.getLocation().getY()));
        ctx.setLocal("player-z",            FValue.of(p.getLocation().getZ()));
        ctx.setLocal("player-yaw",          FValue.of((double) p.getLocation().getYaw()));
        ctx.setLocal("player-pitch",        FValue.of((double) p.getLocation().getPitch()));
    }

    /**
     * Evaluate a persistent key expression (may contain variable interpolation).
     * e.g. the keyExpr for __coins-%player-name%__ evaluates to "coins-Steve".
     */
    private String evalKey(ASTNode keyExpr, ExecutionContext ctx) {
        return eval(keyExpr, ctx).asString();
    }

    private FValue applyAug(String op, FValue current, FValue delta) {
        return switch (op) {
            case "+=" -> current.add(delta);
            case "-=" -> current.subtract(delta);
            case "*=" -> current.multiply(delta);
            case "/=" -> current.divide(delta);
            case "%=" -> current.modulo(delta);
            default   -> delta;
        };
    }

    private void applyPersistAug(String op, String key, FValue delta) {
        if ("+=".equals(op)) {
            storage.increment(key, delta.asNumber());
            return;
        }
        FValue current = storage.get(key);
        storage.set(key, applyAug(op, current, delta));
    }

    /**
     * Resume execution from a wait continuation.
     * Called by ScriptEngine after the scheduled delay has elapsed.
     * Executes the remaining statements, which may themselves contain further waits.
     */
    public void resumeBlock(List<ASTNode> remaining, ExecutionContext ctx) {
        for (int i = 0; i < remaining.size(); i++) {
            if (ctx.isReturnSet() || ctx.isBreakSet() || ctx.isContinueSet()) break;
            try {
                executeStmt(remaining.get(i), ctx);
            } catch (WaitSignal w) {
                List<ASTNode> rest = remaining.subList(i + 1, remaining.size());
                throw new WaitSignal(w.delayTicks(), new java.util.ArrayList<>(rest), w.ctx());
            }
            ctx.tickOp();
        }
    }

    PersistentStorage storage()     { return storage; }
    CompiledScript    script()      { return script; }
    EffectRegistry    effects()     { return effects; }
}