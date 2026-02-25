package wbog.flok.parser;

import wbog.flok.api.FValue;
import wbog.flok.engine.ast.ASTNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * flok recursive-descent parser.
 *
 * Grammar highlights:
 *  - Indentation-based blocks (INDENT/DEDENT from Lexer)
 *  - Trailing colon on headers is optional
 *  - Augmented assignment: %var% += expr, etc.
 *  - stop = alias for break
 *  - String templates: "hello %name%, you have %__coins-%name__%"
 */
public final class Parser {

    private final List<Lexer.Token> tokens;
    private final String fileName;
    private int pos;

    public Parser(List<Lexer.Token> tokens, String fileName) {
        this.tokens   = tokens;
        this.fileName = fileName;
    }

    // ---------------------------------------------------------------
    // Top level
    // ---------------------------------------------------------------

    public ASTNode.Program parse() throws ParseException {
        List<ASTNode> children = new ArrayList<>();
        skipNewlines();
        while (!check(Lexer.TokenType.EOF)) {
            children.add(parseTopLevel());
            skipNewlines();
        }
        return new ASTNode.Program(children, 1);
    }

    private ASTNode parseTopLevel() throws ParseException {
        return switch (peek().type()) {
            case KW_ON       -> parseEventBlock();
            case KW_COMMAND  -> parseCommandBlock();
            case KW_FUNCTION -> parseFunctionDef();
            default          -> parseStatement();
        };
    }

    // ---------------------------------------------------------------
    // Block headers
    // ---------------------------------------------------------------

    private ASTNode.EventBlock parseEventBlock() throws ParseException {
        int line = peek().line();
        consume(Lexer.TokenType.KW_ON);
        StringBuilder name = new StringBuilder();
        while (!check(Lexer.TokenType.COLON) && !check(Lexer.TokenType.NEWLINE) && !check(Lexer.TokenType.EOF)) {
            if (name.length() > 0) name.append(" ");
            name.append(advance().value());
        }
        if (check(Lexer.TokenType.COLON)) advance();
        skipNewlines();
        return new ASTNode.EventBlock(name.toString().trim(), new ArrayList<>(), parseBlock(), line);
    }

    private ASTNode.CommandBlock parseCommandBlock() throws ParseException {
        int line = peek().line();
        consume(Lexer.TokenType.KW_COMMAND);
        String commandName = consumeIdent();
        List<String> params = new ArrayList<>();
        List<String> aliases = new ArrayList<>();
        String permission = "";
        String description = "";

        // Optional parameter list: command foo(arg1, arg2)
        if (check(Lexer.TokenType.LPAREN)) {
            advance();
            while (!check(Lexer.TokenType.RPAREN) && !check(Lexer.TokenType.EOF)) {
                params.add(consumeIdent());
                if (check(Lexer.TokenType.COMMA)) advance();
            }
            consume(Lexer.TokenType.RPAREN);
        }

        // Optional metadata annotations before the colon:
        //   permission: some.permission
        //   description: My command
        //   aliases: cmd1, cmd2
        while (!check(Lexer.TokenType.COLON) && !check(Lexer.TokenType.NEWLINE) && !check(Lexer.TokenType.EOF)) {
            Lexer.Token meta = peek();
            if (meta.type() == Lexer.TokenType.IDENTIFIER) {
                String key = meta.value().toLowerCase();
                advance();
                if (check(Lexer.TokenType.COLON)) {
                    advance();
                    StringBuilder val = new StringBuilder();
                    while (!check(Lexer.TokenType.NEWLINE) && !check(Lexer.TokenType.COLON) && !check(Lexer.TokenType.EOF)) {
                        if (val.length() > 0) val.append(" ");
                        val.append(advance().value());
                    }
                    switch (key) {
                        case "permission"   -> permission   = val.toString().trim();
                        case "description"  -> description  = val.toString().trim();
                        case "aliases"      -> { for (String a : val.toString().split(",")) aliases.add(a.trim()); }
                    }
                }
            } else {
                advance(); // skip unknown token
            }
        }

        if (check(Lexer.TokenType.COLON)) advance();
        skipNewlines();
        return new ASTNode.CommandBlock(commandName, aliases, permission, description, params, parseBlock(), line);
    }

