# Flok — User Manual
### Lightweight Server Scripting for Minecraft 1.21+

---

## Table of Contents

1. [What is Flok?](#what-is-flok)
2. [Installation](#installation)
3. [Your First Script](#your-first-script)
4. [Script Structure](#script-structure)
5. [Variables](#variables)
6. [Operators & Expressions](#operators--expressions)
7. [Control Flow](#control-flow)
8. [Functions](#functions)
9. [Built-in Functions](#built-in-functions)
10. [Effects (Actions)](#effects-actions)
11. [Events Reference](#events-reference)
12. [Commands](#commands)
13. [Persistent Storage](#persistent-storage)
14. [String Templates](#string-templates)
15. [Admin Commands](#admin-commands)
16. [Configuration](#configuration)
17. [Addon API](#addon-api)
18. [Flok vs Skript](#flok-vs-skript)
19. [Troubleshooting](#troubleshooting)

---

## What is Flok?

Flok is a fast, lightweight scripting plugin for Paper/Spigot servers. You write plain `.fk` files — no Java, no compile step, no restarts for most changes. Drop a script in, run `/flok reload`, and it's live.

**Design goals:**
- Zero server lag from scripts (op-limit, throttling, pooled contexts)
- Syntax that feels natural, not arcane
- A clean addon API so other plugins can extend it

---

## Installation

1. Drop `Flok.jar` into your `plugins/` folder.
2. Start or restart the server.
3. Flok creates `plugins/Flok/scripts/` automatically.
4. Put your `.fk` files in that folder.
5. Scripts load on startup. Use `/flok reload` to reload without restarting.

**Requirements:** Paper or Spigot 1.21+, Java 21+

---

## Your First Script

Create `plugins/Flok/scripts/hello.fk`:

```
on player-join:
    send "Welcome to the server, %player-name%!"
    send "&aYou joined for the first time!" if %first-join% is true
```

Run `/flok reload`. Every player who joins now gets a welcome message.

---

## Script Structure

A `.fk` script is a flat file containing **event blocks**, **command blocks**, and **function definitions**. Indentation defines blocks — use 4 spaces (or 2, consistently).

```
# This is a comment

on player-join:
    send "Hello %player-name%!"

on player-quit:
    broadcast "&e%player-name% left the server."

command greet(target):
    send "Hey %target%, %player-name% says hello!"

function double(n):
    return %n% * 2
```

**Rules:**
- Block headers end with an optional `:` — both `on player-join:` and `on player-join` work.
- Indentation must be consistent within a file. Mixing tabs and spaces is not allowed.
- Comments start with `#` and can appear anywhere.

---

## Variables

### Local Variables — `%name%`

Local variables exist for the duration of one event or command execution. They are set automatically for events (e.g. `%player-name%`) or created by your script.

```
%coins% = 100
%message% = "Hello!"
set %score% to 0
```

**Player variables** are automatically available in any player-triggered event:

| Variable | Value |
|---|---|
| `%player-name%` | Player's username |
| `%player-display-name%` | Display name (with prefixes) |
| `%player-uuid%` | Player UUID string |
| `%player-world%` | World name |
| `%player-health%` | Current health (0–20) |
| `%player-max-health%` | Max health |
| `%player-food%` | Food level (0–20) |
| `%player-level%` | XP level |
| `%player-gamemode%` | `survival`, `creative`, `adventure`, `spectator` |
| `%player-x%` | X coordinate |
| `%player-y%` | Y coordinate |
| `%player-z%` | Z coordinate |
| `%player-yaw%` | Yaw (horizontal rotation) |
| `%player-pitch%` | Pitch (vertical rotation) |

**Event-specific variables** are documented in the [Events Reference](#events-reference).

### Persistent Variables — `__key__`

Persistent variables are saved to disk and survive server restarts. They are global — shared across all scripts and players.

```
__total-joins__ += 1
__coins-%player-name%__ = 500
send "You have __coins-%player-name%__ coins."
```

Keys can contain variable interpolation using `%var%` inside the `__...__` delimiters. This is how you make per-player storage.

### Augmented Assignment

Both variable types support compound assignment operators:

```
%score% += 10
%score% -= 5
%score% *= 2
%score% /= 2
__coins-%player-name%__ += 100
```

### Skript-style `set` and `add`

You can also use natural-language syntax:

```
set %coins% to 500
add 10 to %coins%
add 50 to __coins-%player-name%__
remove 5 from %coins%
```

---

## Operators & Expressions

### Arithmetic

| Operator | Meaning |
|---|---|
| `+` | Addition (also string concatenation) |
| `-` | Subtraction |
| `*` | Multiplication |
| `/` | Division |
| `%` | Modulo |
| `^` | Power |

### Comparison

| Operator | Alternatives | Meaning |
|---|---|---|
| `==` | `is` | Equal |
| `!=` | `isnt`, `isn't` | Not equal |
| `<` | | Less than |
| `<=` | | Less than or equal |
| `>` | | Greater than |
| `>=` | | Greater than or equal |
| `contains` | | List or string contains |

### Logical

```
if %health% > 5 and %food% > 3:
    send "You're doing fine."

if %level% == 0 or %coins% < 10:
    send "You need resources!"

if not %player-gamemode% is "creative":
    send "Survival only!"
```

### Ternary (inline if)

```
# Not directly supported — use if/else instead
```

### String Concatenation

The `+` operator concatenates strings:

```
%greeting% = "Hello, " + %player-name% + "!"
```

Or use string templates (see [String Templates](#string-templates)).

### List and Index Access

```
%my-list% = [1, 2, 3, "four"]
%first% = %my-list%[0]

%data% = {"name": "Steve", "score": 42}
%score% = %data%["score"]
```

---

## Control Flow

### If / Else If / Else

```
if %player-health% <= 5:
    send "&cYou're nearly dead!"
else if %player-health% <= 10:
    send "&eWatch your health."
else:
    send "&aYou're healthy."
```

`elseif`, `else if`, and `elif` are all accepted.

### While Loop

```
%count% = 0
while %count% < 5:
    send "Count: %count%"
    %count% += 1
```

### For Each Loop

```
%items% = ["sword", "shield", "potion"]
for item in %items%:
    send "Item: %item%"

# Iterate over string characters
for char in "hello":
    send "%char%"
```

### Repeat

```
repeat 5 times:
    broadcast "This repeats 5 times!"
```

The `times` keyword is optional.

### Break and Continue

```
for item in %list%:
    if %item% == "stop":
        break
    if %item% == "skip":
        continue
    send "%item%"
```

`stop` is an alias for `break`.

### Return

```
function clamp(value, min, max):
    if %value% < %min%:
        return %min%
    if %value% > %max%:
        return %max%
    return %value%
```

---

## Functions

Define reusable functions in any script file. Functions are available within their own script only.

```
function greet(name):
    send "Hello, %name%!"

function add(a, b):
    return %a% + %b%

on player-join:
    greet(%player-name%)
    %result% = add(10, 5)
    send "10 + 5 = %result%"
```

**Rules:**
- Functions can call other functions (up to 64 levels deep — stack overflow protection is built in).
- Recursion works but has a depth limit.
- A function with no `return` statement returns `null`.

---

## Built-in Functions

These are available everywhere in scripts, no import needed.

### Math

| Function | Description |
|---|---|
| `abs(n)` | Absolute value |
| `ceil(n)` | Round up |
| `floor(n)` | Round down |
| `round(n)` | Round to nearest integer |
| `sqrt(n)` | Square root |
| `pow(base, exp)` | Exponentiation |
| `min(a, b)` | Minimum of two values |
| `max(a, b)` | Maximum of two values |
| `clamp(value, min, max)` | Clamp value between min and max |
| `log(n)` | Natural logarithm |
| `log10(n)` | Base-10 logarithm |
| `sin(deg)` | Sine (degrees) |
| `cos(deg)` | Cosine (degrees) |
| `tan(deg)` | Tangent (degrees) |
| `random()` | Random float between 0 and 1 |
| `random-int(min, max)` | Random integer between min and max (inclusive) |

### String

| Function | Description |
|---|---|
| `upper(s)` | Uppercase |
| `lower(s)` | Lowercase |
| `length(s)` | String length (or list size) |
| `trim(s)` | Remove whitespace from both ends |
| `starts-with(s, prefix)` | True if s starts with prefix |
| `ends-with(s, suffix)` | True if s ends with suffix |
| `contains(s, sub)` | True if s contains sub (also works on lists) |
| `replace(s, old, new)` | Replace all occurrences |
| `split(s, delimiter)` | Split string into list |
| `substring(s, start, end)` | Substring (end optional) |
| `index-of(s, sub)` | Index of first occurrence (-1 if not found) |
| `repeat-str(s, n)` | Repeat string n times |
| `str(value)` | Convert any value to string |
| `num(value)` | Convert to number |
| `bool(value)` | Convert to boolean |

### List

| Function | Description |
|---|---|
| `range(start, end)` | List of integers from start (inclusive) to end (exclusive) |
| `push(list, value)` | Append value to list (modifies in place) |
| `pop(list)` | Remove and return last item |
| `remove(list, index)` | Remove item at index |
| `join(list, sep)` | Join list into string (sep defaults to `, `) |
| `sum(list)` | Sum all numeric values |
| `sort(list)` | Return sorted copy |
| `shuffle(list)` | Return shuffled copy |
| `reverse(list)` | Return reversed copy |
| `size(list)` / `count(list)` | Number of items (also works on maps and strings) |

### Type Checking

| Function | Description |
|---|---|
| `is-null(v)` | True if value is null |
| `is-number(v)` | True if value is a number |
| `is-string(v)` | True if value is a string |
| `is-list(v)` | True if value is a list |

### Utility

| Function | Description |
|---|---|
| `format-time(seconds)` | Format seconds as `HH:MM:SS` |

---

## Effects (Actions)

Effects are the actions scripts can take — sending messages, teleporting players, playing sounds, etc. They are called without parentheses, with arguments separated by spaces or commas.

### Messaging

```
# Send a message to the player
send "Hello, %player-name%!"

# Broadcast to all players
broadcast "&aServer announcement!"

# Action bar (above the hotbar)
actionbar "&6Your balance: %__coins-%player-name__%"

# Title and subtitle
title "&lWelcome!" "&7Enjoy your stay"
```

Color codes use `&` followed by a standard Minecraft color/format code.

### Player State

```
# Fully restore health
heal

# Set food level (0–20)
set-food 20

# Enable or disable flight
set-fly true
set-fly false

# Change game mode
set-gamemode creative
set-gamemode survival

# Teleport to coordinates
teleport 100 64 -200
```

### Inventory

```
# Give an item (material name, amount optional)
give diamond 5
give iron-sword 1
give oak-log 64
```

Material names follow Minecraft's internal names — use underscores or hyphens.

### Sound

```
# Play a sound (sound name, volume, pitch — all optional except name)
sound entity-player-levelup
sound block-note-block-pling 1.0 1.5
```

Sound names follow Bukkit's `Sound` enum (case-insensitive, hyphens accepted).

### Potion Effects

```
# Apply a potion effect (type, duration in seconds, amplifier)
potion speed 30 2
potion regeneration 10 1
potion night-vision 60
```

### Scoreboard

```
# Set the sidebar title
scoreboard-title "&6&lMy Server"

# Set a line (score value determines position — higher = higher on board)
scoreboard-line 10 "&fCoins: &e%__coins-%player-name__%"
scoreboard-line 9 "&fKills: &c%__kills-%player-name__%"
scoreboard-line 8 " "
scoreboard-line 7 "&7%player-world%"
```

---

## Events Reference

### Player Lifecycle

#### `on player-join`
Fires when a player joins the server.

| Variable | Value |
|---|---|
| `%first-join%` | `true` if this is the player's first time joining |

```
on player-join:
    if %first-join% is true:
        send "&aWelcome to the server for the first time!"
        __total-joins__ += 1
    else:
        send "&7Welcome back, %player-name%!"
```

#### `on player-quit`
Fires when a player leaves.

```
on player-quit:
    broadcast "&e%player-name% left the server."
```

#### `on player-death`
Fires when a player dies.

| Variable | Value |
|---|---|
| `%death-message%` | The death message (may be empty) |

```
on player-death:
    __deaths-%player-name%__ += 1
    send "You have died %__deaths-%player-name__%% times."
```

#### `on player-respawn`
Fires when a player respawns after death.

```
on player-respawn:
    send "You respawned! Here's a starter kit."
    give bread 10
```

### Player Actions

#### `on player-chat`
Fires when a player sends a chat message.

| Variable | Value |
|---|---|
| `%message%` | The chat message |

```
on player-chat:
    if %message% contains "help":
        send "Type /help for a list of commands."
```

#### `on player-command`
Fires when a player runs any command.

| Variable | Value |
|---|---|
| `%command%` | The full command including the leading `/` |

#### `on player-level-change`
Fires when a player's XP level changes.

| Variable | Value |
|---|---|
| `%old-level%` | Previous level |
| `%new-level%` | New level |

```
on player-level-change:
    if %new-level% == 30:
        send "&6You reached level 30! You can enchant powerful items."
```

#### `on player-gamemode-change`
Fires when a player's game mode changes.

| Variable | Value |
|---|---|
| `%new-gamemode%` | `survival`, `creative`, `adventure`, or `spectator` |

### Movement

#### `on player-move`
Fires when a player moves to a different block. **Throttled to once per second per player** — this is a safety guarantee and cannot be changed per script.

Pure head rotation (looking around without moving) does not fire this event.

| Variable | Value |
|---|---|
| `%from-x%`, `%from-y%`, `%from-z%` | Previous position |
| `%to-x%`, `%to-y%`, `%to-z%` | New position |

```
on player-move:
    if %player-y% < 0:
        send "&cYou're below the void!"
```

### Blocks

#### `on block-break`
Fires when a player breaks a block.

| Variable | Value |
|---|---|
| `%block-type%` | Material name (e.g. `stone`, `oak_log`) |
| `%block-x%`, `%block-y%`, `%block-z%` | Block coordinates |
| `%block-world%` | World name |

```
on block-break:
    if %block-type% == "diamond_ore":
        __diamonds-mined-%player-name%__ += 1
        send "Total diamonds mined: __diamonds-mined-%player-name__%"
```

#### `on block-place`
Fires when a player places a block. Same variables as `block-break`.

---

## Commands

Define custom commands directly in your scripts.

### Basic Command

```
command hello:
    send "Hello, %player-name%!"
```

### Command with Arguments

Named parameters are accessible as local variables. If a player doesn't provide enough arguments, the parameters default to an empty string.

```
command greet(target, message):
    send "%player-name% says to %target%: %message%"
```

Players run it as: `/greet Steve Hello!`

### Command Metadata

You can attach metadata to your command on the same line before the colon:

```
command fly permission: server.fly description: Toggle your flight:
    if %player-gamemode% == "creative":
        send "&cAlready in creative mode."
    else:
        set-fly true
        send "&aFlight enabled!"
```

```
command heal aliases: h, restore permission: staff.heal description: Heal yourself:
    heal
    set-food 20
    send "&aYou have been healed."
```

**Supported metadata:**
- `permission: some.permission.node` — player must have this permission to use the command
- `description: your description` — shows in tab-complete and `/help`
- `aliases: alias1, alias2` — alternative names for the command

### Accessing Raw Arguments

Inside a command, you always have access to:

| Variable | Value |
|---|---|
| `%args%` | List of all arguments |
| `%args-count%` | Number of arguments provided |

```
command info:
    if %args-count% == 0:
        send "Usage: /info <topic>"
    else:
        %topic% = %args%[0]
        send "Info about: %topic%"
```

---

## Persistent Storage

Persistent storage (the `__key__` variables) is a global key-value store saved to `plugins/Flok/flok_data.yml`. Data persists across restarts and reloads.

### Basic Usage

```
# Write
__server-start-count__ += 1

# Read
send "Server has started __server-start-count__ times."

# Delete (set to null)
__temp-value__ = null
```

### Per-Player Data

Use variable interpolation inside the key to create per-player storage:

```
on player-join:
    __visits-%player-name%__ += 1
    send "You've visited %__visits-%player-name__%% times!"
```

### Best Practices

- Use descriptive, namespaced keys: `__myplugin-coins-%player-name%__` rather than just `__coins__`.
- Persistent reads are O(1) in-memory — they're not disk reads on every access.
- Data is auto-saved every 5 minutes and on server shutdown.
- You can inspect and edit storage live with `/flok storage`.

---

## String Templates

Any string can embed variables and persistent values directly:

```
send "Hello %player-name%, you have __coins-%player-name%__ coins!"
```

This is equivalent to concatenating strings and variables manually but much cleaner.

**Rules:**
- `%var-name%` inside a string interpolates a local variable.
- `__key__` inside a string interpolates a persistent value.
- Nested interpolation in persist keys works: `__coins-%player-name%__`.
- The string is evaluated left-to-right at runtime.

---

## Event Cancellation

Scripts can cancel the event that triggered them using the `cancel` effect. This works on any cancellable event — block breaks, chat messages, commands, movement, and block placement.

```
on block-break:
    if %block-type% == "bedrock":
        cancel
        send "&cYou can't break that!"

on player-chat:
    if %message% contains "badword":
        cancel
        send "&cWatch your language."

on player-move:
    if %to-y% < 0:
        cancel

on block-place:
    if %player-gamemode% == "survival" and %block-type% == "tnt":
        cancel
        send "&cTNT is disabled on this server."

on player-command:
    if %command% == "/op":
        cancel
        send "&cNope."
```

You can also use `uncancel` to reverse a cancellation — useful if you want to conditionally re-allow something another script blocked:

```
on block-break:
    if %player-name% == "admin":
        uncancel
```

**Note:** Events that are not cancellable (join, quit, death, respawn, level-change, gamemode-change) ignore `cancel` silently — it won't cause an error.

## Admin Commands

All admin commands require the `flok.admin` permission (default: op).

| Command | Description |
|---|---|
| `/flok reload` | Reload all scripts from disk |
| `/flok reload <file.fk>` | Reload a single script |
| `/flok list` | List loaded scripts with event/command/function counts |
| `/flok info` | Engine stats (scripts, events, commands, JVM memory) |
| `/flok debug` | Toggle verbose debug logging |
| `/flok storage` | Show storage entry count |
| `/flok storage list` | List all persistent keys and values |
| `/flok storage get <key>` | Get a specific value |
| `/flok storage set <key> <value>` | Set a value (string) |
| `/flok storage save` | Force an immediate save to disk |
| `/flok storage reset` | **Wipe all persistent data** (irreversible!) |

`/fk` is an alias for `/flok`.

---

## Configuration

`plugins/Flok/config.yml`:

```yaml
# Enable verbose debug logging (shows script errors with more detail)
debug: false

# Scripts folder (relative to plugin data folder)
scripts:
  folder: scripts

# Persistent storage
storage:
  data-file: flok_data.yml

# Safety limits
safety:
  # Max AST operations per script execution before halting
  # Prevents infinite loops from freezing the server
  # Default 50000 is generous for any normal script
  max-ops: 50000
```

**`max-ops`:** Every expression evaluation, loop iteration, and statement counts as one operation. A script that hits the limit is halted and a warning is logged — the server is never blocked. If you have a legitimate script that hits this, increase the value; 50,000 is intentionally conservative.

---

## Addon API

Other plugins can extend Flok by depending on `flok-api.jar`.

### Getting the API

```java
import wbog.flok.api.FlokAPI;
import org.bukkit.plugin.RegisteredServiceProvider;

RegisteredServiceProvider<FlokAPI> rsp =
    Bukkit.getServicesManager().getRegistration(FlokAPI.class);
if (rsp != null) {
    FlokAPI flok = rsp.getProvider();
    // use the API
}
```

Or use the convenience method:
```java
FlokAPI flok = FlokAPI.get(); // throws if Flok isn't loaded
```

In your `plugin.yml`:
```yaml
softdepend: [Flok]
```

### Registering Custom Effects

```java
flok.registerEffect("send-discord", (player, args, ctx) -> {
    if (args.isEmpty()) return;
    String message = args.get(0).asString();
    // send to Discord webhook...
});
```

In scripts:
```
on player-join:
    send-discord "**%player-name%** joined the server!"
```

### Firing Custom Events

```java
// Fire an event from your plugin (e.g. after a purchase in your shop)
flok.fireEvent("shop-purchase", player, Map.of(
    "item",  FValue.of(itemName),
    "price", FValue.of(price)
));
```

In scripts:
```
on shop-purchase:
    send "You bought %item% for %price% coins."
    __coins-%player-name%__ -= %price%
```

### Reading/Writing Storage

```java
FValue coins = flok.getStorage("coins-" + playerName);
flok.setStorage("coins-" + playerName, FValue.of(newAmount));
```

### Unregister on Disable

Always unregister your effects when your addon disables:

```java
@Override
public void onDisable() {
    FlokAPI flok = FlokAPI.get();
    flok.unregisterEffect("send-discord");
}
```

### FValue Reference

`FValue` is Flok's universal value type:

```java
FValue.of(true)          // boolean
FValue.of(42.0)          // number (always double internally)
FValue.of("hello")       // string
FValue.ofList(list)      // list
FValue.ofMap(map)        // map
FValue.NULL              // null

value.asBoolean()        // coerce to boolean
value.asNumber()         // coerce to double
value.asString()         // coerce to string
value.asList()           // List<FValue>
value.asMap()            // Map<String, FValue>
value.isNull()
value.isNumber()
value.isString()
value.isList()
value.isMap()
```

---

## Flok vs Skript

Flok is designed to be a performance-first alternative. Here's how they compare:

### Performance

| | Flok | Skript |
|---|---|---|
| Event dispatch | O(1) index lookup | Linear scan of all scripts |
| Context allocation | Pooled — zero allocation per event | New object(s) per event |
| Player variables | Lazy — injected only if accessed | Eager — always resolved |
| `player-move` safety | Hard 1s throttle + block-change filter built in | No built-in throttle; easy to lag server |
| Infinite loop protection | Op-limit halts runaway scripts server-safe | Can freeze the server |
| Async safety | Chat event marshalled to main thread | Mixed |

Flok will not lag your server from a badly-written script. Skript can.

### Syntax

Both use an indentation-based, English-like syntax. Flok is more explicit:

**Skript:**
```
on join:
    send "Welcome %player%!" to player
    add 100 to {coins::%player%}
```

**Flok:**
```
on player-join:
    send "Welcome %player-name%!"
    __coins-%player-name%__ += 100
```

Flok uses `%var%` for local variables and `__key__` for persistent storage — a clear visual distinction. In Skript, `{var}` and `{var::player}` look similar and are easy to confuse.

### Features Flok Has That Skript Doesn't (Out of the Box)

- **Op-limit safety** — scripts can never freeze the server
- **Pooled execution contexts** — no GC pressure at high event rates
- **Clean addon API** — other plugins integrate with a stable interface, not reflection hacks
- **Per-player move throttling** — built in, unconditional, can't be bypassed by a script author
- **Human-readable persistent data** — `flok_data.yml` is editable by admins directly

### Features Skript Has That Flok Doesn't (Yet)
- Aliases/triggers for Bukkit entity/world events beyond the built-in set (Flok's addon API fills this gap for other plugins; direct expansion requires adding to EventAdapter)
- A larger community and addon ecosystem

### Verdict

If you need the widest possible event coverage and don't mind the performance risk, Skript's ecosystem is larger. If you want a server that stays fast no matter what's in your scripts, and you're willing to write a small addon for unusual events, Flok is the better foundation.

---

## Troubleshooting

### Script won't load

Run `/flok reload` and check the output. Errors show the filename and line number:
```
✗ myscript.fk: [line 5] Expected INDENT but got NEWLINE
```

Common causes:
- **Inconsistent indentation** — don't mix tabs and spaces, and don't randomly change indent depth.
- **Missing colon** — block headers like `on player-join`, `if`, `while` need a `:` (or at least a newline before the indented body).
- **Unterminated variable** — `%var` without a closing `%`, or `__key` without `__`.

### Script loads but does nothing

- Check the event name. `on join:` won't work — it must be `on player-join:`.
- Check `/flok list` to see if the script actually registered the event.
- Enable debug mode with `/flok debug` to see more output.

### "Script exceeded op limit"

Your script has an infinite loop or is doing too much work in one execution. Check for:
- A `while` loop with a condition that never becomes false.
- A recursive function with no base case.

If your script genuinely needs more operations (e.g. processing a very large list), increase `safety.max-ops` in `config.yml`.

### Persistent data disappeared

Check `/flok storage list`. If it's empty after a crash, the last auto-save may not have run. The auto-save interval is 5 minutes — for critical data, call `/flok storage save` more frequently, or reduce the scheduler interval in a future config option.

### Command not appearing in tab-complete

After `/flok reload`, Flok calls `syncCommands()` automatically. If the command still doesn't appear, restart the server — some clients cache the command list.

### Effects do nothing / "Unknown effect"

Effect names are case-insensitive but must match exactly. Check the [Effects Reference](#effects-actions). If you're using a custom effect from an addon, make sure the addon is loaded and has registered the effect before your script fires.

---

*Made by wbog. _wbog on discord.*