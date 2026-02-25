package wbog.flok.engine.runtime;

/**
 * Thrown by the Interpreter when a recoverable script-level error occurs
 * (e.g. type error, missing variable). Caught by ScriptEngine and logged
 * with file+line context. Never propagates to Bukkit's error handler.
 */
public final class ScriptException extends RuntimeException {

    private final int line;

    public ScriptException(String message, int line) {
        super(message, null, true, false); // disable stack trace — perf
        this.line = line;
    }

    public int getLine() { return line; }
}
