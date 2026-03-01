# Addon API

Other plugins can extend Flok by depending on `flok-api.jar`. This lets you register custom effects, fire custom events, and read/write persistent storage from Java.

## Getting the API

```java
import wbog.flok.api.FlokAPI;
import org.bukkit.plugin.RegisteredServiceProvider;

RegisteredServiceProvider<FlokAPI> rsp =
    Bukkit.getServicesManager().getRegistration(FlokAPI.class);
if (rsp != null) {
    FlokAPI flok = rsp.getProvider();
}
```

Or use the convenience method (throws if Flok isn't loaded):

```java
FlokAPI flok = FlokAPI.get();
```

In your `plugin.yml`:

```yaml
softdepend: [Flok]
```

---

## Registering Custom Effects

```java
flok.registerEffect("send-discord", (player, args, ctx) -> {
    if (args.isEmpty()) return;
    String message = args.get(0).asString();
    // send to your Discord webhook...
});
```

In scripts, it works identically to a built-in effect:

```fk
on player-join:
    send-discord "**%player-name%** joined the server!"
```

### Unregistering on Disable

Always unregister your effects when your plugin shuts down:

```java
@Override
public void onDisable() {
    FlokAPI flok = FlokAPI.get();
    flok.unregisterEffect("send-discord");
}
```

---

## Firing Custom Events

Fire an event from your plugin — for example, after a shop purchase — and pass in any variables you want available in the script:

```java
flok.fireEvent("shop-purchase", player, Map.of(
    "item",  FValue.of(itemName),
    "price", FValue.of(price)
));
```

Scripts can then handle it normally:

```fk
on shop-purchase:
    send "You bought %item% for %price% coins."
    __coins-%player-name%__ -= %price%
```

---

## Reading and Writing Storage

```java
FValue coins = flok.getStorage("coins-" + playerName);
flok.setStorage("coins-" + playerName, FValue.of(newAmount));
```

---

## FValue Reference

`FValue` is Flok's universal value type used throughout the API.

### Creating Values

```java
FValue.of(true)        // boolean
FValue.of(42.0)        // number (always double internally)
FValue.of("hello")     // string
FValue.ofList(list)    // List<FValue>
FValue.ofMap(map)      // Map<String, FValue>
FValue.NULL            // null
```

### Reading Values

```java
value.asBoolean()    // coerce to boolean
value.asNumber()     // coerce to double
value.asString()     // coerce to string
value.asList()       // List<FValue>
value.asMap()        // Map<String, FValue>
```

### Type Checks

```java
value.isNull()
value.isNumber()
value.isString()
value.isList()
value.isMap()
```
