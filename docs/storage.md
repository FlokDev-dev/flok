# Persistent Storage

Persistent variables (`__key__`) are saved to `plugins/Flok/flok_data.yml`. They survive server restarts and `/flok reload`. They're global — shared across all scripts.

## Basic Usage

```fk
# Write
__server-start-count__ += 1

# Read (inline in a string)
send "Server has started __server-start-count__ times."

# Delete (set to null)
__temp-value__ = null
```

---

## Per-Player Data

Use variable interpolation inside the key to create per-player storage. The `%var%` inside `__...__` is resolved at runtime:

```fk
on player-join:
    __visits-%player-name%__ += 1
    send "You've visited __visits-%player-name__%% times!"
```

This creates keys like `visits-Alice`, `visits-Bob`, etc.

---

## Best Practices

**Use descriptive, namespaced keys.** Prefer `__myplugin-coins-%player-name%__` over just `__coins__`. Multiple scripts share the same storage — name collisions will cause bugs.

**Persistent reads are fast.** Storage is held in memory. There's no disk I/O on every read. Writes are batched and flushed every 5 minutes and on shutdown.

**For critical data, flush manually.** Use `/flok storage save` after important writes if you can't afford to lose 5 minutes of data on a crash.

**Data is human-editable.** `flok_data.yml` is plain YAML. Admins can inspect and edit it directly while the server is stopped.

---

## Inspecting Storage In-Game

Use the admin commands to view and edit storage live:

```
/flok storage list
/flok storage get coins-Alice
/flok storage set coins-Alice 9999
/flok storage save
```

See [Admin Commands](admin-commands) for the full reference.

---

## Storage from the Addon API

Java plugins can read and write storage via `FlokAPI`:

```java
FValue coins = flok.getStorage("coins-" + playerName);
flok.setStorage("coins-" + playerName, FValue.of(newAmount));
```

See [Addon API](addon-api) for details.