    private ASTNode.FunctionDef parseFunctionDef() throws ParseException {
        int line = peek().line();
        consume(Lexer.TokenType.KW_FUNCTION);
        String name = consumeIdent();
        List<String> params = new ArrayList<>();
        consume(Lexer.TokenType.LPAREN);
        while (!check(Lexer.TokenType.RPAREN) && !check(Lexer.TokenType.EOF)) {
            params.add(consumeIdent());
            if (check(Lexer.TokenType.COMMA)) advance();
        }
        consume(Lexer.TokenType.RPAREN);
        if (check(Lexer.TokenType.COLON)) advance();
        skipNewlines();
        return new ASTNode.FunctionDef(name, params, parseBlock(), line);
    }

    private ASTNode.Block parseBlock() throws ParseException {
        int line = peek().line();
        if (!check(Lexer.TokenType.INDENT)) return new ASTNode.Block(new ArrayList<>(), line);
        consume(Lexer.TokenType.INDENT);
        List<ASTNode> stmts = new ArrayList<>();
        skipNewlines();
        while (!check(Lexer.TokenType.DEDENT) && !check(Lexer.TokenType.EOF)) {
            stmts.add(parseStatement());
            skipNewlines();
        }
        if (check(Lexer.TokenType.DEDENT)) advance();
        return new ASTNode.Block(stmts, line);
    }

    // ---------------------------------------------------------------
    // Statements
    // ---------------------------------------------------------------

    private ASTNode parseStatement() throws ParseException {
        Lexer.Token t = peek();
        return switch (t.type()) {
            case KW_IF       -> parseIf();
            case KW_WHILE    -> parseWhile();
            case KW_FOR      -> parseForEach();
            case KW_REPEAT   -> parseRepeat();
            case KW_WAIT     -> parseWait();
            case KW_RETURN   -> parseReturn();
            case KW_BREAK, KW_STOP -> { advance(); skipNewlines(); yield new ASTNode.BreakStmt(t.line()); }
            case KW_CONTINUE -> { advance(); skipNewlines(); yield new ASTNode.ContinueStmt(t.line()); }
            case KW_SET      -> parseSet();
            case KW_ADD      -> parseAdd();
            case KW_REMOVE   -> parseRemoveFrom();
            case RUNTIME_VAR -> parseVarStatement();
            case PERSIST_VAR -> parsePersistStatement();
            case IDENTIFIER  -> parseEffectOrCall();
            default -> { ASTNode expr = parseExpression(); skipNewlines(); yield new ASTNode.ExprStmt(expr, t.line()); }
        };
    }

    private ASTNode parseVarStatement() throws ParseException {
        int line = peek().line();
        String name = consume(Lexer.TokenType.RUNTIME_VAR).value();
        ASTNode rhs;
        switch (peek().type()) {
            case ASSIGN       -> { advance(); rhs = parseExpression(); skipNewlines(); return new ASTNode.VarAssign(name, rhs, line); }
            case PLUS_ASSIGN  -> { advance(); rhs = parseExpression(); skipNewlines(); return new ASTNode.VarAssign(name, new ASTNode.BinaryOp("+", new ASTNode.VarRef(name, line), rhs, line), line); }
            case MINUS_ASSIGN -> { advance(); rhs = parseExpression(); skipNewlines(); return new ASTNode.VarAssign(name, new ASTNode.BinaryOp("-", new ASTNode.VarRef(name, line), rhs, line), line); }
            case STAR_ASSIGN  -> { advance(); rhs = parseExpression(); skipNewlines(); return new ASTNode.VarAssign(name, new ASTNode.BinaryOp("*", new ASTNode.VarRef(name, line), rhs, line), line); }
            case SLASH_ASSIGN -> { advance(); rhs = parseExpression(); skipNewlines(); return new ASTNode.VarAssign(name, new ASTNode.BinaryOp("/", new ASTNode.VarRef(name, line), rhs, line), line); }
            default           -> { skipNewlines(); return new ASTNode.ExprStmt(new ASTNode.VarRef(name, line), line); }
        }
    }

