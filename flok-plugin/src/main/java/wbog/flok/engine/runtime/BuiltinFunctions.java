package wbog.flok.engine.runtime;

import wbog.flok.api.FValue;
import wbog.flok.engine.ast.ASTNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class BuiltinFunctions {

    private BuiltinFunctions() {}

    static FValue call(String name, List<ASTNode> argExprs, ExecutionContext ctx, Interpreter interp) {
        List<FValue> args = new ArrayList<>(argExprs.size());
        for (ASTNode a : argExprs) args.add(interp.eval(a, ctx));

        return switch (name.toLowerCase()) {
            case "abs"        -> FValue.of(Math.abs(num(args, 0)));
            case "ceil"       -> FValue.of(Math.ceil(num(args, 0)));
            case "floor"      -> FValue.of(Math.floor(num(args, 0)));
            case "round"      -> FValue.of((double) Math.round(num(args, 0)));
            case "sqrt"       -> FValue.of(Math.sqrt(num(args, 0)));
            case "pow"        -> FValue.of(Math.pow(num(args, 0), num(args, 1)));
            case "min"        -> FValue.of(Math.min(num(args, 0), num(args, 1)));
            case "max"        -> FValue.of(Math.max(num(args, 0), num(args, 1)));
            case "clamp"      -> FValue.of(Math.max(num(args, 1), Math.min(num(args, 2), num(args, 0))));
            case "log"        -> FValue.of(Math.log(num(args, 0)));
            case "log10"      -> FValue.of(Math.log10(num(args, 0)));
            case "sin"        -> FValue.of(Math.sin(Math.toRadians(num(args, 0))));
            case "cos"        -> FValue.of(Math.cos(Math.toRadians(num(args, 0))));
            case "tan"        -> FValue.of(Math.tan(Math.toRadians(num(args, 0))));
            case "random"     -> FValue.of(Math.random());
            case "random-int" -> {
                int lo = args.size() > 1 ? (int) num(args, 0) : 1;
                int hi = args.size() > 1 ? (int) num(args, 1) : (int) num(args, 0);
                yield FValue.of(lo + (int)(Math.random() * (hi - lo + 1)));
            }
            case "upper"        -> FValue.of(str(args, 0).toUpperCase());
            case "lower"        -> FValue.of(str(args, 0).toLowerCase());
            case "length"       -> {
                FValue v = arg(args, 0);
                yield v.isList() ? FValue.of(v.asList().size()) : FValue.of(str(args, 0).length());
            }
            case "trim"         -> FValue.of(str(args, 0).trim());
            case "starts-with"  -> FValue.of(str(args, 0).startsWith(str(args, 1)));
            case "ends-with"    -> FValue.of(str(args, 0).endsWith(str(args, 1)));
            case "contains"     -> {
                FValue haystack = arg(args, 0);
                if (haystack.isList()) yield FValue.of(haystack.asList().contains(arg(args, 1)));
                yield FValue.of(str(args, 0).contains(str(args, 1)));
            }
            case "replace"      -> FValue.of(str(args, 0).replace(str(args, 1), str(args, 2)));
            case "split"        -> {
                String[] parts = str(args, 0).split(str(args, 1));
                List<FValue> list = new ArrayList<>(parts.length);
                for (String p : parts) list.add(FValue.of(p));
                yield FValue.ofList(list);
            }
            case "substring"    -> {
                String s  = str(args, 0);
                int    lo = (int) num(args, 1);
                int    hi = args.size() > 2 ? (int) num(args, 2) : s.length();
                lo = Math.max(0, lo); hi = Math.min(s.length(), hi);
                yield FValue.of(lo < hi ? s.substring(lo, hi) : "");
            }
            case "index-of"     -> FValue.of(str(args, 0).indexOf(str(args, 1)));
            case "repeat-str"   -> FValue.of(str(args, 0).repeat(Math.max(0, (int) num(args, 1))));
            case "str"          -> FValue.of(arg(args, 0).asString());
            case "num"          -> FValue.of(arg(args, 0).asNumber());
            case "bool"         -> FValue.of(arg(args, 0).asBoolean());

            case "range"        -> {
                int start = (int) num(args, 0);
                int end   = (int) num(args, 1);
                List<FValue> list = new ArrayList<>(Math.abs(end - start));
                int step = end >= start ? 1 : -1;
                for (int i = start; i != end; i += step) list.add(FValue.of(i));
                yield FValue.ofList(list);
            }
            case "push"         -> {
                FValue list = arg(args, 0);
                if (list.isList()) list.asList().add(arg(args, 1));
                yield list;
            }
            case "pop"          -> {
                FValue list = arg(args, 0);
                if (list.isList() && !list.asList().isEmpty())
                    yield list.asList().remove(list.asList().size() - 1);
                yield FValue.NULL;
            }
            case "remove"       -> {
                FValue list = arg(args, 0);
                int    idx  = (int) num(args, 1);
                if (list.isList() && idx >= 0 && idx < list.asList().size())
                    yield list.asList().remove(idx);
                yield FValue.NULL;
            }
            case "join"         -> {
                FValue list = arg(args, 0);
                String sep  = args.size() > 1 ? str(args, 1) : ", ";
                if (!list.isList()) yield FValue.of(list.asString());
                StringBuilder sb = new StringBuilder();
                List<FValue> items = list.asList();
                for (int i = 0; i < items.size(); i++) {
                    if (i > 0) sb.append(sep);
                    sb.append(items.get(i).asString());
                }
                yield FValue.of(sb.toString());
            }
            case "sum"          -> {
                double total = 0;
                for (FValue v : arg(args, 0).asList()) total += v.asNumber();
                yield FValue.of(total);
            }
            case "sort"         -> {
                List<FValue> copy = new ArrayList<>(arg(args, 0).asList());
                copy.sort(FValue::compareTo);
                yield FValue.ofList(copy);
            }
            case "shuffle"      -> {
                List<FValue> copy = new ArrayList<>(arg(args, 0).asList());
                Collections.shuffle(copy);
                yield FValue.ofList(copy);
            }
            case "reverse"      -> {
                List<FValue> copy = new ArrayList<>(arg(args, 0).asList());
                Collections.reverse(copy);
                yield FValue.ofList(copy);
            }
            case "size", "count" -> {
                FValue v = arg(args, 0);
                yield v.isList() ? FValue.of(v.asList().size())
                    : v.isMap()  ? FValue.of(v.asMap().size())
                    : FValue.of(v.asString().length());
            }
            case "is-null"      -> FValue.of(arg(args, 0).isNull());
            case "is-number"    -> FValue.of(arg(args, 0).isNumber());
            case "is-string"    -> FValue.of(arg(args, 0).isString());
            case "is-list"      -> FValue.of(arg(args, 0).isList());
            case "format-time"  -> {
                long secs  = (long) num(args, 0);
                long h     = secs / 3600, m = (secs % 3600) / 60, s = secs % 60;
                yield FValue.of(String.format("%02d:%02d:%02d", h, m, s));
            }

            default -> throw new ScriptException("Unknown function: " + name + "()", 0);
        };
    }


    private static FValue arg(List<FValue> args, int i) {
        return i < args.size() ? args.get(i) : FValue.NULL;
    }

    private static double num(List<FValue> args, int i) {
        return arg(args, i).asNumber();
    }

    private static String str(List<FValue> args, int i) {
        return arg(args, i).asString();
    }
}
