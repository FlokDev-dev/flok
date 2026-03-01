package wbog.flok.parser;

import java.util.ArrayList;
import java.util.List;

public final class Lexer {

    public enum TokenType {
        NUMBER, STRING, BOOLEAN, NULL,
        IDENTIFIER,
        KW_ON, KW_COMMAND, KW_FUNCTION,
        KW_IF, KW_ELSEIF, KW_ELSE,
        KW_WHILE, KW_FOR, KW_IN, KW_REPEAT, KW_TIMES,
        KW_RETURN, KW_BREAK, KW_CONTINUE, KW_STOP,
        KW_WAIT, KW_TICKS, KW_SECONDS,
        KW_AND, KW_OR, KW_NOT,
        KW_IS, KW_ISNT, KW_CONTAINS,
        KW_SET, KW_TO, KW_ADD, KW_REMOVE, KW_FROM,
        RUNTIME_VAR,
        PERSIST_VAR,
        PLUS, MINUS, STAR, SLASH, PERCENT, CARET,
        PLUS_ASSIGN, MINUS_ASSIGN, STAR_ASSIGN, SLASH_ASSIGN,
        EQ, NEQ, LT, LTE, GT, GTE,
        ASSIGN,
        COLON, COMMA, DOT, BANG,
        LPAREN, RPAREN, LBRACKET, RBRACKET, LBRACE, RBRACE,
        ARROW,
        NEWLINE, INDENT, DEDENT,
        EOF
    }

    public record Token(TokenType type, String value, int line) {
        @Override public String toString() { return "Token(" + type + ",'" + value + "'," + line + ")"; }
    }

    private final String source;
    private final String fileName;
    private int pos;
    private int line;
    private final List<Token>   tokens      = new ArrayList<>();
    private final List<Integer> indentStack = new ArrayList<>();

    public Lexer(String source, String fileName) {
        this.source   = source;
        this.fileName = fileName;
        this.pos      = 0;
        this.line     = 1;
        this.indentStack.add(0);
    }

    public List<Token> tokenize() throws ParseException {
        while (pos < source.length()) tokenizeLine();
        while (indentStack.size() > 1) {
            indentStack.remove(indentStack.size() - 1);
            tokens.add(new Token(TokenType.DEDENT, "", line));
        }
        tokens.add(new Token(TokenType.EOF, "", line));
        return tokens;
    }

    private void tokenizeLine() throws ParseException {
        int indent = 0;
        while (pos < source.length() && (source.charAt(pos) == ' ' || source.charAt(pos) == '\t')) {
            indent += source.charAt(pos) == '\t' ? 4 : 1;
            pos++;
        }
        if (pos < source.length() && source.charAt(pos) == '\n') { pos++; line++; return; }
        if (pos < source.length() && source.charAt(pos) == '#')  { skipToEndOfLine(); return; }
        if (pos >= source.length()) return;

        int cur = indentStack.get(indentStack.size() - 1);
        if (indent > cur) {
            indentStack.add(indent);
            tokens.add(new Token(TokenType.INDENT, "", line));
        } else if (indent < cur) {
            while (indentStack.size() > 1 && indentStack.get(indentStack.size() - 1) > indent) {
                indentStack.remove(indentStack.size() - 1);
                tokens.add(new Token(TokenType.DEDENT, "", line));
            }
            if (indentStack.get(indentStack.size() - 1) != indent)
                throw new ParseException("Inconsistent indentation", fileName, line);
        }

        while (pos < source.length() && source.charAt(pos) != '\n') {
            char c = source.charAt(pos);
            if (c == '#')                                    { skipToEndOfLine(); break; }
            if (Character.isWhitespace(c))                  { pos++; continue; }
            if (c == '%')                                    { tokens.add(isRuntimeVarStart() ? lexRuntimeVar() : lexSymbol()); continue; }
            if (c == '_' && peek1() == '_')                 { tokens.add(lexPersistVar()); continue; }
            if (c == '"' || c == '\'')                      { tokens.add(lexString(c)); continue; }
            if (isDigitStart())                              { tokens.add(lexNumber()); continue; }
            if (Character.isLetter(c) || c == '_')          { tokens.add(lexWord()); continue; }
            tokens.add(lexSymbol());
        }
        if (pos < source.length() && source.charAt(pos) == '\n') {
            tokens.add(new Token(TokenType.NEWLINE, "", line));
            pos++; line++;
        }
    }

    private boolean isDigitStart() {
        char c = source.charAt(pos);
        if (Character.isDigit(c)) return true;
        if (c == '-' && pos + 1 < source.length() && Character.isDigit(source.charAt(pos + 1)))
            return isUnaryPosition();
        return false;
    }