    private ASTNode parsePersistStatement() throws ParseException {
        int line   = peek().line();
        String raw = consume(Lexer.TokenType.PERSIST_VAR).value();
        ASTNode key = parsePersistKey(raw, line);
        ASTNode rhs;
        switch (peek().type()) {
            case ASSIGN       -> { advance(); rhs = parseExpression(); skipNewlines(); return new ASTNode.PersistAssign(key, rhs, line); }
            case PLUS_ASSIGN  -> { advance(); rhs = parseExpression(); skipNewlines(); return new ASTNode.PersistAssign(key, new ASTNode.BinaryOp("+", new ASTNode.PersistRef(key, line), rhs, line), line); }
            case MINUS_ASSIGN -> { advance(); rhs = parseExpression(); skipNewlines(); return new ASTNode.PersistAssign(key, new ASTNode.BinaryOp("-", new ASTNode.PersistRef(key, line), rhs, line), line); }
            case STAR_ASSIGN  -> { advance(); rhs = parseExpression(); skipNewlines(); return new ASTNode.PersistAssign(key, new ASTNode.BinaryOp("*", new ASTNode.PersistRef(key, line), rhs, line), line); }
            case SLASH_ASSIGN -> { advance(); rhs = parseExpression(); skipNewlines(); return new ASTNode.PersistAssign(key, new ASTNode.BinaryOp("/", new ASTNode.PersistRef(key, line), rhs, line), line); }
            default           -> { skipNewlines(); return new ASTNode.ExprStmt(new ASTNode.PersistRef(key, line), line); }
        }
    }

    private ASTNode parseSet() throws ParseException {
        int line = peek().line();
        advance(); // 'set'
        if (check(Lexer.TokenType.RUNTIME_VAR)) {
            String name = advance().value();
            consume(Lexer.TokenType.KW_TO);
            ASTNode val = parseExpression(); skipNewlines();
            return new ASTNode.VarAssign(name, val, line);
        }
        if (check(Lexer.TokenType.PERSIST_VAR)) {
            ASTNode key = parsePersistKey(advance().value(), line);
            consume(Lexer.TokenType.KW_TO);
            ASTNode val = parseExpression(); skipNewlines();
            return new ASTNode.PersistAssign(key, val, line);
        }
        String name = consumeIdent();
        consume(Lexer.TokenType.KW_TO);
        ASTNode val = parseExpression(); skipNewlines();
        return new ASTNode.VarAssign(name, val, line);
    }

    private ASTNode parseAdd() throws ParseException {
        int line = peek().line();
        advance(); // 'add'
        ASTNode value = parseExpression();
        consume(Lexer.TokenType.KW_TO);
        if (check(Lexer.TokenType.RUNTIME_VAR)) {
            String name = advance().value(); skipNewlines();
            return new ASTNode.VarAssign(name, new ASTNode.BinaryOp("+", new ASTNode.VarRef(name, line), value, line), line);
        }
        if (check(Lexer.TokenType.PERSIST_VAR)) {
            ASTNode key = parsePersistKey(advance().value(), line); skipNewlines();
            return new ASTNode.PersistAssign(key, new ASTNode.BinaryOp("+", new ASTNode.PersistRef(key, line), value, line), line);
        }
        String name = consumeIdent(); skipNewlines();
        return new ASTNode.VarAssign(name, new ASTNode.BinaryOp("+", new ASTNode.VarRef(name, line), value, line), line);
    }

    private ASTNode parseRemoveFrom() throws ParseException {
        int line = peek().line();
        advance(); // 'remove'
        ASTNode value = parseExpression();
        consume(Lexer.TokenType.KW_FROM);
        if (check(Lexer.TokenType.RUNTIME_VAR)) {
            String name = advance().value(); skipNewlines();
            return new ASTNode.VarAssign(name, new ASTNode.BinaryOp("-", new ASTNode.VarRef(name, line), value, line), line);
        }
        if (check(Lexer.TokenType.PERSIST_VAR)) {
            ASTNode key = parsePersistKey(advance().value(), line); skipNewlines();
            return new ASTNode.PersistAssign(key, new ASTNode.BinaryOp("-", new ASTNode.PersistRef(key, line), value, line), line);
        }
        String name = consumeIdent(); skipNewlines();
        return new ASTNode.VarAssign(name, new ASTNode.BinaryOp("-", new ASTNode.VarRef(name, line), value, line), line);
    }

