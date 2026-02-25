package wbog.flok.engine.ast;

import wbog.flok.api.FValue;

import java.util.List;
import java.util.Map;

/**
 * Abstract Syntax Tree node hierarchy for Flok.
 * Sealed for exhaustive pattern matching — adding a new node type
 * causes a compile error everywhere a switch is missing it.
 */
public sealed interface ASTNode permits
        ASTNode.Program,
        ASTNode.EventBlock,
        ASTNode.CommandBlock,
        ASTNode.FunctionDef,
        ASTNode.IfStmt,
        ASTNode.WhileStmt,
        ASTNode.ForEachStmt,
        ASTNode.RepeatStmt,
        ASTNode.ReturnStmt,
        ASTNode.BreakStmt,
        ASTNode.ContinueStmt,
        ASTNode.VarAssign,
        ASTNode.AugAssign,
        ASTNode.PersistAssign,
        ASTNode.PersistAugAssign,
        ASTNode.EffectStmt,
        ASTNode.ExprStmt,
        ASTNode.Block,
        ASTNode.Literal,
        ASTNode.VarRef,
        ASTNode.PersistRef,
        ASTNode.BinaryOp,
        ASTNode.UnaryOp,
        ASTNode.FunctionCall,
        ASTNode.IndexAccess,
        ASTNode.ListLiteral,
        ASTNode.MapLiteral,
        ASTNode.StringTemplate,
        ASTNode.Conditional,
        ASTNode.WaitStmt,
        ASTNode.PropertyAccess
{
    int line();

    // ── Statements ────────────────────────────────────────────────────────────

    record Program(List<ASTNode> children, int line) implements ASTNode {}

    record EventBlock(String eventName, List<String> params, Block body, int line) implements ASTNode {}

    record CommandBlock(String commandName, List<String> aliases, String permission,
                        String description, List<String> paramNames, Block body, int line) implements ASTNode {}

    record FunctionDef(String name, List<String> params, Block body, int line) implements ASTNode {}

    record IfStmt(ASTNode condition, Block thenBlock,
                  List<ElseBranch> elseBranches, Block elseBlock, int line) implements ASTNode {
        public record ElseBranch(ASTNode condition, Block body) {}
    }

    record WhileStmt(ASTNode condition, Block body, int line) implements ASTNode {}

    record ForEachStmt(String varName, ASTNode iterable, Block body, int line) implements ASTNode {}

    record RepeatStmt(ASTNode count, Block body, int line) implements ASTNode {}

    record ReturnStmt(ASTNode value, int line) implements ASTNode {}
    record BreakStmt(int line) implements ASTNode {}
    record ContinueStmt(int line) implements ASTNode {}

    record VarAssign(String name, ASTNode value, int line) implements ASTNode {}

    record AugAssign(String name, String op, ASTNode value, int line) implements ASTNode {}

    record PersistAssign(ASTNode keyExpr, ASTNode value, int line) implements ASTNode {}

    record PersistAugAssign(ASTNode keyExpr, String op, ASTNode value, int line) implements ASTNode {}

    record EffectStmt(String effectName, List<ASTNode> args, int line) implements ASTNode {}

    record ExprStmt(ASTNode expr, int line) implements ASTNode {}

    record Block(List<ASTNode> statements, int line) implements ASTNode {}

    // ── Expressions ──────────────────────────────────────────────────────────

    record Literal(FValue value, int line) implements ASTNode {}

    record VarRef(String name, int line) implements ASTNode {}

    record PersistRef(ASTNode keyExpr, int line) implements ASTNode {}

    record BinaryOp(String op, ASTNode left, ASTNode right, int line) implements ASTNode {}

    record UnaryOp(String op, ASTNode operand, int line) implements ASTNode {}

    record FunctionCall(String name, List<ASTNode> args, int line) implements ASTNode {}

    record IndexAccess(ASTNode target, ASTNode index, int line) implements ASTNode {}

    record ListLiteral(List<ASTNode> elements, int line) implements ASTNode {}

    record MapLiteral(List<Map.Entry<ASTNode, ASTNode>> entries, int line) implements ASTNode {}

    record StringTemplate(List<Object> parts, int line) implements ASTNode {}
    // parts: String (literal text) | ASTNode (interpolated expression)

    record Conditional(ASTNode condition, ASTNode ifTrue, ASTNode ifFalse, int line) implements ASTNode {}

    record WaitStmt(ASTNode ticks, int line) implements ASTNode {}

    record PropertyAccess(ASTNode target, String property, int line) implements ASTNode {}
}