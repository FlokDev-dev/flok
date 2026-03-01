# Variables

Flok has two kinds of variables with distinct syntax so you always know what you're looking at.

## Local Variables — `%name%`

Local variables exist for the duration of one event or command execution. They're created by your script or injected automatically by the engine.

```fk
%coins% = 100
%message% = "Hello!"
set %score% to 0
```

### Auto-Populated Player Variables

These are automatically available in any player-triggered event:

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

> **Event-specific variables** (like `%first-join%` or `%block-type%`) are documented on the [Events Reference](events) page.

---

## Persistent Variables — `__key__`

Persistent variables are saved to disk and survive server restarts. They are **global** — shared across all scripts.

```fk
__total-joins__ += 1
__coins-%player-name%__ = 500
send "You have __coins-%player-name%__ coins."
```

Keys can embed local variable values using `%var%` inside the `__...__` delimiters. This is how you create per-player storage.

> See [Persistent Storage](storage) for more detail, including best practices and the admin commands for inspecting data.

---

## Assignment

### Direct Assignment

```fk
%score% = 0
__coins-%player-name%__ = 500
```

### Augmented Assignment

Both variable types support compound operators:

```fk
%score% += 10
%score% -= 5
%score% *= 2
%score% /= 2
__coins-%player-name%__ += 100
```

### Natural-Language Syntax

```fk
set %coins% to 500
add 10 to %coins%
add 50 to __coins-%player-name%__
remove 5 from %coins%
```

---

## Lists and Maps

```fk
%my-list% = [1, 2, 3, "four"]
%first% = %my-list%[0]

%data% = {"name": "Steve", "score": 42}
%score% = %data%["score"]
```

List and map operations are covered in [Built-in Functions](builtins).
