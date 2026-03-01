package wbog.flok.api;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class FValue {

    public enum Type { NULL, BOOLEAN, NUMBER, STRING, LIST, MAP }

    public static final FValue NULL         = new FValue(Type.NULL,    null);
    public static final FValue TRUE         = new FValue(Type.BOOLEAN, Boolean.TRUE);
    public static final FValue FALSE        = new FValue(Type.BOOLEAN, Boolean.FALSE);
    public static final FValue ZERO         = new FValue(Type.NUMBER,  0.0);
    public static final FValue ONE          = new FValue(Type.NUMBER,  1.0);
    public static final FValue EMPTY_STRING = new FValue(Type.STRING,  "");

    private final Type   type;
    private final Object raw;

    private FValue(Type type, Object raw) {
        this.type = type;
        this.raw  = raw;
    }

    public static FValue of(boolean b)           { return b ? TRUE : FALSE; }
    public static FValue of(double d)            { return d == 0.0 ? ZERO : d == 1.0 ? ONE : new FValue(Type.NUMBER, d); }
    public static FValue of(long l)              { return of((double) l); }
    public static FValue of(int i)               { return of((double) i); }
    public static FValue of(String s)            { return s == null ? NULL : s.isEmpty() ? EMPTY_STRING : new FValue(Type.STRING, s); }
    public static FValue ofList(List<FValue> l)  { return new FValue(Type.LIST, l); }
    public static FValue ofMap(Map<String,FValue> m) { return new FValue(Type.MAP, m); }
    public static FValue newList()               { return ofList(new ArrayList<>()); }
    public static FValue newMap()                { return ofMap(new LinkedHashMap<>()); }

    public Type    getType()   { return type; }
    public boolean isNull()    { return type == Type.NULL; }
    public boolean isBoolean() { return type == Type.BOOLEAN; }
    public boolean isNumber()  { return type == Type.NUMBER; }
    public boolean isString()  { return type == Type.STRING; }
    public boolean isList()    { return type == Type.LIST; }
    public boolean isMap()     { return type == Type.MAP; }

    public boolean asBoolean() {
        return switch (type) {
            case NULL    -> false;
            case BOOLEAN -> (Boolean) raw;
            case NUMBER  -> (Double) raw != 0.0;
            case STRING  -> !((String) raw).isEmpty();
            case LIST    -> !((List<?>) raw).isEmpty();
            case MAP     -> !((Map<?,?>) raw).isEmpty();
        };
    }

    public double asNumber() {
        return switch (type) {
            case NULL    -> 0.0;
            case BOOLEAN -> (Boolean) raw ? 1.0 : 0.0;
            case NUMBER  -> (Double) raw;
            case STRING  -> { try { yield Double.parseDouble((String) raw); } catch (NumberFormatException e) { yield 0.0; } }
            default      -> 0.0;
        };
    }

    public long   asLong() { return (long)  asNumber(); }
    public int    asInt()  { return (int)   asNumber(); }

    @SuppressWarnings("unchecked")
    public List<FValue> asList() {
        return type == Type.LIST ? (List<FValue>) raw : new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    public Map<String, FValue> asMap() {
        return type == Type.MAP ? (Map<String, FValue>) raw : new LinkedHashMap<>();
    }

    public String asString() {
        return switch (type) {
            case NULL    -> "";
            case BOOLEAN -> raw.toString();
            case NUMBER  -> {
                double d = (Double) raw;
                yield (d == Math.floor(d) && !Double.isInfinite(d))
                    ? String.valueOf((long) d)
                    : String.valueOf(d);
            }
            case STRING  -> (String) raw;
            case LIST    -> {
                StringBuilder sb = new StringBuilder("[");
                List<FValue> l = asList();
                for (int i = 0; i < l.size(); i++) { if (i > 0) sb.append(", "); sb.append(l.get(i).asString()); }
                sb.append("]");
                yield sb.toString();
            }
            case MAP     -> {
                StringBuilder sb = new StringBuilder("{");
                boolean first = true;
                for (var e : asMap().entrySet()) {
                    if (!first) sb.append(", ");
                    sb.append(e.getKey()).append(": ").append(e.getValue().asString());
                    first = false;
                }
                sb.append("}");
                yield sb.toString();
            }
        };
    }

    public FValue add(FValue o) {
        if (type == Type.STRING || o.type == Type.STRING) return FValue.of(asString() + o.asString());
        if (type == Type.LIST) {
            List<FValue> n = new ArrayList<>(asList());
            n.add(o);
            return FValue.ofList(n);
        }
        return FValue.of(asNumber() + o.asNumber());
    }

    public FValue subtract(FValue o) { return FValue.of(asNumber() - o.asNumber()); }
    public FValue multiply(FValue o) { return FValue.of(asNumber() * o.asNumber()); }
    public FValue divide(FValue o)   { double d = o.asNumber(); return d == 0.0 ? FValue.of(Double.NaN) : FValue.of(asNumber() / d); }
    public FValue modulo(FValue o)   { double d = o.asNumber(); return d == 0.0 ? ZERO : FValue.of(asNumber() % d); }
    public FValue power(FValue o)    { return FValue.of(Math.pow(asNumber(), o.asNumber())); }
    public FValue negate()           { return type == Type.BOOLEAN ? FValue.of(!asBoolean()) : FValue.of(-asNumber()); }

    public int compareTo(FValue o) {
        if (type == Type.NUMBER && o.type == Type.NUMBER) return Double.compare(asNumber(), o.asNumber());
        return asString().compareTo(o.asString());
    }

    public boolean equalsValue(FValue o) {
        if (type == Type.NULL && o.type == Type.NULL) return true;
        if (type == Type.NULL || o.type == Type.NULL) return false;
        if (type != o.type) {
            if ((type == Type.NUMBER || o.type == Type.NUMBER) &&
                (type == Type.STRING || o.type == Type.STRING)) {
                try { return asNumber() == o.asNumber(); } catch (Exception e) { return asString().equals(o.asString()); }
            }
            return false;
        }
        if (type == Type.LIST || type == Type.MAP) return raw == o.raw;
        return Objects.equals(raw, o.raw);
    }

    @Override public String  toString()  { return asString(); }
    @Override public boolean equals(Object o) { return o instanceof FValue f && equalsValue(f); }
    @Override public int     hashCode()  { return Objects.hash(type, raw); }

    public Object toSerializable() {
        return switch (type) {
            case NULL    -> null;
            case BOOLEAN -> (Boolean) raw;
            case NUMBER  -> (Double) raw;
            case STRING  -> (String) raw;
            case LIST    -> { List<Object> out = new ArrayList<>(); for (FValue v : asList()) out.add(v.toSerializable()); yield out; }
            case MAP     -> { Map<String,Object> out = new LinkedHashMap<>(); for (var e : asMap().entrySet()) out.put(e.getKey(), e.getValue().toSerializable()); yield out; }
        };
    }

    public static FValue fromSerializable(Object obj) {
        if (obj == null)           return NULL;
        if (obj instanceof Boolean b)  return FValue.of(b);
        if (obj instanceof Number n)   return FValue.of(n.doubleValue());
        if (obj instanceof String s)   return FValue.of(s);
        if (obj instanceof List<?> l)  { List<FValue> out = new ArrayList<>(l.size()); for (Object i : l) out.add(fromSerializable(i)); return FValue.ofList(out); }
        if (obj instanceof Map<?,?> m) { Map<String,FValue> out = new LinkedHashMap<>(); for (var e : m.entrySet()) out.put(String.valueOf(e.getKey()), fromSerializable(e.getValue())); return FValue.ofMap(out); }
        return FValue.of(obj.toString());
    }
}