    private ASTNode parseEffectOrCall() throws ParseException {
        int line    = peek().line();
        String name = advance().value();
        if (check(Lexer.TokenType.LPAREN)) {
            advance();
            List<ASTNode> args = new ArrayList<>();
            while (!check(Lexer.TokenType.RPAREN) && !check(Lexer.TokenType.EOF) && !check(Lexer.TokenType.NEWLINE)) {
                args.add(parseExpression());
                if (check(Lexer.TokenType.COMMA)) advance();
            }
            consume(Lexer.TokenType.RPAREN);
            skipNewlines();
            return new ASTNode.ExprStmt(new ASTNode.FunctionCall(name, args, line), line);
        }
        List<ASTNode> args = new ArrayList<>();
        while (!check(Lexer.TokenType.NEWLINE) && !check(Lexer.TokenType.DEDENT)
                && !check(Lexer.TokenType.EOF) && !check(Lexer.TokenType.INDENT)) {
            args.add(parseExpression());
        }
        skipNewlines();
        return new ASTNode.EffectStmt(name, args, line);
    }

    // ---------------------------------------------------------------
    // Control flow
    // ---------------------------------------------------------------

    private ASTNode parseIf() throws ParseException {
        int line = peek().line();
        consume(Lexer.TokenType.KW_IF);
        ASTNode condition = parseExpression();
        if (check(Lexer.TokenType.COLON)) advance();
        skipNewlines();
        ASTNode.Block thenBlock = parseBlock();
        List<ASTNode.IfStmt.ElseBranch> branches = new ArrayList<>();
        ASTNode.Block elseBlock = null;

        skipNewlines();
        while (check(Lexer.TokenType.KW_ELSEIF) ||
                (check(Lexer.TokenType.KW_ELSE) && peekAhead(1) != null && peekAhead(1).type() == Lexer.TokenType.KW_IF)) {
            if (check(Lexer.TokenType.KW_ELSE)) advance();
            advance();
            ASTNode cond2 = parseExpression();
            if (check(Lexer.TokenType.COLON)) advance();
            skipNewlines();
            branches.add(new ASTNode.IfStmt.ElseBranch(cond2, parseBlock()));
            skipNewlines();
        }
        if (check(Lexer.TokenType.KW_ELSE)) {
            advance();
            if (check(Lexer.TokenType.COLON)) advance();
            skipNewlines();
            elseBlock = parseBlock();
        }
        return new ASTNode.IfStmt(condition, thenBlock, branches, elseBlock, line);
    }

    private ASTNode parseWhile() throws ParseException {
        int line = peek().line();
        consume(Lexer.TokenType.KW_WHILE);
        ASTNode cond = parseExpression();
        if (check(Lexer.TokenType.COLON)) advance();
        skipNewlines();
        return new ASTNode.WhileStmt(cond, parseBlock(), line);
    }

    private ASTNode parseForEach() throws ParseException {
        int line = peek().line();
        consume(Lexer.TokenType.KW_FOR);
        String var = consumeIdent();
        consume(Lexer.TokenType.KW_IN);
        ASTNode iterable = parseExpression();
        if (check(Lexer.TokenType.COLON)) advance();
        skipNewlines();
        return new ASTNode.ForEachStmt(var, iterable, parseBlock(), line);
    }

    private ASTNode parseRepeat() throws ParseException {
        int line = peek().line();
        consume(Lexer.TokenType.KW_REPEAT);
        ASTNode count = parseExpression();
        if (check(Lexer.TokenType.KW_TIMES)) advance();
        if (check(Lexer.TokenType.COLON)) advance();
        skipNewlines();
        return new ASTNode.RepeatStmt(count, parseBlock(), line);
    }

