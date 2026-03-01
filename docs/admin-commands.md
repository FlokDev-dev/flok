# Admin Commands

All admin commands require the `flok.admin` permission (default: op). `/fk` is an alias for `/flok`.

## Script Management

| Command | Description |
|---|---|
| `/flok reload` | Reload all scripts from disk |
| `/flok reload <file.fk>` | Reload a single script |
| `/flok list` | List loaded scripts with event/command/function counts |
| `/flok info` | Engine stats (scripts, events, commands, JVM memory) |
| `/flok debug` | Toggle verbose debug logging |

---

## Storage Management

| Command | Description |
|---|---|
| `/flok storage` | Show total storage entry count |
| `/flok storage list` | List all persistent keys and values |
| `/flok storage get <key>` | Get a specific value |
| `/flok storage set <key> <value>` | Set a value (stored as string) |
| `/flok storage save` | Force an immediate flush to disk |
| `/flok storage reset` | **Wipe all persistent data** — irreversible! |

> `/flok storage reset` cannot be undone. Back up `flok_data.yml` before using it.

---

## Typical Workflow

**After editing a script:**
```
/flok reload myscript.fk
```

**Checking if a script registered correctly:**
```
/flok list
```

**Debugging a script that isn't working:**
```
/flok debug
# reproduce the issue
/flok debug   ← toggle off when done
```

**Manually fixing a player's coin balance:**
```
/flok storage get coins-PlayerName
/flok storage set coins-PlayerName 500
```
