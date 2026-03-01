package wbog.flok.util;

/**
 * Result of loading/compiling a single .fk script file.
 */
public final class ScriptLoadResult {

    public enum Status { OK, PARSE_ERROR, IO_ERROR, RUNTIME_ERROR }

    private final String fileName;
    private final Status status;
    private final String errorMessage;
    private final int    errorLine;

    private ScriptLoadResult(String fileName, Status status, String errorMessage, int errorLine) {
        this.fileName     = fileName;
        this.status       = status;
        this.errorMessage = errorMessage;
        this.errorLine    = errorLine;
    }

    public static ScriptLoadResult ok(String fileName) {
        return new ScriptLoadResult(fileName, Status.OK, null, -1);
    }

    public static ScriptLoadResult parseError(String fileName, String message, int line) {
        return new ScriptLoadResult(fileName, Status.PARSE_ERROR, message, line);
    }

    public static ScriptLoadResult ioError(String fileName, String message) {
        return new ScriptLoadResult(fileName, Status.IO_ERROR, message, -1);
    }

    public static ScriptLoadResult runtimeError(String fileName, String message) {
        return new ScriptLoadResult(fileName, Status.RUNTIME_ERROR, message, -1);
    }

    public boolean isOk()          { return status == Status.OK; }
    public boolean hasLine()       { return errorLine > 0; }
    public String  getFileName()   { return fileName; }
    public Status  getStatus()     { return status; }
    public String  getErrorMessage() { return errorMessage; }
    public int     getErrorLine()  { return errorLine; }
}