    private boolean isUnaryPosition() {
        if (tokens.isEmpty()) return true;
        return switch (tokens.get(tokens.size() - 1).type()) {
            case LPAREN, COMMA, ASSIGN, PLUS, MINUS, STAR, SLASH, PERCENT, CARET,
                 PLUS_ASSIGN, MINUS_ASSIGN, STAR_ASSIGN, SLASH_ASSIGN,
                 EQ, NEQ, LT, LTE, GT, GTE,
                 INDENT, NEWLINE, KW_RETURN, KW_TO, KW_AND, KW_OR, KW_NOT,
                 COLON, LBRACKET -> true;
            default -> false;
        };
    }

    private boolean isRuntimeVarStart() {
        int next = pos + 1;
        if (next >= source.length()) return false;
        char n = source.charAt(next);
        if (!Character.isLetterOrDigit(n) && n != '_') return false;
        if (n == '_' && next + 1 < source.length() && source.charAt(next + 1) == '_') return false;
        int close = next;
        while (close < source.length() && source.charAt(close) != '%' && source.charAt(close) != '\n') close++;
        return close < source.length() && source.charAt(close) == '%';
    }

    private Token lexRuntimeVar() throws ParseException {
        int startLine = line;
        pos++;
        StringBuilder sb = new StringBuilder();
        while (pos < source.length() && source.charAt(pos) != '%' && source.charAt(pos) != '\n')
            sb.append(source.charAt(pos++));
        if (pos >= source.length() || source.charAt(pos) == '\n')
            throw new ParseException("Unterminated %variable%", fileName, startLine);
        pos++;
        return new Token(TokenType.RUNTIME_VAR, sb.toString(), startLine);
    }

    private Token lexPersistVar() throws ParseException {
        int startLine = line;
        pos += 2;
        StringBuilder sb = new StringBuilder();
        while (pos + 1 < source.length()) {
            if (source.charAt(pos) == '\n')
                throw new ParseException("Unterminated __variable__", fileName, startLine);
            if (source.charAt(pos) == '_' && source.charAt(pos + 1) == '_') break;
            sb.append(source.charAt(pos++));
        }
        if (pos + 1 >= source.length())
            throw new ParseException("Unterminated __variable__", fileName, startLine);
        pos += 2;
        return new Token(TokenType.PERSIST_VAR, sb.toString(), startLine);
    }

    private Token lexString(char quote) throws ParseException {
        int startLine = line;
        pos++;
        StringBuilder sb = new StringBuilder();
        while (pos < source.length() && source.charAt(pos) != quote) {
            char c = source.charAt(pos);
            if (c == '\n') throw new ParseException("Unterminated string", fileName, startLine);
            if (c == '\\') {
                pos++;
                if (pos >= source.length()) break;
                sb.append(switch (source.charAt(pos)) {
                    case 'n'  -> '\n';  case 't'  -> '\t';
                    case '\\' -> '\\';  case '\'' -> '\'';
                    case '"'  -> '"';   default   -> source.charAt(pos);
                });
            } else {
                sb.append(c);
            }
            pos++;
        }
        if (pos >= source.length()) throw new ParseException("Unterminated string", fileName, startLine);
        pos++;
        return new Token(TokenType.STRING, sb.toString(), startLine);
    }

    private Token lexNumber() {
        int startLine = line;
        StringBuilder sb = new StringBuilder();
        if (source.charAt(pos) == '-') sb.append(source.charAt(pos++));
        while (pos < source.length() && Character.isDigit(source.charAt(pos))) sb.append(source.charAt(pos++));
        if (pos < source.length() && source.charAt(pos) == '.' &&
                pos + 1 < source.length() && Character.isDigit(source.charAt(pos + 1))) {
            sb.append(source.charAt(pos++));
            while (pos < source.length() && Character.isDigit(source.charAt(pos))) sb.append(source.charAt(pos++));
        }
        return new Token(TokenType.NUMBER, sb.toString(), startLine);
    }

    private Token lexWord() {
        int startLine = line;
        StringBuilder sb = new StringBuilder();
        while (pos < source.length()) {
            char c = source.charAt(pos);
            if (Character.isLetterOrDigit(c) || c == '_') { sb.append(c); pos++; }
            else if (c == '-' && pos + 1 < source.length() && Character.isLetterOrDigit(source.charAt(pos + 1))) {
                sb.append(c); pos++;
            } else break;
        }
        String word = sb.toString();
        TokenType type = keyword(word.toLowerCase());
        return new Token(type, word, startLine);
    }

