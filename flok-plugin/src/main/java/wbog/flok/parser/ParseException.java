package wbog.flok.parser;


public final class ParseException extends Exception {

    private final String rawMessage;
    private final int    line;

    public ParseException(String message, int line) {
        super("[line " + line + "] " + message);
        this.rawMessage = message;
        this.line       = line;
    }

    public ParseException(String message, String fileName, int line) {
        super("[" + fileName + " line " + line + "] " + message);
        this.rawMessage = message;
        this.line       = line;
    }

    public ParseException(String message) {
        this(message, -1);
    }

    public String getRawMessage() { return rawMessage; }
    public int    getLine()       { return line; }
}