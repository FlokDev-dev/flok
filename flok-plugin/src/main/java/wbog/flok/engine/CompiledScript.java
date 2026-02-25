package wbog.flok.engine;

import wbog.flok.engine.ast.ASTNode;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A compiled Flok script — holds the AST and pre-built dispatch indexes.
 * Indexing is done once at compile time so dispatch is O(1) at runtime.
 */
public final class CompiledScript {

    private final String name;
    private final ASTNode.Program ast;

    private final Map<String, ASTNode.EventBlock>   eventIndex    = new HashMap<>();
    private final Map<String, ASTNode.CommandBlock> commandIndex  = new HashMap<>();
    private final Map<String, ASTNode.FunctionDef>  functionIndex = new HashMap<>();

    public CompiledScript(String name, ASTNode.Program ast) {
        this.name = name;
        this.ast  = ast;
        buildIndices();
    }

    private void buildIndices() {
        for (ASTNode node : ast.children()) {
            switch (node) {
                case ASTNode.EventBlock   eb -> eventIndex.put(normalizeEventName(eb.eventName()), eb);
                case ASTNode.CommandBlock cb -> commandIndex.put(cb.commandName().toLowerCase(), cb);
                case ASTNode.FunctionDef  fd -> functionIndex.put(fd.name().toLowerCase(), fd);
                default -> {} // top-level statements not supported outside blocks
            }
        }
    }

    /** Normalize event names: lowercase, trim, spaces → hyphens. */
    public static String normalizeEventName(String name) {
        return name.toLowerCase().trim().replace(' ', '-');
    }

    public String               getName()         { return name; }
    public ASTNode.Program      getAst()          { return ast; }
    public ASTNode.FunctionDef  getFunction(String n) { return functionIndex.get(n.toLowerCase()); }
    public ASTNode.EventBlock   getEvent(String n)    { return eventIndex.get(normalizeEventName(n)); }
    public ASTNode.CommandBlock getCommand(String n)  { return commandIndex.get(n.toLowerCase()); }

    public Map<String, ASTNode.EventBlock>   getEventIndex()    { return Collections.unmodifiableMap(eventIndex); }
    public Map<String, ASTNode.CommandBlock> getCommandIndex()  { return Collections.unmodifiableMap(commandIndex); }
    public Map<String, ASTNode.FunctionDef>  getFunctionIndex() { return Collections.unmodifiableMap(functionIndex); }

    @Override
    public String toString() {
        return "CompiledScript{name='" + name + "', events=" + eventIndex.keySet()
            + ", commands=" + commandIndex.keySet() + ", functions=" + functionIndex.keySet() + '}';
    }
}