    private ASTNode parseWait() throws ParseException {
        int line = peek().line();
        consume(Lexer.TokenType.KW_WAIT);
        ASTNode amount = parseExpression();
        if (check(Lexer.TokenType.KW_TICKS)) {
            advance();
        } else if (check(Lexer.TokenType.KW_SECONDS)) {
            advance();
            amount = new ASTNode.BinaryOp("*", amount, new ASTNode.Literal(FValue.of(20.0), line), line);
        }
        skipNewlines();
        return new ASTNode.WaitStmt(amount, line);
    }

    private ASTNode parseReturn() throws ParseException {
        int line = peek().line();
        consume(Lexer.TokenType.KW_RETURN);
        if (check(Lexer.TokenType.NEWLINE) || check(Lexer.TokenType.DEDENT) || check(Lexer.TokenType.EOF)) {
            skipNewlines();
            return new ASTNode.ReturnStmt(new ASTNode.Literal(FValue.NULL, line), line);
        }
        ASTNode val = parseExpression();
        skipNewlines();
        return new ASTNode.ReturnStmt(val, line);
    }

    // ---------------------------------------------------------------
    // Expressions (precedence climbing)
    // ---------------------------------------------------------------

    private ASTNode parseExpression() throws ParseException { return parseOr(); }

    private ASTNode parseOr() throws ParseException {
        ASTNode l = parseAnd();
        while (check(Lexer.TokenType.KW_OR)) { int ln = peek().line(); advance(); l = new ASTNode.BinaryOp("or", l, parseAnd(), ln); }
        return l;
    }

    private ASTNode parseAnd() throws ParseException {
        ASTNode l = parseNot();
        while (check(Lexer.TokenType.KW_AND)) { int ln = peek().line(); advance(); l = new ASTNode.BinaryOp("and", l, parseNot(), ln); }
        return l;
    }

    private ASTNode parseNot() throws ParseException {
        if (check(Lexer.TokenType.KW_NOT) || check(Lexer.TokenType.BANG)) {
            int ln = peek().line(); advance();
            return new ASTNode.UnaryOp("not", parseNot(), ln);
        }
        return parseComparison();
    }

    private ASTNode parseComparison() throws ParseException {
        ASTNode l = parseAdditive();
        while (true) {
            int ln = peek().line();
            String op = switch (peek().type()) {
                case EQ       -> "=="; case NEQ      -> "!=";
                case LT       -> "<";  case LTE      -> "<=";
                case GT       -> ">";  case GTE      -> ">=";
                case KW_IS    -> "=="; case KW_ISNT  -> "!=";
                case KW_CONTAINS -> "contains";
                default -> null;
            };
            if (op == null) break;
            advance();
            if ("==".equals(op) && check(Lexer.TokenType.KW_NOT)) { advance(); op = "!="; }
            l = new ASTNode.BinaryOp(op, l, parseAdditive(), ln);
        }
        return l;
    }

    private ASTNode parseAdditive() throws ParseException {
        ASTNode l = parseMultiplicative();
        while (check(Lexer.TokenType.PLUS) || check(Lexer.TokenType.MINUS)) {
            int ln = peek().line(); String op = advance().value();
            l = new ASTNode.BinaryOp(op, l, parseMultiplicative(), ln);
        }
        return l;
    }

    private ASTNode parseMultiplicative() throws ParseException {
        ASTNode l = parseUnary();
        while (check(Lexer.TokenType.STAR) || check(Lexer.TokenType.SLASH) || check(Lexer.TokenType.PERCENT)) {
            int ln = peek().line(); String op = advance().value();
            l = new ASTNode.BinaryOp(op, l, parseUnary(), ln);
        }
        return l;
    }

    private ASTNode parseUnary() throws ParseException {
        if (check(Lexer.TokenType.MINUS)) { int ln = peek().line(); advance(); return new ASTNode.UnaryOp("-", parsePower(), ln); }
        return parsePower();
    }

    private ASTNode parsePower() throws ParseException {
        ASTNode base = parsePostfix();
        if (check(Lexer.TokenType.CARET)) { int ln = peek().line(); advance(); return new ASTNode.BinaryOp("^", base, parseUnary(), ln); }
        return base;
    }

