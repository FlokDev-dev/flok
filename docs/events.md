# Events Reference

## Player Lifecycle

### `on player-join`

Fires when a player connects to the server.

| Variable | Value |
|---|---|
| `%first-join%` | `true` if this is the player's first time joining |

```fk
on player-join:
    if %first-join% is true:
        send "&aWelcome to the server for the first time!"
        __total-joins__ += 1
    else:
        send "&7Welcome back, %player-name%!"
```

---

### `on player-quit`

Fires when a player disconnects.

```fk
on player-quit:
    broadcast "&e%player-name% left the server."
```

---

### `on player-death`

Fires when a player dies.

| Variable | Value |
|---|---|
| `%death-message%` | The death message (may be empty) |

```fk
on player-death:
    __deaths-%player-name%__ += 1
    send "You have died __deaths-%player-name__%% times."
```

---

### `on player-respawn`

Fires when a player respawns after death.

```fk
on player-respawn:
    send "You respawned! Here's a starter kit."
    give bread 10
```

---

## Player Actions

### `on player-chat`

Fires when a player sends a chat message.

| Variable | Value |
|---|---|
| `%message%` | The chat message |

```fk
on player-chat:
    if %message% contains "help":
        send "Type /help for a list of commands."
```

---

### `on player-command`

Fires when a player runs any command.

| Variable | Value |
|---|---|
| `%command%` | The full command string including the leading `/` |

```fk
on player-command:
    if %command% == "/op":
        cancel
        send "&cNope."
```

---

### `on player-level-change`

Fires when a player's XP level changes.

| Variable | Value |
|---|---|
| `%old-level%` | Previous level |
| `%new-level%` | New level |

```fk
on player-level-change:
    if %new-level% == 30:
        send "&6You reached level 30! Time to enchant."
```

---

### `on player-gamemode-change`

Fires when a player's game mode changes.

| Variable | Value |
|---|---|
| `%new-gamemode%` | `survival`, `creative`, `adventure`, or `spectator` |

---

## Movement

### `on player-move`

Fires when a player moves to a different block. **Throttled to once per second per player** — this is unconditional and cannot be changed per script.

Pure head rotation (looking around without moving) does not fire this event.

| Variable | Value |
|---|---|
| `%from-x%`, `%from-y%`, `%from-z%` | Previous block position |
| `%to-x%`, `%to-y%`, `%to-z%` | New block position |

```fk
on player-move:
    if %player-y% < 0:
        send "&cYou're below the void!"
```

---

## Blocks

### `on block-break`

Fires when a player breaks a block.

| Variable | Value |
|---|---|
| `%block-type%` | Material name (e.g. `stone`, `oak_log`) |
| `%block-x%`, `%block-y%`, `%block-z%` | Block coordinates |
| `%block-world%` | World name |

```fk
on block-break:
    if %block-type% == "diamond_ore":
        __diamonds-mined-%player-name%__ += 1
        send "Total diamonds mined: __diamonds-mined-%player-name__%"
```

---

### `on block-place`

Fires when a player places a block. Same variables as `block-break`.

```fk
on block-place:
    if %player-gamemode% == "survival" and %block-type% == "tnt":
        cancel
        send "&cTNT is disabled on this server."
```

---

## Cancellable Events

The following events support `cancel` and `uncancel`:

`player-chat`, `player-command`, `player-move`, `block-break`, `block-place`

The lifecycle events (`player-join`, `player-quit`, `player-death`, `player-respawn`, `player-level-change`, `player-gamemode-change`) are **not cancellable** — `cancel` is silently ignored on them.

See [Event Cancellation](cancellation) for full details.