    private TokenType keyword(String w) {
        return switch (w) {
            case "on"                        -> TokenType.KW_ON;
            case "command"                   -> TokenType.KW_COMMAND;
            case "function"                  -> TokenType.KW_FUNCTION;
            case "if"                        -> TokenType.KW_IF;
            case "elseif","else-if","elif"   -> TokenType.KW_ELSEIF;
            case "else"                      -> TokenType.KW_ELSE;
            case "while"                     -> TokenType.KW_WHILE;
            case "for"                       -> TokenType.KW_FOR;
            case "in"                        -> TokenType.KW_IN;
            case "repeat"                    -> TokenType.KW_REPEAT;
            case "times"                     -> TokenType.KW_TIMES;
            case "return"                    -> TokenType.KW_RETURN;
            case "break"                     -> TokenType.KW_BREAK;
            case "continue"                  -> TokenType.KW_CONTINUE;
            case "stop"                      -> TokenType.KW_STOP;
            case "wait"                      -> TokenType.KW_WAIT;
            case "tick","ticks"             -> TokenType.KW_TICKS;
            case "second","seconds"         -> TokenType.KW_SECONDS;
            case "and"                       -> TokenType.KW_AND;
            case "or"                        -> TokenType.KW_OR;
            case "not"                       -> TokenType.KW_NOT;
            case "is"                        -> TokenType.KW_IS;
            case "isnt","isn't"             -> TokenType.KW_ISNT;
            case "contains"                  -> TokenType.KW_CONTAINS;
            case "set"                       -> TokenType.KW_SET;
            case "to"                        -> TokenType.KW_TO;
            case "add"                       -> TokenType.KW_ADD;
            case "remove","subtract"        -> TokenType.KW_REMOVE;
            case "from"                      -> TokenType.KW_FROM;
            case "true"                      -> TokenType.BOOLEAN;
            case "false"                     -> TokenType.BOOLEAN;
            case "null","none","nil"        -> TokenType.NULL;
            default                          -> TokenType.IDENTIFIER;
        };
    }

    private Token lexSymbol() throws ParseException {
        int startLine = line;
        char c = source.charAt(pos++);
        return switch (c) {
            case '+' -> { if (pos < source.length() && source.charAt(pos) == '=') { pos++; yield new Token(TokenType.PLUS_ASSIGN,  "+=", startLine); } yield new Token(TokenType.PLUS,  "+", startLine); }
            case '-' -> { if (pos < source.length() && source.charAt(pos) == '=') { pos++; yield new Token(TokenType.MINUS_ASSIGN, "-=", startLine); }
                if (pos < source.length() && source.charAt(pos) == '>') { pos++; yield new Token(TokenType.ARROW,        "->", startLine); }
                yield new Token(TokenType.MINUS, "-", startLine); }
            case '*' -> { if (pos < source.length() && source.charAt(pos) == '=') { pos++; yield new Token(TokenType.STAR_ASSIGN,  "*=", startLine); } yield new Token(TokenType.STAR,  "*", startLine); }
            case '/' -> { if (pos < source.length() && source.charAt(pos) == '=') { pos++; yield new Token(TokenType.SLASH_ASSIGN, "/=", startLine); } yield new Token(TokenType.SLASH, "/", startLine); }
            case '%' -> new Token(TokenType.PERCENT, "%", startLine);
            case '^' -> new Token(TokenType.CARET,   "^", startLine);
            case '=' -> { if (pos < source.length() && source.charAt(pos) == '=') { pos++; yield new Token(TokenType.EQ,  "==", startLine); } yield new Token(TokenType.ASSIGN, "=", startLine); }
            case '!' -> { if (pos < source.length() && source.charAt(pos) == '=') { pos++; yield new Token(TokenType.NEQ, "!=", startLine); } yield new Token(TokenType.BANG, "!", startLine); }
            case '<' -> { if (pos < source.length() && source.charAt(pos) == '=') { pos++; yield new Token(TokenType.LTE, "<=", startLine); } yield new Token(TokenType.LT, "<", startLine); }
            case '>' -> { if (pos < source.length() && source.charAt(pos) == '=') { pos++; yield new Token(TokenType.GTE, ">=", startLine); } yield new Token(TokenType.GT, ">", startLine); }
            case '(' -> new Token(TokenType.LPAREN,   "(", startLine);
            case ')' -> new Token(TokenType.RPAREN,   ")", startLine);
            case '[' -> new Token(TokenType.LBRACKET, "[", startLine);
            case ']' -> new Token(TokenType.RBRACKET, "]", startLine);
            case '{' -> new Token(TokenType.LBRACE,   "{", startLine);
            case '}' -> new Token(TokenType.RBRACE,   "}", startLine);
            case ':' -> new Token(TokenType.COLON,    ":", startLine);
            case ',' -> new Token(TokenType.COMMA,    ",", startLine);
            case '.' -> new Token(TokenType.DOT,      ".", startLine);
            case '?' -> new Token(TokenType.BANG,     "?", startLine);
            default  -> throw new ParseException("Unexpected character: '" + c + "'", fileName, startLine);
        };
    }

    private char peek1() { return (pos + 1 < source.length()) ? source.charAt(pos + 1) : '\0'; }

    private void skipToEndOfLine() {
        while (pos < source.length() && source.charAt(pos) != '\n') pos++;
        if (pos < source.length()) { pos++; line++; }
    }
}