    private ASTNode parsePostfix() throws ParseException {
        ASTNode expr = parsePrimary();
        while (true) {
            if (check(Lexer.TokenType.LBRACKET)) {
                int ln = peek().line(); advance();
                ASTNode idx = parseExpression();
                consume(Lexer.TokenType.RBRACKET);
                expr = new ASTNode.IndexAccess(expr, idx, ln);
            } else if (check(Lexer.TokenType.DOT)) {
                int ln = peek().line(); advance();
                String prop = consumeIdent();
                if (check(Lexer.TokenType.LPAREN)) {
                    advance();
                    List<ASTNode> margs = new ArrayList<>();
                    margs.add(expr);
                    while (!check(Lexer.TokenType.RPAREN) && !check(Lexer.TokenType.EOF)) {
                        margs.add(parseExpression());
                        if (check(Lexer.TokenType.COMMA)) advance();
                    }
                    consume(Lexer.TokenType.RPAREN);
                    expr = new ASTNode.FunctionCall(prop, margs, ln);
                } else {
                    expr = new ASTNode.PropertyAccess(expr, prop, ln);
                }
            } else break;
        }
        return expr;
    }

    private ASTNode parsePrimary() throws ParseException {
        Lexer.Token t = peek();
        return switch (t.type()) {
            case NUMBER  -> { advance(); yield new ASTNode.Literal(FValue.of(Double.parseDouble(t.value())), t.line()); }
            case BOOLEAN -> { advance(); yield new ASTNode.Literal(FValue.of(t.value().equalsIgnoreCase("true")), t.line()); }
            case NULL    -> { advance(); yield new ASTNode.Literal(FValue.NULL, t.line()); }
            case STRING  -> { advance(); yield parseStringTemplate(t.value(), t.line()); }
            case RUNTIME_VAR -> { advance(); yield new ASTNode.VarRef(t.value(), t.line()); }
            case PERSIST_VAR -> { advance(); yield new ASTNode.PersistRef(parsePersistKey(t.value(), t.line()), t.line()); }
            case IDENTIFIER  -> {
                advance();
                if (check(Lexer.TokenType.LPAREN)) {
                    advance();
                    List<ASTNode> args = new ArrayList<>();
                    while (!check(Lexer.TokenType.RPAREN) && !check(Lexer.TokenType.EOF) && !check(Lexer.TokenType.NEWLINE)) {
                        args.add(parseExpression());
                        if (check(Lexer.TokenType.COMMA)) advance();
                    }
                    consume(Lexer.TokenType.RPAREN);
                    yield new ASTNode.FunctionCall(t.value(), args, t.line());
                }
                yield new ASTNode.Literal(FValue.of(t.value()), t.line());
            }
            case LPAREN -> {
                advance();
                ASTNode expr = parseExpression();
                consume(Lexer.TokenType.RPAREN);
                yield expr;
            }
            case LBRACKET -> {
                advance();
                List<ASTNode> elems = new ArrayList<>();
                while (!check(Lexer.TokenType.RBRACKET) && !check(Lexer.TokenType.EOF)) {
                    elems.add(parseExpression());
                    if (check(Lexer.TokenType.COMMA)) advance();
                }
                consume(Lexer.TokenType.RBRACKET);
                yield new ASTNode.ListLiteral(elems, t.line());
            }
            case LBRACE -> {
                advance();
                List<Map.Entry<ASTNode,ASTNode>> entries = new ArrayList<>();
                while (!check(Lexer.TokenType.RBRACE) && !check(Lexer.TokenType.EOF)) {
                    ASTNode key = parseExpression();
                    consume(Lexer.TokenType.COLON);
                    ASTNode val = parseExpression();
                    entries.add(Map.entry(key, val));
                    if (check(Lexer.TokenType.COMMA)) advance();
                }
                consume(Lexer.TokenType.RBRACE);
                yield new ASTNode.MapLiteral(entries, t.line());
            }
            default -> throw new ParseException("Unexpected token: " + t.type() + " ('" + t.value() + "')", fileName, t.line());
        };
    }

    // ---------------------------------------------------------------
    // String template parser
    // ---------------------------------------------------------------

    private ASTNode parseStringTemplate(String raw, int line) {
        List<Object> parts = new ArrayList<>();
        int i = 0;
        StringBuilder lit = new StringBuilder();

        while (i < raw.length()) {
            char c = raw.charAt(i);
            if (c == '_' && i + 1 < raw.length() && raw.charAt(i + 1) == '_') {
                int end = raw.indexOf("__", i + 2);
                if (end >= i + 2) {
                    if (lit.length() > 0) { parts.add(lit.toString()); lit = new StringBuilder(); }
                    String keyRaw = raw.substring(i + 2, end);
                    parts.add(new ASTNode.PersistRef(parsePersistKey(keyRaw, line), line));
                    i = end + 2;
                    continue;
                }
            }
            if (c == '%') {
                // If %__ follows, skip the % and let the __ branch handle it next iteration
                // e.g. "%__coins-%player-name%__" — the % is not a var, it prefixes a persist embed
                if (i + 1 < raw.length() && raw.charAt(i + 1) == '_'
                        && i + 2 < raw.length() && raw.charAt(i + 2) == '_') {
                    i++; // skip %, next char is _ which triggers persist branch
                    continue;
                }
                int end = raw.indexOf('%', i + 1);
                if (end > i) {
                    if (lit.length() > 0) { parts.add(lit.toString()); lit = new StringBuilder(); }
                    parts.add(new ASTNode.VarRef(raw.substring(i + 1, end), line));
                    i = end + 1;
                    continue;
                }
            }
            lit.append(raw.charAt(i++));
        }
        if (lit.length() > 0) parts.add(lit.toString());
        if (parts.size() == 1 && parts.get(0) instanceof String s) return new ASTNode.Literal(FValue.of(s), line);
        return new ASTNode.StringTemplate(parts, line);
    }

    private ASTNode parsePersistKey(String raw, int line) {
        if (!raw.contains("%")) return new ASTNode.Literal(FValue.of(raw), line);
        List<Object> parts = new ArrayList<>();
        int i = 0;
        StringBuilder lit = new StringBuilder();
        while (i < raw.length()) {
            if (raw.charAt(i) == '%') {
                int end = raw.indexOf('%', i + 1);
                if (end > i) {
                    if (lit.length() > 0) { parts.add(lit.toString()); lit = new StringBuilder(); }
                    parts.add(new ASTNode.VarRef(raw.substring(i + 1, end), line));
                    i = end + 1;
                    continue;
                }
            }
            lit.append(raw.charAt(i++));
        }
        if (lit.length() > 0) parts.add(lit.toString());
        if (parts.size() == 1 && parts.get(0) instanceof String s) return new ASTNode.Literal(FValue.of(s), line);
        return new ASTNode.StringTemplate(parts, line);
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private String consumeIdent() throws ParseException {
        Lexer.Token t = peek();
        if (t.type() == Lexer.TokenType.IDENTIFIER) { advance(); return t.value(); }
        return switch (t.type()) {
            case KW_ADD, KW_REMOVE, KW_SET, KW_TO, KW_FROM,
                 KW_IN, KW_IS, KW_WAIT, KW_TIMES, KW_STOP,
                 KW_AND, KW_OR, KW_NOT -> { advance(); yield t.value(); }
            default -> throw new ParseException("Expected identifier, got " + t.type() + " ('" + t.value() + "')", fileName, t.line());
        };
    }

    private Lexer.Token peek()                    { return tokens.get(pos); }
    private Lexer.Token peekAhead(int offset)     { int idx = pos + offset; return idx < tokens.size() ? tokens.get(idx) : null; }
    private Lexer.Token advance()                 { return tokens.get(pos++); }
    private boolean check(Lexer.TokenType t)      { return tokens.get(pos).type() == t; }

    private Lexer.Token consume(Lexer.TokenType t) throws ParseException {
        if (!check(t)) {
            Lexer.Token cur = peek();
            throw new ParseException("Expected " + t + " but got " + cur.type() + " ('" + cur.value() + "')", fileName, cur.line());
        }
        return advance();
    }

    private void skipNewlines() { while (check(Lexer.TokenType.NEWLINE)) advance(); }
}